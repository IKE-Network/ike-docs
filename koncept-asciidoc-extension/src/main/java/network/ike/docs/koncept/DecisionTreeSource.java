/*
 * Copyright © 2026 IKE Network (support@ike.network)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.koncept;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code [decision-tree]} block read store-free, for the documentation build: the same
 * grammar the live renderer in {@code komet-claude-plugin} parses ({@code DecisionTreeText},
 * {@code dev-decision-tree-blocks}), kept byte for byte in step so one block renders in both
 * media, but read here as plain source. Identifiers stay as written, labels are the authoring
 * labels, and nothing resolves against a store.
 *
 * <pre>
 * decision: k:ScanNow[Scan now?] @id=…
 *   alternative: k:Scan[Scan] @id=…
 *     money = [700,700] k:Usd[US dollar] @id=… basis=estimated
 *     health = [0.002,0.002] k:Qaly[QALY] @id=…
 *     chance: k:MraResult[MRA result] @id=…
 *       event: k:Positive[Scan positive] @id=…
 *         probability = [0.214,0.214]
 *         …
 *       residual: k:Negative[Scan negative] @id=…
 * </pre>
 *
 * A vertex is a line whose word ends in a colon, a measure a line whose word is followed by an
 * equals sign; indentation is the edge. Everything the processor does not understand is a
 * problem with its line number, and the tree is built around it.
 *
 * <p>A {@code [graphlet-model]} block is the same grammar with parameters that read the document's
 * ANF statements by name ({@code dev-decision-point-knowledge}): {@code parameter = [0.15,0.15]
 * anf:est-material-problem} reads the statement's range and scale through {@link StatementMeasures}
 * and holds the value the tree runs at; {@code parameter = anf:est-material-problem} runs at the
 * midpoint of the statement's range, computed and never written; {@code p = from anf:…} and
 * {@code money = from anf:…} read such a parameter as {@code from} a quantity does, a cost on the
 * statement's own scale; {@code of anf:…} reads it as an operand. A statement the document has not
 * stated before the block leaves the parameter with no value, named as a problem, never guessed.
 */
final class DecisionTreeSource {

    private DecisionTreeSource() {
    }

    /** The kinds of vertex. */
    enum Kind { DECISION, ALTERNATIVE, CHANCE, EVENT, OUTCOME }

    /** The kinds of measure a vertex carries. */
    enum MeasureKind { PROBABILITY, MONEY, HEALTH, COUNT, PARAMETER }

    /**
     * The derivation kinds with native implementations, as the IKE starter set names them, known
     * here by the identity the starter set gives each ({@code IkeConcept}) and by its bare name, so a
     * block written with or without identities reads alike.
     */
    enum Derivation {
        COMPLEMENT("Complement", 1),
        PRODUCT("Product", -1),
        CUMULATIVE_HAZARD("Cumulative hazard", 2),
        BAYES_POSTERIOR("Bayes posterior", 3),
        BAYES_MARGINAL("Bayes marginal", 3);

        final String label;
        /** The operands the kind reads, or -1 for two or more. */
        final int arity;

        Derivation(String label, int arity) {
            this.label = label;
            this.arity = arity;
        }

        /** The kind a concept token names: by its label or key, case and spaces aside. */
        static Optional<Derivation> of(Concept concept) {
            for (String name : new String[] {concept.label(), concept.key()}) {
                if (name == null) {
                    continue;
                }
                String bare = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
                for (Derivation kind : values()) {
                    String own = kind.label.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
                    if (bare.equals(own) || bare.equals(own + "derivation")) {
                        return Optional.of(kind);
                    }
                }
            }
            return Optional.empty();
        }
    }

    /** A concept as written: key and label are aids, the identifier is the identity. */
    record Concept(String key, String label, String identifier) {
        /** The label to show: the written label, else the key, else the identifier. */
        String shown() {
            if (label != null && !label.isBlank()) {
                return label;
            }
            return key != null && !key.isBlank() ? key : identifier == null ? "" : identifier;
        }
    }

