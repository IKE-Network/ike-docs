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
import network.ike.docs.koncept.DecisionTreeSource.Concept;
import network.ike.docs.koncept.DecisionTreeSource.Interval;

/**
 * The calculator profile's two blocks read store-free for the documentation build
 * ({@code dev-calculator-blocks}): the {@code [calculator]} block, the method, and the
 * {@code [calculate]} block, the record that binds a document's values to its inputs. The grammar
 * is the one the Komet assistant plugin's {@code CalculatorText} and {@code CalculationText}
 * read; the two are kept in step by hand, as {@link DecisionTreeSource} is with the plugin's tree
 * reader. Identities are symbols here, never resolved, and a statement reference cannot be read
 * by a build with no store, so it reads as a clarification.
 */
final class CalculatorSource {

    private CalculatorSource() {
    }

    /** The derivation kinds an equation term may be computed with, and the formula's. */
    enum Kind {
        NATURAL_LOGARITHM("NaturalLogarithm", 1), PRODUCT("Product", -1), COMPLEMENT("Complement", 1),
        COX_SURVIVAL("CoxSurvival", 2);

        final String label;
        final int arity;

        Kind(String label, int arity) {
            this.label = label;
            this.arity = arity;
        }

        static Optional<Kind> of(Concept concept) {
            for (String name : new String[] {concept.label(), concept.key()}) {
                if (name == null) {
                    continue;
                }
                String bare = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
                if (bare.endsWith("derivation")) {
                    bare = bare.substring(0, bare.length() - "derivation".length());
                }
                switch (bare) {
                    case "naturallogarithm", "ln", "log" -> {
                        return Optional.of(NATURAL_LOGARITHM);
                    }
                    case "product" -> {
                        return Optional.of(PRODUCT);
                    }
                    case "complement" -> {
                        return Optional.of(COMPLEMENT);
                    }
                    case "coxsurvival", "cox" -> {
                        return Optional.of(COX_SURVIVAL);
                    }
                    default -> { }
                }
            }
            return Optional.empty();
        }
    }

    record Conversion(Concept fromSemantic, Interval factor) {
    }

    record Input(Concept quantity, Concept semantic, Interval domain, boolean required, List<Concept> values,
                 String whenMissing, Interval missingBounds, Concept missingConcept, List<Conversion> conversions, int line) {
        boolean conceptValued() {
            return !values.isEmpty();
        }

        String key() {
            return DecisionTreeRollback.quantityKey(quantity);
        }

        String name() {
            return quantity.shown();
        }
    }

    /** A quantity derived from inputs and other terms; a null kind reads the one quantity named as it is. */
    record Term(Concept quantity, Kind kind, Concept reads, List<Concept> operands, int line) {
        String key() {
            return DecisionTreeRollback.quantityKey(quantity);
        }
    }

    record Coefficient(Concept quantity, Interval value, String basis, String source, int line) {
        String key() {
            return DecisionTreeRollback.quantityKey(quantity);
        }
    }

    record Condition(Concept input, Concept value, int line) {
    }

    record Stratum(Concept concept, List<Condition> conditions, List<Coefficient> coefficients, int line) {
    }

    record Band(Concept concept, Interval interval, String source, int line) {
    }

    record Output(Concept topic, Concept semantic, Interval domain, double scale, int line) {
    }

    record Formula(Kind kind, List<Concept> operands, int line) {
    }

    record Calculator(Concept concept, String source, Output output, List<Input> inputs, List<Term> terms,
                      List<Stratum> strata, Formula formula, List<Band> bands) {
        Optional<Input> input(String key) {
            return inputs.stream().filter(input -> input.key().equals(key)).findFirst();
        }

        Optional<Term> term(String key) {
            return terms.stream().filter(term -> term.key().equals(key)).findFirst();
        }

        /** The keys a calculate block or a tree may name the calculator's result by. */
        List<String> keys() {
            List<String> keys = new ArrayList<>();
            keys.add(DecisionTreeRollback.quantityKey(concept));
            if (output != null && output.topic() != null) {
                keys.add(DecisionTreeRollback.quantityKey(output.topic()));
            }
            return keys;
        }
    }

