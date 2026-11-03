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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code complex-concept} block read store-free for the documentation build: the twin of
 * {@code ComplexConceptText} in {@code komet-claude-plugin}, which reads the same grammar against
 * the knowledge base ({@code dev-decision-point-knowledge}; IKE-Network/ike-issues#812).
 *
 * <pre>
 * concept: k:SurveillanceDecision[Surveillance imaging decision after aneurysm seal] @id=rda-surveillance-decision
 *   parent: k:ClinicalDecision[Clinical decision] @id=…
 *   expansion: focused k:ImagingDecision[Surveillance imaging decision] @id=rda-imaging-decision
 *     prerequisite: k:AneurysmSealed[Intracranial aneurysm sealed by endovascular treatment] @id=…
 *       value = [1,1] k:Presence[presence] @id=…
 * </pre>
 * or {@code expansion: uniform and|or <domain>} with {@code member:} lines. The parent defaults
 * to the focus, as the architecture prescribes. Parsing never refuses: what cannot be read is a
 * problem, and an identifier is never resolved here; whether it is a SNOMED CT identifier, a UUID
 * or a symbol local to the document is read from its form.
 */
final class ComplexConceptSource {

    private static final Pattern LINE = Pattern.compile("^([A-Za-z][A-Za-z0-9_-]*)\\s*(:|=)\\s*(.*)$");
    private static final Pattern TOKEN = Pattern.compile("^k:([^\\[\\s]*)\\[(.*?)]\\s*(?:@id=(\\S+))?\\s*(.*)$");
    private static final Pattern INTERVAL = Pattern.compile("^([\\[(])\\s*(\\S*?)\\s*,\\s*(\\S*?)\\s*([\\])])\\s*(.*)$");
    private static final Pattern ATTRIBUTE = Pattern.compile("([A-Za-z][A-Za-z0-9_-]*)=(\\S+)");
    private static final Pattern SCTID = Pattern.compile("^\\d{6,18}$");

    private ComplexConceptSource() {
    }

    /** A concept the block names: its key, its label, and the identifier after {@code @id}. */
    record Ref(String key, String label, String identifier) {
        /** The label to show: the written label, else the key, else the identifier. */
        String shown() {
            if (label != null && !label.isBlank()) {
                return label;
            }
            return key != null && !key.isBlank() ? key : identifier == null ? "" : identifier;
        }

        /** Whether the identifier is a SNOMED CT identifier. */
        boolean sctid() {
            return identifier != null && SCTID.matcher(identifier).matches();
        }

        /** Whether the identifier is one or more UUIDs. */
        boolean uuid() {
            if (identifier == null) {
                return false;
            }
            for (String part : identifier.split(",")) {
                try {
                    UUID.fromString(part.strip());
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            return true;
        }

        /** Whether the identifier names no published concept: a symbol local to the document. */
        boolean local() {
            return !sctid() && !uuid();
        }
    }

    /** A bound pair; a null side is unbounded. */
    record Interval(Double lower, Double upper, boolean includeLower, boolean includeUpper) {
    }

    /** One prerequisite: an ANF performance statement whose circumstance fixes the result. */
    record Prerequisite(Ref topic, Interval value, Ref unit) {
    }

    sealed interface Shape permits Focused, Uniform {
    }

    record Focused(Ref focus, List<Prerequisite> prerequisites) implements Shape {
    }

    record Uniform(String conjunction, Ref domain, List<Ref> members, String unsafe) implements Shape {
    }

    /** A specification: the concept, its stated parent if any, its expansion if any. */
    record Spec(Ref concept, Ref parent, Shape shape) {
        /** The parent of the description logic definition: the stated one, else a focused expansion's focus. */
        Optional<Ref> definitionParent() {
            if (parent != null) {
                return Optional.of(parent);
            }
            return shape instanceof Focused focused ? Optional.of(focused.focus()) : Optional.empty();
        }
    }

    /** What the block reads as: the spec, if its root reads, and the problems found on the way. */
    record Parsed(Spec spec, List<String> problems) {
        /** The identifiers that name no published concept, in the order they first appear. */
        List<String> localSymbols() {
            Set<String> symbols = new LinkedHashSet<>();
            if (spec != null) {
                for (Ref ref : refs(spec)) {
                    if (ref.local() && ref.identifier() != null) {
                        symbols.add(ref.identifier());
                    }
                }
            }
            return List.copyOf(symbols);
        }
    }

    private record Line(int number, int indent, String word, boolean value, String rest, List<Line> children) {
        Line(int number, int indent, String word, boolean value, String rest) {
            this(number, indent, word, value, rest, new ArrayList<>());
        }
    }

    /** Reads the block's lines. */
    static Parsed parse(List<String> lines) {
        List<String> problems = new ArrayList<>();
        List<Line> roots = new ArrayList<>();
        List<Line> stack = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank() || raw.strip().startsWith("```")) {
                continue;
            }
            int indent = raw.length() - raw.stripLeading().length();
            Matcher m = LINE.matcher(raw.strip());
            if (!m.matches()) {
                problems.add("line " + (i + 1) + " is not a word: line");
                continue;
            }
            Line line = new Line(i + 1, indent, m.group(1).toLowerCase(Locale.ROOT), "=".equals(m.group(2)), m.group(3).strip());
            while (!stack.isEmpty() && stack.get(stack.size() - 1).indent() >= indent) {
                stack.remove(stack.size() - 1);
            }
            if (stack.isEmpty()) {
                roots.add(line);
            } else {
                stack.get(stack.size() - 1).children().add(line);
            }
            stack.add(line);
        }
        if (roots.size() != 1) {
            problems.add(roots.isEmpty() ? "the block is empty" : "the block has more than one root");
            return new Parsed(null, problems);
        }
        Reader reader = new Reader(problems);
        Line root = roots.get(0);
        if (root.value() || !"concept".equals(root.word())) {
            problems.add("the root is not a concept line");
            return new Parsed(null, problems);
        }
        Ref concept = reader.ref(root, root.rest(), "the complex concept");
        if (concept == null) {
            return new Parsed(null, problems);
        }
        Ref parent = null;
        Shape shape = null;
        for (Line child : root.children()) {
            switch (child.word()) {
                case "parent" -> parent = reader.ref(child, child.rest(), "the parent");
                case "expansion" -> {
                    if (shape != null) {
                        problems.add("line " + child.number() + ": a second expansion; a specification has one");
                    } else {
                        shape = reader.expansion(child, concept);
                    }
                }
                default -> problems.add("line " + child.number() + ": unknown word " + child.word());
            }
        }
        return new Parsed(new Spec(concept, parent, shape), problems);
    }

    /** What the spec still lacks to be complete, as the builder in Komet lists it. */
    static List<String> completeness(Spec spec) {
        List<String> missing = new ArrayList<>();
        if (spec.definitionParent().isEmpty()) {
            missing.add("a definition: a parent line");
        }
        switch (spec.shape()) {
            case null -> missing.add("an expansion, uniform or focused");
            case Uniform uniform -> {
                if (uniform.domain() == null) {
                    missing.add("the uniform expansion's measurement domain");
                }
                if (uniform.members().size() < 2) {
                    missing.add("at least two members");
                }
                if ("or".equals(uniform.conjunction()) && uniform.unsafe() == null) {
                    missing.add("how the OR conjunction handles a non-absent value: unsafe=refuse, flag or retain");
                }
            }
            case Focused focused -> {
                if (focused.prerequisites().isEmpty()) {
                    missing.add("at least one prerequisite");
                }
                for (Prerequisite prerequisite : focused.prerequisites()) {
                    if (prerequisite.value() == null) {
                        missing.add("a fixed value for " + prerequisite.topic().shown());
                    }
                }
            }
        }
        return missing;
    }

    /** A prerequisite's value in words: present, absent, a number with its unit, a bound, or "any". */
    static String valueWords(Prerequisite prerequisite) {
        Interval value = prerequisite.value();
        if (value == null) {
            return "(no value)";
        }
        String unit = prerequisite.unit() == null ? "" : prerequisite.unit().shown();
        boolean presence = unit.toLowerCase(Locale.ROOT).startsWith("presence");
        Double lower = value.lower();
        Double upper = value.upper();
        if (presence && lower != null && upper != null && lower == 0 && upper == 0) {
            return "absent";
        }
        if (presence && lower != null && lower == 1 && (upper == null || upper == 1)) {
            return "present";
        }
        // a count is its number; a lower-case unit word is plural beside any number but one
        String suffix = unit.isEmpty() || unit.equalsIgnoreCase("count") ? "" : " " + unit;
        boolean one = lower != null && upper != null && lower.equals(upper) && lower == 1;
        if (!suffix.isEmpty() && !one && unit.matches("[a-z]+") && !unit.endsWith("s")) {
            suffix = suffix + "s";
        }
        if (lower != null && upper != null && lower.equals(upper)) {
            return number(lower) + suffix;
        }
        if (lower != null && upper == null) {
            return (value.includeLower() ? "at least " : "more than ") + number(lower) + suffix;
        }
        if (lower == null && upper != null) {
            return (value.includeUpper() ? "at most " : "less than ") + number(upper) + suffix;
        }
        if (lower == null) {
            return "any" + suffix;
        }
        return number(lower) + " to " + number(upper) + suffix;
    }

    /** The interval as written: {@code [0, 2]}, an open side in a parenthesis, an unbounded side as ∞. */
    static String interval(Interval value) {
        if (value == null) {
            return "";
        }
        return (value.includeLower() ? "[" : "(") + (value.lower() == null ? "−∞" : number(value.lower()))
                + ", " + (value.upper() == null ? "∞" : number(value.upper())) + (value.includeUpper() ? "]" : ")");
    }

    /** What the complex concept is, in labels and without its own name: "a kind of …, given …". */
    static String describe(Spec spec) {
        List<String> parts = new ArrayList<>();
        spec.definitionParent().ifPresent(parent -> parts.add("a kind of " + lowerFirst(parent.shown())));
        switch (spec.shape()) {
            case null -> {
            }
            case Focused focused -> {
                if (!focused.prerequisites().isEmpty()) {
                    List<String> clauses = new ArrayList<>();
                    for (Prerequisite prerequisite : focused.prerequisites()) {
                        clauses.add(lowerFirst(prerequisite.topic().shown()) + " " + valueWords(prerequisite));
                    }
                    parts.add("given " + join(clauses));
                }
            }
            case Uniform uniform -> {
                List<String> names = new ArrayList<>();
                for (Ref member : uniform.members()) {
                    names.add(lowerFirst(member.shown()));
                }
                parts.add(("and".equals(uniform.conjunction()) ? "every one of " : "any of ") + join(names));
            }
        }
        return String.join(", ", parts);
    }

    static String number(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    private static String join(List<String> parts) {
        if (parts.size() <= 1) {
            return String.join("", parts);
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    /** A label's first letter in lower case, unless the label starts with an acronym. */
    private static String lowerFirst(String label) {
        if (label.length() > 1 && Character.isUpperCase(label.charAt(0)) && !Character.isUpperCase(label.charAt(1))) {
            return Character.toLowerCase(label.charAt(0)) + label.substring(1);
        }
        return label;
    }

    private static List<Ref> refs(Spec spec) {
        List<Ref> refs = new ArrayList<>();
        refs.add(spec.concept());
        if (spec.parent() != null) {
            refs.add(spec.parent());
        }
        switch (spec.shape()) {
            case null -> {
            }
            case Focused focused -> {
                refs.add(focused.focus());
                for (Prerequisite prerequisite : focused.prerequisites()) {
                    refs.add(prerequisite.topic());
                    if (prerequisite.unit() != null) {
                        refs.add(prerequisite.unit());
                    }
                }
            }
            case Uniform uniform -> {
                if (uniform.domain() != null) {
                    refs.add(uniform.domain());
                }
                refs.addAll(uniform.members());
            }
        }
        return refs;
    }

    // ── The reader ───────────────────────────────────────────────────────────

    private static final class Reader {
        private final List<String> problems;
        /** The label each identifier was first given, so {@code k:Key[]} names what the first mention named. */
        private final Map<String, String> labels = new HashMap<>();

        private Reader(List<String> problems) {
            this.problems = problems;
        }

        private Ref ref(Line line, String text, String what) {
            Matcher m = TOKEN.matcher(text == null ? "" : text.strip());
            if (!m.matches()) {
                problems.add("line " + line.number() + ": " + what + " is not a k: token");
                return null;
            }
            String key = m.group(1);
            String label = m.group(2);
            String identifier = m.group(3);
            if (identifier == null && key.contains("=")) {
                // the Koncept Badge form, k:sctid=…[label]: the identifier is what follows the =
                identifier = key.substring(key.indexOf('=') + 1);
                key = "";
            }
            if (identifier == null) {
                problems.add("line " + line.number() + ": " + what + " carries no @id");
                return null;
            }
            if (label == null || label.isBlank()) {
                label = labels.getOrDefault(identifier, label);
            } else {
                labels.putIfAbsent(identifier, label);
            }
            return new Ref(key, label, identifier);
        }

        private static String trailing(String text) {
            Matcher m = TOKEN.matcher(text == null ? "" : text.strip());
            return m.matches() ? m.group(4) : "";
        }

        private static Optional<String> attribute(String trailing, String name) {
            Matcher m = ATTRIBUTE.matcher(trailing);
            while (m.find()) {
                if (m.group(1).equals(name)) {
                    return Optional.of(m.group(2).toLowerCase(Locale.ROOT));
                }
            }
            return Optional.empty();
        }

        private Shape expansion(Line line, Ref concept) {
            String[] words = line.rest().split("\\s+", 2);
            String kind = words[0].toLowerCase(Locale.ROOT);
            String rest = words.length > 1 ? words[1] : "";
            switch (kind) {
                case "focused" -> {
                    Ref focus = ref(line, rest, "the focus");
                    if (focus == null) {
                        return null;
                    }
                    if (focus.identifier().equals(concept.identifier())) {
                        problems.add("line " + line.number() + ": the focus is the complex concept itself");
                    }
                    List<Prerequisite> prerequisites = new ArrayList<>();
                    for (Line child : line.children()) {
                        if (!child.value() && "prerequisite".equals(child.word())) {
                            Ref topic = ref(child, child.rest(), "a prerequisite");
                            if (topic != null) {
                                prerequisites.add(prerequisite(child, topic));
                            }
                        } else {
                            problems.add("line " + child.number() + ": a focused expansion holds prerequisite lines, not " + child.word());
                        }
                    }
                    return new Focused(focus, List.copyOf(prerequisites));
                }
                case "uniform" -> {
                    String[] more = rest.split("\\s+", 2);
                    String conjunction = more[0].toLowerCase(Locale.ROOT);
                    if (!"and".equals(conjunction) && !"or".equals(conjunction)) {
                        problems.add("line " + line.number() + ": a uniform expansion says and or or after uniform");
                        return null;
                    }
                    String domainText = more.length > 1 ? more[1] : "";
                    Ref domain = domainText.isBlank() ? null : ref(line, domainText, "the measurement domain");
                    String unsafe = attribute(trailing(domainText), "unsafe")
                            .or(() -> attribute(domainText, "unsafe")).orElse(null);
                    List<Ref> members = new ArrayList<>();
                    for (Line child : line.children()) {
                        if (!child.value() && "member".equals(child.word())) {
                            Ref member = ref(child, child.rest(), "a member");
                            if (member != null) {
                                members.add(member);
                            }
                        } else {
                            problems.add("line " + child.number() + ": a uniform expansion holds member lines, not " + child.word());
                        }
                    }
                    return new Uniform(conjunction, domain, List.copyOf(members), unsafe);
                }
                default -> {
                    problems.add("line " + line.number() + ": an expansion is focused or uniform, not " + words[0]);
                    return null;
                }
            }
        }

        private Prerequisite prerequisite(Line line, Ref topic) {
            Interval value = null;
            Ref unit = null;
            for (Line child : line.children()) {
                if (!child.value() || !"value".equals(child.word())) {
                    problems.add("line " + child.number() + ": a prerequisite holds one value line, not " + child.word());
                    continue;
                }
                Matcher m = INTERVAL.matcher(child.rest());
                if (!m.matches()) {
                    problems.add("line " + child.number() + ": a value starts with an interval, [lo,hi]");
                    continue;
                }
                value = new Interval(bound(m.group(2)), bound(m.group(3)), "[".equals(m.group(1)), "]".equals(m.group(4)));
                String rest = m.group(5).strip();
                if (!rest.isEmpty()) {
                    unit = ref(child, rest, "the value's unit");
                }
            }
            return new Prerequisite(topic, value, unit);
        }

        private static Double bound(String s) {
            String t = s == null ? "" : s.strip().toLowerCase(Locale.ROOT);
            if (t.isEmpty() || t.equals("*") || t.endsWith("inf") || t.endsWith("∞")) {
                return null;
            }
            try {
                return Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