    /** The bounds of a measure, with the interval arithmetic a rollback does on them. */
    record Interval(double lower, double upper, boolean lowerOpen, boolean upperOpen) {
        static final double EPSILON = 1e-9;

        static Interval point(double value) {
            return new Interval(value, value, false, false);
        }

        boolean isPoint() {
            return lower == upper;
        }

        Interval plus(Interval other) {
            return new Interval(lower + other.lower, upper + other.upper,
                    lowerOpen || other.lowerOpen, upperOpen || other.upperOpen);
        }

        Interval times(Interval other) {
            double[] products = {lower * other.lower, lower * other.upper, upper * other.lower, upper * other.upper};
            boolean[] open = {lowerOpen || other.lowerOpen, lowerOpen || other.upperOpen,
                    upperOpen || other.lowerOpen, upperOpen || other.upperOpen};
            int least = 0;
            int greatest = 0;
            for (int i = 1; i < 4; i++) {
                if (products[i] < products[least] || (products[i] == products[least] && !open[i])) {
                    least = i;
                }
                if (products[i] > products[greatest] || (products[i] == products[greatest] && !open[i])) {
                    greatest = i;
                }
            }
            return new Interval(products[least], products[greatest], open[least], open[greatest]);
        }

        Interval complement() {
            return new Interval(1 - upper, 1 - lower, upperOpen, lowerOpen);
        }

        /** The interval quotient, for a divisor that does not cross zero. */
        Interval dividedBy(Interval divisor) {
            if (divisor.lower <= 0 && divisor.upper >= 0) {
                throw new ArithmeticException("a divisor crosses zero");
            }
            double[] q = {lower / divisor.lower, lower / divisor.upper, upper / divisor.lower, upper / divisor.upper};
            boolean[] open = {lowerOpen || divisor.lowerOpen, lowerOpen || divisor.upperOpen,
                    upperOpen || divisor.lowerOpen, upperOpen || divisor.upperOpen};
            int least = 0;
            int greatest = 0;
            for (int i = 1; i < 4; i++) {
                if (q[i] < q[least] || (q[i] == q[least] && !open[i])) {
                    least = i;
                }
                if (q[i] > q[greatest] || (q[i] == q[greatest] && !open[i])) {
                    greatest = i;
                }
            }
            return new Interval(q[least], q[greatest], open[least], open[greatest]);
        }

        Interval clip(double floor, double ceiling) {
            double lo = Math.max(floor, Math.min(ceiling, lower));
            double hi = Math.max(floor, Math.min(ceiling, upper));
            return new Interval(lo, hi, lowerOpen && lo == lower, upperOpen && hi == upper);
        }

        Interval least(Interval other) {
            boolean lo = lower < other.lower ? lowerOpen : other.lower < lower ? other.lowerOpen : lowerOpen && other.lowerOpen;
            boolean hi = upper < other.upper ? upperOpen : other.upper < upper ? other.upperOpen : upperOpen && other.upperOpen;
            return new Interval(Math.min(lower, other.lower), Math.min(upper, other.upper), lo, hi);
        }

        boolean noGreaterThan(Interval other) {
            return upper <= other.lower + EPSILON;
        }
    }