    record Parsed(Calculator calculator, List<String> problems) {
    }

    // ── The record block ─────────────────────────────────────────────────────

    sealed interface Binding permits Inline, InlineConcept, Missing, Clarify, Statement {
    }

    record Inline(Interval bounds, Concept semantic) implements Binding {
    }

    record InlineConcept(Concept value) implements Binding {
    }

    record Missing() implements Binding {
    }

    record Clarify(String question) implements Binding {
    }

    record Statement(String identifier, String policy) implements Binding {
    }

    record Calculation(Concept calculator, Map<String, Binding> bindings, Map<String, Inline> assumptions,
                       Map<String, Concept> inputs, List<String> problems) {
    }

    // ── Grammar, in step with the plugin's BlockLines and Tokens ─────────────

    private static final Pattern LINE = Pattern.compile("^([A-Za-z][A-Za-z0-9_-]*)\\s*(:|=)\\s*(.*)$");
    private static final Pattern HEADER = Pattern.compile("^\\[[A-Za-z].*]$");
    private static final Pattern DELIMITER = Pattern.compile("^-{2,}$");
    private static final Pattern INTERVAL = Pattern.compile("^([\\[(])\\s*(\\S+?)\\s*,\\s*(\\S+?)\\s*([\\])])\\s*(.*)$");

    private record Line(int number, int indent, boolean vertex, String word, String rest, List<Line> children) {
    }

    private static List<Line> lines(List<String> raw, List<String> problems) {
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
        return roots;
    }

    /** Whether a block opens with a line of the given word: how a processor tells its block. */
    static boolean opensWith(List<String> raw, String word) {
        List<Line> roots = lines(raw, new ArrayList<>());
        return roots.size() == 1 && roots.get(0).vertex() && word.equals(roots.get(0).word());
    }

    // ── The method ───────────────────────────────────────────────────────────

    static Parsed parse(List<String> raw) {
        List<String> problems = new ArrayList<>();
        List<Line> roots = lines(raw, problems);
        if (roots.size() != 1) {
            problems.add("a calculator has one root line, not " + roots.size());
            return new Parsed(null, problems);
        }
        Line root = roots.get(0);
        if (!root.vertex() || !"calculator".equals(root.word())) {
            problems.add("line " + root.number() + ": the root is not a calculator");
            return new Parsed(null, problems);
        }
        Head rootHead = head(root, problems);
        Concept concept = rootHead.first().orElse(null);
        if (concept == null) {
            problems.add("line " + root.number() + ": a calculator names its concept");
        }
        Map<String, String> rootAttributes = DecisionTreeSource.attributes(root.rest());
        String source = rootAttributes.get("source");
        Output output = null;
        List<Input> inputs = new ArrayList<>();
        List<Term> terms = new ArrayList<>();
        List<Stratum> strata = new ArrayList<>();
        Formula formula = null;
        List<Band> bands = new ArrayList<>();
        for (Line child : root.children()) {
            String word = child.word().toLowerCase(Locale.ROOT);
            if (!child.vertex()) {
                switch (word) {
                    case "output" -> output = output(child, problems);
                    case "term" -> term(child, problems).ifPresent(terms::add);
                    case "formula" -> formula = formula(child, problems);
                    default -> problems.add("line " + child.number() + ": unknown value of a calculator: " + child.word());
                }
                continue;
            }
            switch (word) {
                case "input" -> inputs.add(input(child, problems));
                case "stratum" -> strata.add(stratum(child, problems));
                case "band" -> band(child, problems).ifPresent(bands::add);
                default -> problems.add("line " + child.number() + ": unknown kind of vertex under a calculator: " + child.word());
            }
        }
        if (output == null) {
            problems.add("a calculator has no output");
        }
        if (inputs.isEmpty()) {
            problems.add("a calculator has no input");
        }
        if (strata.isEmpty()) {
            problems.add("a calculator has no stratum");
        }
        if (formula == null) {
            problems.add("a calculator has no formula");
        }
        return new Parsed(new Calculator(concept, source, output, inputs, terms, strata, formula, bands), problems);
    }

