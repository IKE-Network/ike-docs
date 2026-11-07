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

import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;

import network.ike.docs.koncept.DecisionTreeSource.Concept;
import network.ike.docs.koncept.DecisionTreeSource.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code [anf]} block in the documentation build ({@code dev-anf-adoc-representation}): the
 * static projection of the ANF statement card the assistant surface draws for the same block. It
 * reads the statement's fields store-free and puts in its place a small table, the statement's
 * type and name above, one row per field beneath, each concept a Koncept chip when its identifier
 * is a SNOMED CT identifier or a UUID, a candidate set in italics and marked as one.
 *
 * <pre>
 * [anf,type=request,id=req-scan]
 * --
 * topic::   k:MraHead[Magnetic resonance angiography of head without contrast] @id=437731000119100
 * timing::  [2,2] k:Year[year] @id=258707000
 * purpose:: candidate[detection of reopening of the sealed aneurysm] disp=PENDING
 * --
 * </pre>
 *
 * A block that names itself, {@code id=…}, is remembered by that name, so a {@code [decision-tree]}
 * later in the document whose nodes hold it, {@code statement=anf:req-scan}, lists what each node
 * states, and a {@code [graphlet-model]} whose parameters read it, {@code parameter = [0.15,0.15]
 * anf:est-material-problem}, takes the range its {@code result::} determined and the scale the range
 * is on ({@link #measure(Statement)}). An element block whose body is still empty shows its sentence
 * and that it is not yet lifted. The output is AsciiDoc re-parsed in place, so it is backend-agnostic.
 */
@Name("anf")
@Contexts({Contexts.OPEN, Contexts.LISTING})
@ContentModel(ContentModel.RAW)
public class AnfBlockProcessor extends BlockProcessor {

    /** The role the statement's table carries. */
    static final String ROLE = "anf-statement";

    private static final Pattern FIELD = Pattern.compile("^([a-z_]+)::\\s*(.*)$");
    private static final Pattern TOKEN = Pattern.compile("^k:([^\\[\\s]*)\\[(.*?)]\\s*(?:@id=(\\S+))?\\s*(.*)$");
    private static final Pattern CANDIDATE = Pattern.compile("^candidate\\[(.*?)]\\s*(.*)$");
    private static final Pattern CLARIFY = Pattern.compile("^clarify\\[(.*?)]\\s*(.*)$");
    private static final Pattern INTERVAL = Pattern.compile("^([\\[(])\\s*(\\S*?)\\s*,\\s*(\\S*?)\\s*([\\])])\\s*(?:res=\\S+\\s*)?(.*)$");

    private static final Map<Document, Map<String, Statement>> BY_DOCUMENT = new WeakHashMap<>();

    /** One statement, as the build reads it: its type, its name, its fields in written order. */
    record Statement(String type, String name, List<Field> fields) {
        /** The first value of a field, when the statement has it. */
        Optional<String> first(String field) {
            return fields.stream().filter(f -> f.name().equals(field)).map(Field::shown).findFirst();
        }

        /** The first value of a field as the block wrote it, when the statement has it. */
        Optional<String> raw(String field) {
            return fields.stream().filter(f -> f.name().equals(field)).map(Field::raw).findFirst();
        }

        /** What the statement says in one line: the topic, then the result or request, timing, method and purpose. */
        String summary() {
            List<String> parts = new ArrayList<>();
            first("topic").ifPresent(parts::add);
            for (Field field : fields) {
                switch (field.name()) {
                    case "result", "requested_result", "timing", "method", "purpose", "body_site" ->
                            parts.add(words(field.name()) + " " + field.shown());
                    default -> {
                    }
                }
            }
            return String.join(", ", parts);
        }
    }

    /** One field: its name, its value as the block wrote it, and the value as the document shows it, AsciiDoc with chips. */
    record Field(String name, String raw, String shown) {
    }

    /** Creates a new block processor instance. */
    public AnfBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> lines = reader.readLines();
        String type = attribute(attributes, "type").orElse("performance").toLowerCase(Locale.ROOT);
        String name = attribute(attributes, "id").orElse(null);
        Statement statement = new Statement(type, name, fields(lines));
        if (name != null) {
            define(parent.getDocument(), statement);
        }
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>(), new HashMap<>(Map.of("role", ROLE)));
        parseContent(container, source(statement, attribute(attributes, "element"), attribute(attributes, "narrative")));
        return container;
    }

    /** The AsciiDoc that stands in the block's place. */
    static List<String> source(Statement statement, Optional<String> element, Optional<String> narrative) {
        List<String> source = new ArrayList<>();
        source.add("[cols=\"1,4\", role=\"" + ROLE + "\"]");
        source.add("|===");
        source.add("2+| *ANF " + statement.type() + " statement*" + (statement.name() == null ? "" : " `" + statement.name() + "`"));
        element.ifPresent(sentence -> source.add("| Element | " + cell(sentence)));
        narrative.ifPresent(words -> source.add("| Narrative | “" + cell(words) + "”"));
        if (statement.fields().isEmpty()) {
            source.add("2+| _Not yet lifted._");
        }
        for (Field field : statement.fields()) {
            source.add("| " + capitalised(words(field.name())) + " | " + field.shown());
        }
        source.add("|===");
        source.add("");
        return source;
    }

    /** The fields of the block's body, each value shown with chips. */
    static List<Field> fields(List<String> lines) {
        List<Field> fields = new ArrayList<>();
        for (String raw : lines) {
            Matcher m = FIELD.matcher(raw.strip());
            if (m.matches()) {
                fields.add(new Field(m.group(1), m.group(2).strip(), value(m.group(2).strip())));
            }
        }
        return fields;
    }

    /** A value as the document shows it: a concept, a candidate, a question, an interval with its unit, or text. */
    static String value(String text) {
        Matcher interval = INTERVAL.matcher(text);
        if (interval.matches()) {
            String bounds = interval.group(1) + bound(interval.group(2), "−∞") + ", " + bound(interval.group(3), "∞")
                    + interval.group(4);
            String unit = interval.group(5).strip();
            return unit.isEmpty() ? bounds : bounds + " " + value(unit);
        }
        Matcher candidate = CANDIDATE.matcher(text);
        if (candidate.matches()) {
            return "[.anf-candidate]_" + cell(candidate.group(1)) + "_ (candidate)";
        }
        Matcher clarify = CLARIFY.matcher(text);
        if (clarify.matches()) {
            return "to clarify: " + cell(clarify.group(2).replace("\"", "").strip());
        }
        Matcher token = TOKEN.matcher(text);
        if (token.matches()) {
            String label = token.group(2).isBlank() ? token.group(1) : token.group(2);
            String identifier = token.group(3);
            String rest = token.group(4).strip();
            String chip = ComplexConceptBlockProcessor.chip(new ComplexConceptSource.Ref(token.group(1), label, identifier));
            if (rest.startsWith("role=")) {
                rest = "as " + value(rest.substring("role=".length()));
            }
            return rest.isEmpty() ? chip : chip + " " + rest;
        }
        return cell(text.replaceAll("^\"|\"$", ""));
    }

    private static String bound(String bound, String unbounded) {
        String b = bound == null ? "" : bound.strip();
        return b.isEmpty() || b.endsWith("inf") ? unbounded : b;
    }

    /** A field name in words: {@code requested_result} is "requested result". */
    static String words(String field) {
        return field.replace('_', ' ');
    }

    private static String capitalised(String words) {
        return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String cell(String text) {
        return DecisionTreeFormat.cell(text);
    }

    private static Optional<String> attribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value == null || String.valueOf(value).isBlank() ? Optional.empty() : Optional.of(String.valueOf(value));
    }

    // ── What a statement states, as a model's parameter reads it ─────────────

    /**
     * The measure a statement states, as a graphlet model's parameter reads it: a performance whose
     * {@code result::} is an interval. The interval is the range someone determined; the scale after
     * it is what the number is on, a share of the subject group when its name says share, fraction,
     * proportion, probability or percent, which the tree reads as the share at a node, else the
     * statement's own concept, a currency say. The parameter is named by the topic and, for a share,
     * the group it is about, or else the scale in brackets, so two statements about one topic, the
     * cost of an inspection and its sensitivity, read apart.
     */
    static Optional<StatementMeasures.Stated> measure(Statement statement) {
        if (!"performance".equals(statement.type())) {
            return Optional.empty();
        }
        Optional<String> result = statement.raw("result");
        if (result.isEmpty()) {
            return Optional.empty();
        }
        Matcher m = INTERVAL.matcher(result.get());
        if (!m.matches()) {
            return Optional.empty();
        }
        Interval range = new Interval(number(m.group(2), Double.NEGATIVE_INFINITY), number(m.group(3), Double.POSITIVE_INFINITY),
                !"[".equals(m.group(1)), !"]".equals(m.group(4)));
        if (Double.isNaN(range.lower()) || Double.isNaN(range.upper())) {
            return Optional.empty();
        }
        Concept scale = concept(m.group(5).strip()).orElse(null);
        boolean share = scale == null || isShare(scale.shown());
        String topic = statement.raw("topic").flatMap(AnfBlockProcessor::concept).map(Concept::shown).orElse(statement.name());
        String label;
        if (share) {
            String group = statement.raw("subject_of_information").flatMap(AnfBlockProcessor::concept).map(Concept::shown).orElse("");
            label = group.isBlank() ? topic : topic + " among " + group;
        } else {
            label = topic + " (" + scale.shown() + ")";
        }
        return Optional.of(new StatementMeasures.Stated(range, share ? null : scale, label));
    }

    /** Whether a scale's name says the number is a share of its group: a share, a fraction, a proportion, a probability, a percentage. */
    static boolean isShare(String label) {
        if (label == null) {
            return false;
        }
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.contains("fraction") || lower.contains("share") || lower.contains("proportion")
                || lower.contains("probabilit") || lower.contains("percent");
    }

    /** The statements the document's {@code [anf]} blocks before this point state, as a model's parameters read them. */
    static StatementMeasures measures(Document document) {
        return name -> defined(document, name).flatMap(AnfBlockProcessor::measure);
    }

    /**
     * A value as a concept: a {@code k:} token by its key, label and identifier; a candidate by its
     * provisional label alone; nothing for a question or blank text.
     */
    static Optional<Concept> concept(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher token = TOKEN.matcher(text);
        if (token.matches()) {
            String label = token.group(2).isBlank() ? token.group(1) : token.group(2);
            return Optional.of(new Concept(token.group(1), label, token.group(3)));
        }
        Matcher candidate = CANDIDATE.matcher(text);
        if (candidate.matches()) {
            return Optional.of(new Concept(candidate.group(1), candidate.group(1), null));
        }
        if (CLARIFY.matcher(text).matches()) {
            return Optional.empty();
        }
        String plain = text.replaceAll("^\"|\"$", "").strip();
        return plain.isEmpty() ? Optional.empty() : Optional.of(new Concept(plain, plain, null));
    }

    private static double number(String bound, double unbounded) {
        String b = bound == null ? "" : bound.strip();
        if (b.isEmpty() || b.endsWith("inf")) {
            return unbounded;
        }
        try {
            return Double.parseDouble(b);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    // ── The document's statements ────────────────────────────────────────────

    private static synchronized void define(Document document, Statement statement) {
        BY_DOCUMENT.computeIfAbsent(document, d -> new LinkedHashMap<>()).put(statement.name(), statement);
    }

    /** The statement an {@code [anf]} block earlier in the document named. */
    static synchronized Optional<Statement> defined(Document document, String name) {
        if (document == null || name == null) {
            return Optional.empty();
        }
        Map<String, Statement> statements = BY_DOCUMENT.get(document);
        return statements == null ? Optional.empty() : Optional.ofNullable(statements.get(name));
    }
}