    /**
     * One measure a vertex carries: stated with bounds, or derived from a kind over the quantities
     * it reads, or read as it is from one parameter ({@code from} the quantity alone, no kind and
     * no operands). A parameter also names the quantity it measures; a parameter that reads one of
     * the document's statements names it, {@code anf:<name>}, and carries the range the statement
     * determined, which is what a sweep swings it over.
     */
    record Measure(MeasureKind kind, Interval bounds, Concept semantic, String basis, String source, Concept quantity,
                   Derivation derivation, Concept derivationConcept, List<Concept> operands, int line,
                   String statement, Interval range) {
        /** A measure that reads no statement. */
        Measure(MeasureKind kind, Interval bounds, Concept semantic, String basis, String source, Concept quantity,
                Derivation derivation, Concept derivationConcept, List<Concept> operands, int line) {
            this(kind, bounds, semantic, basis, source, quantity, derivation, derivationConcept, operands, line, null, null);
        }

        boolean stated() {
            return bounds != null;
        }

        /** Whether this is a parameter that reads one of the document's statements by name. */
        boolean readsAStatement() {
            return statement != null;
        }

        /** The range a statement determined for this parameter, when it reads one the document states and the range is not a point. */
        boolean hasStatedRange() {
            return range != null && !range.isPoint();
        }

        boolean derived() {
            return derivationConcept != null;
        }

        /** Whether the measure takes one parameter's value as it is: from a quantity, no kind. */
        boolean readsAParameter() {
            return derivationConcept != null && derivation == null && operands.isEmpty();
        }

        /** What this measure is called in a worked table: its quantity, else its kind. */
        String name() {
            return quantity != null ? quantity.shown() : kind.name().toLowerCase(Locale.ROOT);
        }
    }

    /** One vertex of the tree, with what it carries and what is beneath it. */
    static final class Node {
        final Kind kind;
        final boolean residual;
        final Concept concept;
        final String vertexId;
        final List<String> statements = new ArrayList<>();
        final List<Measure> measures = new ArrayList<>();
        final List<Node> children = new ArrayList<>();
        final int line;
        String inputKind;
        String treeReference;

        Node(Kind kind, boolean residual, Concept concept, String vertexId, int line) {
            this.kind = kind;
            this.residual = residual;
            this.concept = concept;
            this.vertexId = vertexId;
            this.line = line;
        }

        /** The label the vertex shows: its concept's, else its kind word. */
        String label() {
            String shown = concept == null ? "" : concept.shown();
            return shown.isEmpty() ? kind.name().toLowerCase(Locale.ROOT) : shown;
        }

        /** The one node a branch leads to, when it leads anywhere. */
        Optional<Node> next() {
            return children.isEmpty() ? Optional.empty() : Optional.of(children.get(0));
        }

        Optional<Measure> probability() {
            return measures.stream().filter(m -> m.kind() == MeasureKind.PROBABILITY).findFirst();
        }

        List<Measure> costs() {
            return measures.stream().filter(m -> m.kind() == MeasureKind.MONEY || m.kind() == MeasureKind.HEALTH
                    || m.kind() == MeasureKind.COUNT).toList();
        }

        List<Measure> parameters() {
            return measures.stream().filter(m -> m.kind() == MeasureKind.PARAMETER).toList();
        }
    }

    /** The tree read, when there was a root to read, and every problem with its line. */
    record Parsed(Node root, List<String> problems) {
    }

    // ── Grammar, in step with the plugin's BlockLines and Tokens ─────────────

    private static final Pattern LINE = Pattern.compile("^([A-Za-z][A-Za-z0-9_-]*)\\s*(:|=)\\s*(.*)$");
    private static final Pattern HEADER = Pattern.compile("^\\[[A-Za-z].*]$");
    private static final Pattern DELIMITER = Pattern.compile("^-{2,}$");
    private static final Pattern CONCEPT = Pattern.compile("^k:([^\\[\\s]*)\\[(.*?)]\\s*(?:@id=(\\S+))?\\s*$");
    private static final Pattern INTERVAL = Pattern.compile("^([\\[(])\\s*(\\S+?)\\s*,\\s*(\\S+?)\\s*([\\])])\\s*(.*)$");

    /** The prefix of a reference to one of the document's {@code [anf]} blocks by name, {@code anf:est-material-problem}. */
    static final String ANF_REFERENCE = "anf:";

    /** One raw line with its nesting resolved. */
    private record Line(int number, int indent, boolean vertex, String word, String rest, List<Line> children) {
    }

    /** Reads the block lines into a decision tree, with no statements to read parameters from. */
    static Parsed parse(List<String> raw) {
        return parse(raw, StatementMeasures.none());
    }