    private static Output output(Line line, List<String> problems) {
        Head head = head(line, problems);
        if (head.first().isEmpty()) {
            problems.add("line " + line.number() + ": an output names its topic");
        }
        if (head.in() == null) {
            problems.add("line " + line.number() + ": an output names its measure semantic after in");
        }
        double scale = 1;
        for (Map.Entry<String, String> attribute : DecisionTreeSource.attributes(line.rest()).entrySet()) {
            switch (attribute.getKey()) {
                case "scale" -> {
                    try {
                        scale = Double.parseDouble(attribute.getValue());
                    } catch (NumberFormatException e) {
                        problems.add("line " + line.number() + ": a scale is a number: " + attribute.getValue());
                    }
                }
                case "@vertex" -> { }
                default -> problems.add("line " + line.number() + ": unknown attribute of an output: " + attribute.getKey());
            }
        }
        return new Output(head.first().orElse(null), head.in(), head.domain(), scale, line.number());
    }

    private static Input input(Line line, List<String> problems) {
        Head head = head(line, problems);
        Concept quantity = head.first().orElse(null);
        if (quantity == null) {
            problems.add("line " + line.number() + ": an input names its quantity");
            quantity = new Concept("Input", "input", null);
        }
        boolean required = head.flags().contains("required");
        List<Concept> values = new ArrayList<>();
        String whenMissing = null;
        for (Map.Entry<String, String> attribute : DecisionTreeSource.attributes(line.rest()).entrySet()) {
            switch (attribute.getKey()) {
                case "topic", "@vertex" -> { }
                case "required" -> required = true;
                case "values" -> {
                    for (String identifier : attribute.getValue().split(";")) {
                        if (!identifier.isBlank()) {
                            values.add(new Concept(identifier.strip(), identifier.strip(), identifier.strip()));
                        }
                    }
                }
                case "when-missing" -> {
                    String policy = attribute.getValue().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
                    whenMissing = switch (policy) {
                        case "notdeterminable" -> "not-determinable";
                        case "plausible", "plausiblerange" -> "plausible";
                        case "assumed", "assume", "assumedvalue" -> "assumed";
                        default -> {
                            problems.add("line " + line.number() + ": a when-missing policy is not-determinable, plausible or assumed, not " + attribute.getValue());
                            yield null;
                        }
                    };
                }
                default -> problems.add("line " + line.number() + ": unknown attribute of an input: " + attribute.getKey());
            }
        }
        Interval missingBounds = null;
        Concept missingConcept = null;
        List<Conversion> conversions = new ArrayList<>();
        for (Line child : line.children()) {
            if (child.vertex()) {
                problems.add("line " + child.number() + ": an input carries values, not vertices: " + child.word());
                continue;
            }
            Head h = head(child, problems);
            switch (child.word().toLowerCase(Locale.ROOT)) {
                case "conversion" -> {
                    if (h.interval() == null || h.first().isEmpty()) {
                        problems.add("line " + child.number() + ": a conversion states its factor and the semantic it converts from");
                    } else {
                        conversions.add(new Conversion(h.first().get(), h.interval()));
                    }
                }
                case "recency" -> { }
                case "missing" -> {
                    if (h.interval() != null) {
                        missingBounds = h.interval();
                    } else if (h.first().isPresent()) {
                        missingConcept = h.first().get();
                    } else {
                        problems.add("line " + child.number() + ": a missing value states bounds or a concept");
                    }
                }
                default -> problems.add("line " + child.number() + ": unknown value of an input: " + child.word());
            }
        }
        if (head.in() == null && values.isEmpty()) {
            problems.add("line " + line.number() + ": an input names the measure semantic it expects, or the concept values it admits");
        }
        if (("plausible".equals(whenMissing) || "assumed".equals(whenMissing)) && missingBounds == null && missingConcept == null) {
            problems.add("line " + line.number() + ": an input swept or assumed when missing states its missing value");
        }
        return new Input(quantity, head.in(), head.domain(), required, values, whenMissing, missingBounds, missingConcept,
                conversions, line.number());
    }

