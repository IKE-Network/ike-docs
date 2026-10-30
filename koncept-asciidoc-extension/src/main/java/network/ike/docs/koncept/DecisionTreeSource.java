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
 */
final class DecisionTreeSource {

    private DecisionTreeSource() {
    }

    /** The kinds of vertex. */
    enum Kind { DECISION, ALTERNATIVE, CHANCE, EVENT, OUTCOME }

    /** The kinds of measure a vertex carries. */
    enum MeasureKind { PROBABILITY, MONEY, HEALTH, PARAMETER }

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
     * no operands). A parameter also names the quantity it measures.
     */
    record Measure(MeasureKind kind, Interval bounds, Concept semantic, String basis, String source, Concept quantity,
                   Derivation derivation, Concept derivationConcept, List<Concept> operands, int line) {
        boolean stated() {
            return bounds != null;
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
            return measures.stream().filter(m -> m.kind() == MeasureKind.MONEY || m.kind() == MeasureKind.HEALTH).toList();
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

    /** One raw line with its nesting resolved. */
    private record Line(int number, int indent, boolean vertex, String word, String rest, List<Line> children) {
    }

    /** Reads the block lines into a decision tree. */
    static Parsed parse(List<String> raw) {
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
        if (!root.vertex() || !"decision".equals(root.word())) {
            problems.add("line " + root.number() + ": the root is not a decision");
            return new Parsed(null, problems);
        }
        Node tree = vertex(root, Kind.DECISION, false, problems);
        return new Parsed(tree, problems);
    }

    private static Node vertex(Line line, Kind kind, boolean residual, List<String> problems) {
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
                measure(child, node, problems);
                continue;
            }
            Node childNode = switch (child.word()) {
                case "alternative" -> vertex(child, Kind.ALTERNATIVE, false, problems);
                case "chance" -> vertex(child, Kind.CHANCE, false, problems);
                case "event" -> vertex(child, Kind.EVENT, false, problems);
                case "residual" -> vertex(child, Kind.EVENT, true, problems);
                case "decision" -> vertex(child, Kind.DECISION, false, problems);
                case "outcome" -> vertex(child, Kind.OUTCOME, false, problems);
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

    private static void measure(Line line, Node node, List<String> problems) {
        MeasureKind kind = switch (line.word().toLowerCase(Locale.ROOT)) {
            case "probability", "p" -> MeasureKind.PROBABILITY;
            case "money" -> MeasureKind.MONEY;
            case "health" -> MeasureKind.HEALTH;
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
        List<String> words = words(head(line.rest()));
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word.startsWith("k:")) {
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
        if (unit == null && !share) {
            problems.add("line " + line.number() + ": a cost names its measure semantic"
                    + (derivationConcept != null ? " after in" : " after its interval"));
        }
        Map<String, String> attributes = attributes(line.rest());
        String basis = attributes.get("basis");
        if (basis != null && !List.of("estimated", "cited", "derived").contains(basis.toLowerCase(Locale.ROOT))) {
            problems.add("line " + line.number() + ": a basis is estimated, cited or derived, not " + basis);
            basis = null;
        }
        if (basis == null && derivationConcept != null) {
            basis = "derived";
        }
        attributes.keySet().forEach(name -> {
            if (!List.of("basis", "source", "@vertex").contains(name)) {
                problems.add("line " + line.number() + ": unknown attribute of a measure: " + name);
            }
        });
        node.measures.add(new Measure(kind, bounds, unit, basis, attributes.get("source"), quantity,
                derivation, derivationConcept, List.copyOf(operands), line.number()));
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