    /**
     * Reads the block lines into a decision tree whose parameters may read the document's ANF
     * statements by name, {@code anf:<name>}, through the measures given.
     */
    static Parsed parse(List<String> raw, StatementMeasures statements) {
        List<String> problems = new ArrayList<>();
        List<Line> roots = new ArrayList<>();
        Deque<Line> open = new ArrayDeque<>();
        int number = 0;
        for (String text : raw) {
            number++;
            String stripped = text.strip();
            if (stripped.isEmpty() || HEADER.matcher(stripped).matches() || DELIMITER.matcher(stripped).matches()) {
                continue;
            }
            int indent = text.length() - text.stripLeading().length();
            Matcher m = LINE.matcher(stripped);
            if (!m.matches()) {
                problems.add("line " + number + ": neither a vertex (word: …) nor a value (word = …): " + stripped);
                continue;
            }
            Line line = new Line(number, indent, ":".equals(m.group(2)), m.group(1), m.group(3).strip(), new ArrayList<>());
            while (!open.isEmpty() && open.peek().indent() >= indent) {
                open.pop();
            }
            if (open.isEmpty()) {
                roots.add(line);
            } else {
                open.peek().children().add(line);
            }
            open.push(line);
        }
        if (roots.size() != 1) {
            problems.add("a decision tree has one root line, not " + roots.size());
            return new Parsed(null, problems);
        }
        Line root = roots.get(0);
        if (!root.vertex() || !("decision".equals(root.word()) || "chance".equals(root.word()))) {
            problems.add("line " + root.number() + ": the root is not a decision or a chance");
            return new Parsed(null, problems);
        }
        // a root chance is a tree of consequences with no choice in it, which other trees fold in
        Node tree = vertex(root, "chance".equals(root.word()) ? Kind.CHANCE : Kind.DECISION, false, problems,
                statements == null ? StatementMeasures.none() : statements);
        return new Parsed(tree, problems);
    }

    private static Node vertex(Line line, Kind kind, boolean residual, List<String> problems, StatementMeasures statements) {
        Map<String, String> attributes = attributes(line.rest());
        String head = head(line.rest());
        Concept concept = null;
        if (head.isEmpty()) {
            problems.add("line " + line.number() + ": " + article(kind) + " names no concept");
        } else {
            concept = concept(head).orElse(null);
            if (concept == null) {
                problems.add("line " + line.number() + ": " + article(kind) + " is not a k: token: " + head);
            } else if (concept.identifier() == null) {
                problems.add("line " + line.number() + ": " + article(kind) + " carries no @id: " + head);
            }
        }
        Node node = new Node(kind, residual, concept, attributes.get("@vertex"), line.number());
        attributes.forEach((name, value) -> {
            switch (name) {
                case "statement" -> {
                    for (String id : value.split(";")) {
                        if (!id.isBlank()) {
                            node.statements.add(id.strip());
                        }
                    }
                }
                case "input" -> node.inputKind = value;
                case "tree" -> {
                    if (kind == Kind.OUTCOME) {
                        node.treeReference = value;
                    } else {
                        problems.add("line " + line.number() + ": only an outcome folds in another tree");
                    }
                }
                case "@vertex" -> { }
                default -> problems.add("line " + line.number() + ": unknown attribute of a vertex: " + name);
            }
        });
        for (Line child : line.children()) {
            if (!child.vertex()) {
                measure(child, node, problems, statements);
                continue;
            }
            Node childNode = switch (child.word()) {
                case "alternative" -> vertex(child, Kind.ALTERNATIVE, false, problems, statements);
                case "chance" -> vertex(child, Kind.CHANCE, false, problems, statements);
                case "event" -> vertex(child, Kind.EVENT, false, problems, statements);
                case "residual" -> vertex(child, Kind.EVENT, true, problems, statements);
                case "decision" -> vertex(child, Kind.DECISION, false, problems, statements);
                case "outcome" -> vertex(child, Kind.OUTCOME, false, problems, statements);
                default -> null;
            };
            if (childNode == null) {
                problems.add("line " + child.number() + ": unknown kind of vertex under " + line.word() + ": " + child.word());
                continue;
            }
            node.children.add(childNode);
        }
        return node;
    }