    private static Optional<Term> term(Line line, List<String> problems) {
        Head head = head(line, problems);
        if (head.first().isEmpty()) {
            problems.add("line " + line.number() + ": a term names its quantity before from");
            return Optional.empty();
        }
        if (head.from() == null) {
            problems.add("line " + line.number() + ": a term is derived from a kind or reads a quantity after from");
            return Optional.empty();
        }
        Optional<Kind> kind = Kind.of(head.from());
        if (kind.isPresent() && kind.get() != Kind.COX_SURVIVAL) {
            Kind k = kind.get();
            if (k.arity >= 0 ? head.of().size() != k.arity : head.of().size() < 2) {
                problems.add("line " + line.number() + ": a " + k.label + " reads "
                        + (k.arity >= 0 ? k.arity + "" : "two or more") + " operands, not " + head.of().size());
            }
            return Optional.of(new Term(head.first().get(), k, null, List.copyOf(head.of()), line.number()));
        }
        if (!head.of().isEmpty()) {
            problems.add("line " + line.number() + ": a derivation names no kind the build can run: " + head.from().shown());
            return Optional.empty();
        }
        return Optional.of(new Term(head.first().get(), null, head.from(), List.of(), line.number()));
    }

    private static Formula formula(Line line, List<String> problems) {
        Head head = head(line, problems);
        if (head.from() == null) {
            problems.add("line " + line.number() + ": a formula names its kind after from");
            return null;
        }
        Optional<Kind> kind = Kind.of(head.from());
        if (kind.isEmpty() || kind.get() != Kind.COX_SURVIVAL) {
            problems.add("line " + line.number() + ": a formula names no kind the build can run: " + head.from().shown());
            return null;
        }
        if (head.of().size() != 2) {
            problems.add("line " + line.number() + ": a Cox survival formula reads the baseline survival then the mean, not "
                    + head.of().size() + " operands");
        }
        return new Formula(kind.get(), List.copyOf(head.of()), line.number());
    }

    private static Stratum stratum(Line line, List<String> problems) {
        Head head = head(line, problems);
        Concept concept = head.first().orElse(null);
        if (concept == null) {
            problems.add("line " + line.number() + ": a stratum names its concept");
            concept = new Concept("Stratum", "stratum", null);
        }
        List<Condition> conditions = new ArrayList<>();
        List<Coefficient> coefficients = new ArrayList<>();
        for (Line child : line.children()) {
            if (child.vertex()) {
                problems.add("line " + child.number() + ": a stratum carries values, not vertices: " + child.word());
                continue;
            }
            Head h = head(child, problems);
            switch (child.word().toLowerCase(Locale.ROOT)) {
                case "when" -> {
                    if (h.concepts().size() != 2) {
                        problems.add("line " + child.number() + ": a condition names the input then the value it requires");
                    } else {
                        conditions.add(new Condition(h.concepts().get(0), h.concepts().get(1), child.number()));
                    }
                }
                case "coefficient" -> {
                    if (h.interval() == null) {
                        problems.add("line " + child.number() + ": a coefficient begins with its value, [lo,hi]");
                    } else if (h.first().isEmpty()) {
                        problems.add("line " + child.number() + ": a coefficient names the term or constant it belongs to");
                    } else {
                        Map<String, String> attributes = DecisionTreeSource.attributes(child.rest());
                        coefficients.add(new Coefficient(h.first().get(), h.interval(), attributes.get("basis"),
                                attributes.get("source"), child.number()));
                    }
                }
                default -> problems.add("line " + child.number() + ": unknown value of a stratum: " + child.word());
            }
        }
        if (coefficients.isEmpty()) {
            problems.add("line " + line.number() + ": a stratum states no coefficient");
        }
        return new Stratum(concept, conditions, coefficients, line.number());
    }