    private static void measure(Line line, Node node, List<String> problems, StatementMeasures statements) {
        MeasureKind kind = switch (line.word().toLowerCase(Locale.ROOT)) {
            case "probability", "p" -> MeasureKind.PROBABILITY;
            case "money" -> MeasureKind.MONEY;
            case "health" -> MeasureKind.HEALTH;
            case "count" -> MeasureKind.COUNT;
            case "parameter" -> MeasureKind.PARAMETER;
            default -> null;
        };
        if (kind == null) {
            problems.add("line " + line.number() + ": unknown value of a vertex: " + line.word());
            return;
        }
        if (!line.children().isEmpty()) {
            problems.add("line " + line.number() + ": a measure has nothing beneath it");
        }
        boolean parameter = kind == MeasureKind.PARAMETER;
        boolean share = parameter || kind == MeasureKind.PROBABILITY;
        Interval bounds = null;
        Concept quantity = null;
        Concept derivationConcept = null;
        List<Concept> operands = new ArrayList<>();
        Concept unit = null;
        String mode = "";
        // the statement a parameter reads, and the statement a value reads as it is
        String statementName = null;
        String readStatement = null;
        List<String> words = words(head(line.rest()));
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word.startsWith(ANF_REFERENCE) && word.length() > ANF_REFERENCE.length()) {
                String name = word.substring(ANF_REFERENCE.length());
                switch (mode) {
                    case "from" -> {
                        if (derivationConcept != null || readStatement != null) {
                            problems.add("line " + line.number() + ": a measure reads one statement after from: anf:" + name);
                        }
                        readStatement = name;
                    }
                    case "of" -> operands.add(statementConcept(name, statements.measure(name)));
                    case "in" -> problems.add("line " + line.number() + ": a measure semantic is a concept, not a statement: anf:" + name);
                    default -> {
                        if (parameter && quantity == null && statementName == null) {
                            statementName = name;
                        } else {
                            problems.add("line " + line.number() + ": an unexpected statement: anf:" + name);
                        }
                    }
                }
            } else if (word.startsWith("k:")) {
                String text = word;
                if (i + 1 < words.size() && words.get(i + 1).startsWith("@id=")) {
                    text = word + " " + words.get(++i);
                }
                Optional<Concept> token = concept(text);
                if (token.isEmpty()) {
                    problems.add("line " + line.number() + ": not a k: token: " + text);
                    continue;
                }
                switch (mode) {
                    case "from" -> derivationConcept = token.get();
                    case "of" -> operands.add(token.get());
                    case "in" -> unit = token.get();
                    default -> {
                        if (parameter && quantity == null) {
                            quantity = token.get();
                        } else if (!parameter && unit == null && bounds != null) {
                            unit = token.get();
                        } else {
                            problems.add("line " + line.number() + ": an unexpected token: " + text);
                        }
                    }
                }
            } else if (word.startsWith("[") || word.startsWith("(")) {
                Matcher m = INTERVAL.matcher(word);
                if (!m.matches()) {
                    problems.add("line " + line.number() + ": not an interval: " + word);
                    continue;
                }
                bounds = new Interval(bound(m.group(2), Double.NEGATIVE_INFINITY), bound(m.group(3), Double.POSITIVE_INFINITY),
                        !"[".equals(m.group(1)), !"]".equals(m.group(4)));
            } else if (List.of("from", "of", "in").contains(word.toLowerCase(Locale.ROOT))) {
                mode = word.toLowerCase(Locale.ROOT);
            } else if ("from".equals(mode) && derivationConcept == null
                    && Derivation.of(new Concept(word, "", null)).isPresent()) {
                // a derivation kind written bare after from, as it reads naturally and as the live
                // surface's assistant writes it: from CumulativeHazard of ...
                derivationConcept = new Concept(word, "", null);
            } else {
                problems.add("line " + line.number() + ": an unexpected word: " + word);
            }
        }
        Map<String, String> attributes = attributes(line.rest());
        String basis = attributes.get("basis");
        if (basis != null && !List.of("estimated", "cited", "derived").contains(basis.toLowerCase(Locale.ROOT))) {
            problems.add("line " + line.number() + ": a basis is estimated, cited or derived, not " + basis);
            basis = null;
        }
        attributes.keySet().forEach(name -> {
            if (!List.of("basis", "source", "@vertex").contains(name)) {
                problems.add("line " + line.number() + ": unknown attribute of a measure: " + name);
            }
        });
        if (statementName != null) {
            // a parameter that reads a statement: the statement's range and scale, the model's value
            if (derivationConcept != null || readStatement != null || !operands.isEmpty()) {
                problems.add("line " + line.number() + ": a parameter that reads a statement is not also derived");
            }
            if (unit != null) {
                problems.add("line " + line.number() + ": a parameter that reads a statement is on the statement's own scale, so it names no measure semantic");
            }
            Optional<StatementMeasures.Stated> stated = statements.measure(statementName);
            if (stated.isEmpty()) {
                problems.add("line " + line.number() + ": a parameter reads anf:" + statementName
                        + ", which no anf block before it states" + (bounds == null ? ", and holds no value" : ""));
            }
            Interval value;
            if (bounds != null) {
                value = bounds;
                if (!bounds.isPoint()) {
                    problems.add("line " + line.number() + ": the value a model runs at is one number, with equal bounds; the range is the statement's");
                }
            } else if (stated.isPresent() && !Double.isInfinite(stated.get().range().lower())
                    && !Double.isInfinite(stated.get().range().upper())) {
                // the midpoint of the statement's range, computed and never written
                value = Interval.point((stated.get().range().lower() + stated.get().range().upper()) / 2);
            } else {
                value = new Interval(Double.NaN, Double.NaN, false, false);
                if (stated.isPresent()) {
                    problems.add("line " + line.number() + ": a parameter reads anf:" + statementName
                            + ", whose range has no midpoint to run at, and holds no value");
                }
            }
            node.measures.add(new Measure(kind, value, stated.map(StatementMeasures.Stated::scale).orElse(null), basis,
                    attributes.get("source"), statementConcept(statementName, stated), null, null, List.of(), line.number(),
                    statementName, stated.map(StatementMeasures.Stated::range).orElse(null)));
            return;
        }
        boolean scaleUnknown = false;
        if (readStatement != null) {
            // from a statement alone reads its parameter as it is, a cost on the statement's own scale
            Optional<StatementMeasures.Stated> stated = statements.measure(readStatement);
            derivationConcept = statementConcept(readStatement, stated);
            if (unit == null && !share) {
                if (stated.isPresent()) {
                    unit = stated.get().scale();
                } else {
                    scaleUnknown = true;
                    problems.add("line " + line.number() + ": a cost reads anf:" + readStatement
                            + ", which no anf block before it states, so its scale is unknown");
                }
            }
        }
        if (bounds == null && derivationConcept == null) {
            problems.add("line " + line.number() + ": a measure begins with an interval, [lo,hi], or is derived from a kind: " + head(line.rest()));
            return;
        }
        if (bounds != null && derivationConcept != null) {
            problems.add("line " + line.number() + ": a measure is stated or derived, not both");
        }
        Derivation derivation = null;
        if (derivationConcept != null) {
            derivation = Derivation.of(derivationConcept).orElse(null);
            if (derivation == null && !operands.isEmpty()) {
                problems.add("line " + line.number() + ": a derivation names no kind the build can run: " + derivationConcept.shown());
            } else if (derivation == null) {
                // from a quantity alone, with nothing after of: the measure reads that parameter as it is
            } else if (derivation.arity >= 0 ? operands.size() != derivation.arity : operands.size() < 2) {
                problems.add("line " + line.number() + ": a " + derivation.label.toLowerCase(Locale.ROOT) + " reads "
                        + (derivation.arity >= 0 ? derivation.arity + "" : "two or more") + " operands, not " + operands.size());
            }
        }
        if (parameter && quantity == null) {
            problems.add("line " + line.number() + ": a parameter names its quantity");
        }
        if (unit == null && !share && !scaleUnknown) {
            problems.add("line " + line.number() + (kind == MeasureKind.COUNT ? ": a count names what it counts" : ": a cost names its measure semantic")
                    + (derivationConcept != null ? " after in" : " after its interval"));
        }
        if (basis == null && derivationConcept != null) {
            basis = "derived";
        }
        node.measures.add(new Measure(kind, bounds, unit, basis, attributes.get("source"), quantity,
                derivation, derivationConcept, List.copyOf(operands), line.number()));
    }

    /**
     * The quantity a statement's parameter is named by: an identity under the statement's name,
     * {@code anf:<name>}, the same wherever the name recurs, labelled as the statement names it
     * when the document has the statement, else by the name alone.
     */
    private static Concept statementConcept(String name, Optional<StatementMeasures.Stated> stated) {
        String label = stated.map(StatementMeasures.Stated::label).filter(l -> !l.isBlank()).orElse(name);
        return new Concept(name, label, ANF_REFERENCE + name);
    }

    private static double bound(String s, double unbounded) {
        String t = s.trim();
        if (t.equals("inf") || t.equals("+inf") || t.equals("-inf")) {
            return unbounded;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    static Optional<Concept> concept(String text) {
        Matcher m = CONCEPT.matcher(text.strip());
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(new Concept(m.group(1), m.group(2), m.group(3)));
    }

    private static String article(Kind kind) {
        return switch (kind) {
            case DECISION -> "a decision";
            case ALTERNATIVE -> "an alternative";
            case CHANCE -> "a chance";
            case EVENT -> "an event";
            case OUTCOME -> "an outcome";
        };
    }

    /** The words before the first {@code name=value} attribute, joined by one space. */
    static String head(String rest) {
        StringBuilder head = new StringBuilder();
        for (String word : words(rest)) {
            if (isAttribute(word)) {
                break;
            }
            if (!head.isEmpty()) {
                head.append(' ');
            }
            head.append(word);
        }
        return head.toString();
    }

    /** The {@code name=value} attributes after the head, in written order. */
    static Map<String, String> attributes(String rest) {
        Map<String, String> attributes = new LinkedHashMap<>();
        boolean inHead = true;
        for (String word : words(rest)) {
            if (isAttribute(word)) {
                inHead = false;
                int eq = word.indexOf('=');
                attributes.put(word.substring(0, eq), word.substring(eq + 1));
            } else if (!inHead) {
                attributes.put(word, "");
            }
        }
        return attributes;
    }

    private static boolean isAttribute(String word) {
        int eq = word.indexOf('=');
        if (eq <= 0 || word.startsWith("k:") || word.startsWith("\"") || word.startsWith("@id=")) {
            return false;
        }
        return word.substring(0, eq).matches("@?[A-Za-z][A-Za-z0-9_-]*");
    }

    /** Splits on whitespace, keeping bracketed labels and quoted text whole. */
    static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                word.append(c);
                if (c == '\\' && i + 1 < text.length()) {
                    word.append(text.charAt(++i));
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            switch (c) {
                case '"' -> {
                    quoted = true;
                    word.append(c);
                }
                case '[' -> {
                    depth++;
                    word.append(c);
                }
                case ']' -> {
                    depth = Math.max(0, depth - 1);
                    word.append(c);
                }
                case ' ', '\t' -> {
                    if (depth > 0) {
                        word.append(c);
                    } else if (!word.isEmpty()) {
                        words.add(word.toString());
                        word.setLength(0);
                    }
                }
                default -> word.append(c);
            }
        }
        if (!word.isEmpty()) {
            words.add(word.toString());
        }
        return words;
    }
}