    private static Optional<Band> band(Line line, List<String> problems) {
        Head head = head(line, problems);
        if (head.first().isEmpty()) {
            problems.add("line " + line.number() + ": a band names its concept");
            return Optional.empty();
        }
        if (head.when() == null) {
            problems.add("line " + line.number() + ": a band states its interval after when");
            return Optional.empty();
        }
        return Optional.of(new Band(head.first().get(), head.when(), DecisionTreeSource.attributes(line.rest()).get("source"),
                line.number()));
    }

    // ── The record ───────────────────────────────────────────────────────────

    static Calculation parseCalculation(List<String> raw) {
        List<String> problems = new ArrayList<>();
        List<Line> roots = lines(raw, problems);
        if (roots.size() != 1 || !roots.get(0).vertex() || !"calculate".equals(roots.get(0).word())) {
            problems.add("a calculation has one calculate root line");
            return new Calculation(null, Map.of(), Map.of(), Map.of(), problems);
        }
        Line root = roots.get(0);
        Concept calculator = DecisionTreeSource.concept(DecisionTreeSource.head(root.rest())).orElse(null);
        if (calculator == null) {
            problems.add("line " + root.number() + ": a calculation names its calculator");
        }
        Map<String, Binding> bindings = new LinkedHashMap<>();
        Map<String, Inline> assumptions = new LinkedHashMap<>();
        Map<String, Concept> inputs = new LinkedHashMap<>();
        for (Line child : root.children()) {
            boolean assume = "assume".equals(child.word());
            if (child.vertex() || !(assume || "bind".equals(child.word()))) {
                problems.add("line " + child.number() + ": a calculation holds bind and assume lines, not " + child.word());
                continue;
            }
            List<String> words = DecisionTreeSource.words(child.rest());
            if (words.isEmpty() || !words.get(0).startsWith("k:")) {
                problems.add("line " + child.number() + ": a " + child.word() + " line names the input first");
                continue;
            }
            String inputText = words.get(0);
            int next = 1;
            if (words.size() > 1 && words.get(1).startsWith("@id=")) {
                inputText += " " + words.get(1);
                next = 2;
            }
            Concept input = DecisionTreeSource.concept(inputText).orElse(null);
            if (input == null) {
                problems.add("line " + child.number() + ": an input is not a k: token: " + inputText);
                continue;
            }
            String key = DecisionTreeRollback.quantityKey(input);
            inputs.put(key, input);
            List<String> rest = words.subList(next, words.size());
            if (rest.isEmpty()) {
                problems.add("line " + child.number() + ": a " + child.word() + " line says what binds");
                continue;
            }
            String first = rest.get(0);
            if (assume) {
                Optional<Binding> value = inline(child, rest, problems);
                value.ifPresent(v -> {
                    if (v instanceof Inline inline) {
                        assumptions.put(key, inline);
                    } else {
                        problems.add("line " + child.number() + ": an assumption is a value, not a concept");
                    }
                });
                continue;
            }
            if (first.startsWith("anf:")) {
                String identifier = first.substring("anf:".length());
                if (identifier.startsWith("@id=")) {
                    identifier = identifier.substring("@id=".length());
                }
                String policy = rest.size() >= 3 && "by".equals(rest.get(1)) ? rest.get(2) : null;
                bindings.put(key, new Statement(identifier, policy));
                continue;
            }
            switch (first.toLowerCase(Locale.ROOT)) {
                case "missing" -> bindings.put(key, new Missing());
                case "clarify" -> bindings.put(key, new Clarify(rest.size() > 1
                        ? unquote(String.join(" ", rest.subList(1, rest.size()))) : ""));
                default -> inline(child, rest, problems).ifPresent(value -> bindings.put(key, value));
            }
        }
        return new Calculation(calculator, bindings, assumptions, inputs, problems);
    }

    private static Optional<Binding> inline(Line line, List<String> words, List<String> problems) {
        String first = words.get(0);
        if (first.startsWith("[") || first.startsWith("(")) {
            Matcher m = INTERVAL.matcher(first);
            if (!m.matches()) {
                problems.add("line " + line.number() + ": not an interval: " + first);
                return Optional.empty();
            }
            Interval bounds = new Interval(bound(m.group(2), Double.NEGATIVE_INFINITY), bound(m.group(3), Double.POSITIVE_INFINITY),
                    !"[".equals(m.group(1)), !"]".equals(m.group(4)));
            Concept semantic = null;
            if (words.size() > 1) {
                String text = words.get(1);
                if (words.size() > 2 && words.get(2).startsWith("@id=")) {
                    text += " " + words.get(2);
                }
                semantic = DecisionTreeSource.concept(text).orElse(null);
            }
            return Optional.of(new Inline(bounds, semantic));
        }
        if (first.startsWith("k:")) {
            String text = first;
            if (words.size() > 1 && words.get(1).startsWith("@id=")) {
                text += " " + words.get(1);
            }
            Optional<Concept> concept = DecisionTreeSource.concept(text);
            if (concept.isEmpty()) {
                problems.add("line " + line.number() + ": not a k: token: " + text);
                return Optional.empty();
            }
            return Optional.of(new InlineConcept(concept.get()));
        }
        problems.add("line " + line.number() + ": an unexpected word: " + first);
        return Optional.empty();
    }

    private static String unquote(String s) {
        String t = s.strip();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // ── Heads ────────────────────────────────────────────────────────────────

    /** A line's head read into roles: the leading concepts, and what follows each keyword. */
    private record Head(List<Concept> concepts, Concept in, Interval interval, Interval domain, Interval when,
                        Concept from, List<Concept> of, List<String> flags) {
        Optional<Concept> first() {
            return concepts.isEmpty() ? Optional.empty() : Optional.of(concepts.get(0));
        }
    }

    private static Head head(Line line, List<String> problems) {
        List<Concept> concepts = new ArrayList<>();
        Concept in = null;
        Interval interval = null;
        Interval domain = null;
        Interval when = null;
        Concept from = null;
        List<Concept> of = new ArrayList<>();
        List<String> flags = new ArrayList<>();
        String mode = "";
        List<String> words = DecisionTreeSource.words(DecisionTreeSource.head(line.rest()));
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word.startsWith("k:")) {
                String text = word;
                if (i + 1 < words.size() && words.get(i + 1).startsWith("@id=")) {
                    text = word + " " + words.get(++i);
                }
                Optional<Concept> token = DecisionTreeSource.concept(text);
                if (token.isEmpty()) {
                    problems.add("line " + line.number() + ": not a k: token: " + text);
                    continue;
                }
                switch (mode) {
                    case "from" -> from = token.get();
                    case "of" -> of.add(token.get());
                    case "in" -> {
                        in = token.get();
                        mode = "";
                    }
                    default -> concepts.add(token.get());
                }
            } else if (word.startsWith("[") || word.startsWith("(")) {
                Matcher m = INTERVAL.matcher(word);
                if (!m.matches()) {
                    problems.add("line " + line.number() + ": not an interval: " + word);
                    continue;
                }
                Interval parsed = new Interval(bound(m.group(2), Double.NEGATIVE_INFINITY), bound(m.group(3), Double.POSITIVE_INFINITY),
                        !"[".equals(m.group(1)), !"]".equals(m.group(4)));
                switch (mode) {
                    case "domain" -> domain = parsed;
                    case "when" -> when = parsed;
                    default -> interval = parsed;
                }
                mode = "";
            } else if (List.of("from", "of", "in", "domain", "when").contains(word.toLowerCase(Locale.ROOT))) {
                mode = word.toLowerCase(Locale.ROOT);
            } else if ("required".equalsIgnoreCase(word)) {
                flags.add("required");
            } else if ("from".equals(mode) && from == null && Kind.of(new Concept(word, "", null)).isPresent()) {
                from = new Concept(word, "", null);
            } else {
                problems.add("line " + line.number() + ": an unexpected word: " + word);
            }
        }
        return new Head(concepts, in, interval, domain, when, from, of, flags);
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
}
