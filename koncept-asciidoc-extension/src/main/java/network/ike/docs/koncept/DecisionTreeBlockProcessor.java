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

import network.ike.docs.koncept.DecisionTreeRollback.Step;
import network.ike.docs.koncept.DecisionTreeSource.Concept;
import network.ike.docs.koncept.DecisionTreeSource.Node;
import network.ike.docs.koncept.DecisionTreeSource.Parsed;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * The {@code [decision-tree]} block in the documentation build ({@code dev-decision-tree-blocks}):
 * the static projection of the block the assistant surface renders live. It reads the block
 * store-free ({@link DecisionTreeSource}), rolls it back at a health price
 * ({@link DecisionTreeRollback}), sweeps every stated value one way ({@link DecisionTreeSweep}),
 * and puts in the block's place what Komet draws: the tree as decision analysts draw it, left to
 * right, a square where someone chooses, a circle where the world decides, a triangle where the
 * consequences are counted, the rolled-back cost at every glyph, the chosen alternative heavy
 * ({@link DecisionTreeFigures}); beneath it the tornado of the sweep, one bar per value; then the
 * worked table, where each orange callout's number is computed from the inputs, generated rather
 * than maintained.
 *
 * <pre>
 * [decision-tree, health-price=100000, symbol="$", title="Should this patient have a scan in year 2?"]
 * ----
 * decision: k:ScanNow[Scan now?] @id=…
 *   …
 * ----
 * </pre>
 *
 * The figures are drawn from the same geometry the live diagram lays out, with label widths
 * estimated rather than measured: as self-contained SVG, inline on the html5 family, so the text
 * stays text and a wide tree scrolls, and as a content-addressed file on Prawn and DocBook, where
 * an image block is what those converters place; on the Typst backend as native Typst drawing code
 * ({@link DecisionFigureTypst}), handed to the converter in a passthrough block with the role
 * {@code typst}, so the print pipeline draws the figure with the document's own text and never
 * through an embedded image. A block with no root decision renders as a literal block, so the text
 * is visible, never dropped; a block with gaps renders with them named in a note.
 *
 * <p>A block that names a model, {@code [decision-tree, model=<id>]}, is a rendering of the
 * {@code [graphlet-model]} block before it ({@code dev-decision-point-knowledge}, settled 2026-11-06):
 * it holds no structure and no number, only how the model is viewed, the standpoint it is rolled
 * back from and the cohort it is counted out for. It draws the model's tree, ledger and sentences
 * and the worked table, and no tornado: that is the {@code [tornado, model=<id>]} block's; and no
 * table of assumptions: the model block draws its own parameters. A block with a body of its own
 * still reads as before, tornado beneath.
 */
@Name("decision-tree")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class DecisionTreeBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionTreeBlockProcessor.class);

    /** The default price of a year of healthy life: the regulatory topics' central value. */
    static final double DEFAULT_HEALTH_PRICE = 100_000;

    /** The tornado's width, in figure units: the html5 family's text column. */
    static final double TORNADO_WIDTH = 640;

    /**
     * The tornado's width on the Typst backend, in points: a little under the text column of the
     * converter's A4 page setup (210 mm less 24 mm inside and 18 mm outside), so the bars take the
     * column and the drawing is never shrunk to fit it.
     */
    static final double TYPST_TORNADO_WIDTH = 470;

    /** The role a passthrough block carries when its content is Typst source for the converter. */
    static final String ROLE_TYPST = "typst";

    /** The role a Typst passthrough block carries when its content is a drawing to fit to the column. */
    static final String ROLE_DRAWING = "drawing";

    private static final Path FILE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "ike-koncept-figures");

    /** Creates a new block processor instance. */
    public DecisionTreeBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> lines = reader.readLines();
        Document doc = parent.getDocument();
        // a rendering of a model: the model's text stands in for the body, which is empty
        Optional<String> model = modelReference(attributes);
        boolean rendering = model.isPresent();
        if (rendering) {
            Optional<List<String>> text = DocumentModels.of(doc).model(model.get());
            if (text.isEmpty()) {
                LOG.warn("decision-tree: no graphlet-model block before it is named {}", model.get());
                StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
                parseContent(container, List.of("NOTE: No graphlet-model block earlier in this document is named `" + model.get()
                        + "`, so there is no model to draw.", ""));
                return container;
            }
            lines = text.get();
        }
        Parsed parsed = DecisionTreeSource.parse(lines, AnfBlockProcessor.measures(doc));
        if (parsed.root() == null) {
            LOG.debug("decision-tree: no root decision; rendering raw content as literal: {}", parsed.problems());
            return createBlock(parent, "literal", lines);
        }
        double price = price(attributes.get("health-price"));
        OptionalDouble cohort = cohort(attributes.get("cohort"));
        String symbol = attributes.get("symbol") == null ? "$" : String.valueOf(attributes.get("symbol"));
        String title = attributes.get("title") == null ? "The decision as a tree" : String.valueOf(attributes.get("title"));
        Node root = parsed.root();
        // a share a calculate block earlier in the document computed becomes a parameter the tree
        // states no line for (dev-calculator-blocks); the text never carries the number
        DocumentCalculations.of(doc).inject(root);
        // known to the document by its root, so a tree after it may fold it in at an outcome, tree=…
        String treeId = root.concept == null ? null : root.concept.identifier();
        DocumentTrees trees = DocumentTrees.of(doc);
        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, price, Map.of(), trees::tree);
        Optional<DecisionTreeSweep> sweep = rollback.value(root).isPresent()
                ? Optional.of(DecisionTreeSweep.of(root, price, DecisionTreeSweep.DEFAULT_STEPS, trees::tree))
                : Optional.empty();

        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        if (treeId != null) {
            container.setId(DocumentTrees.anchor(treeId));
        }
        trees.define(treeId, root, container);
        boolean typst = isTypst(doc);
        container.append(figure(container, doc, DecisionTreeFigures.tree(root, rollback, symbol, sweep, cohort,
                node -> !node.statements.isEmpty() || situation(doc, node).isPresent()), title));
        parseContent(container, legend(rollback, symbol, anyHealth(root)));
        List<String> foldIns = foldIns(this, rollback, trees, treeId);
        if (!foldIns.isEmpty()) {
            parseContent(container, foldIns);
        }
        decisionPoint(doc, root).ifPresent(sentence -> parseContent(container, List.of(sentence, "")));
        List<String> statementProblems = new ArrayList<>();
        List<String> statements = statements(doc, root, statementProblems);
        if (!statements.isEmpty()) {
            parseContent(container, statements);
        }
        // a rendering draws no tornado: the tornado block does, from the same model
        sweep.filter(s -> !rendering && !s.swings().isEmpty()).ifPresent(s -> {
            DecisionFigure tornado = DecisionFigureTornado.build(DecisionTreeFigures.tornado(s, symbol),
                    typst ? TYPST_TORNADO_WIDTH : TORNADO_WIDTH, cost -> DecisionTreeFigures.money(cost, symbol));
            container.append(figure(container, doc, tornado, s.netValued()
                    ? "How far each value, moved on its own across its range, swings the net value of the chosen alternative"
                    : "How far each value, moved on its own across its range, swings the expected cost at the root"));
        });
        List<String> source = new ArrayList<>();
        if (!rendering) {
            // a model's parameters are tabled by the model block, each with its statement's range
            source.addAll(assumptions(root, rollback, symbol));
        }
        source.addAll(table(rollback, symbol));
        List<String> problems = new ArrayList<>(parsed.problems());
        problems.addAll(rollback.findings());
        problems.addAll(statementProblems);
        if (!problems.isEmpty()) {
            source.add("");
            source.add("NOTE: The tree has gaps: " + String.join("; ", problems) + ".");
        }
        parseContent(container, source);
        return container;
    }

    /** The model a rendering names, {@code model=<id>}, when it names one. */
    static Optional<String> modelReference(Map<String, Object> attributes) {
        Object value = attributes.get("model");
        return value == null || String.valueOf(value).isBlank() ? Optional.empty() : Optional.of(String.valueOf(value).strip());
    }

    /** The cohort a block asks to be counted out for, {@code cohort=1000}; empty when it states none or a bad one. */
    static OptionalDouble cohort(Object attribute) {
        if (attribute == null) {
            return OptionalDouble.empty();
        }
        try {
            double size = Double.parseDouble(String.valueOf(attribute).replace(",", "").strip());
            return size > 0 ? OptionalDouble.of(size) : OptionalDouble.empty();
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    static double price(Object attribute) {
        if (attribute == null) {
            return DEFAULT_HEALTH_PRICE;
        }
        try {
            return Double.parseDouble(String.valueOf(attribute).replace(",", "").strip());
        } catch (NumberFormatException e) {
            return DEFAULT_HEALTH_PRICE;
        }
    }

    // ── The figures ──────────────────────────────────────────────────────────

    static boolean isTypst(Document doc) {
        return "typst".equals(doc.getAttribute("backend", "html5").toString());
    }

    /**
     * A captioned, numbered figure: the SVG inline on the html5 family, in a block that scrolls
     * when the tree is wider than the column; an image block over a content-addressed SVG file
     * where the converter places files (Prawn, DocBook); on Typst the figure's own drawing code
     * in a passthrough block with the roles {@code typst} and {@code drawing}, which the converter
     * places verbatim, captioned, and fitted to the column.
     */
    private Block figure(StructuralNode parent, Document doc, DecisionFigure figure, String title) {
        return figure(this, parent, doc, figure, title);
    }

    /** A figure for any block processor of this family: a pass block on typst, an image file on the file backends, inline SVG otherwise. */
    static Block figure(BlockProcessor processor, StructuralNode parent, Document doc, DecisionFigure figure, String title) {
        String backend = doc.getAttribute("backend", "html5").toString();
        String role = figure.role();
        boolean fileBackend = "pdf".equals(backend) || "docbook5".equals(backend) || "docbook".equals(backend);
        String caption = doc.getAttribute("figure-caption", "Figure") + " " + doc.getAndIncrementCounter("figure-number") + ". ";
        if (isTypst(doc)) {
            Block pass = processor.createBlock(parent, "pass", DecisionFigureTypst.write(figure), new HashMap<>());
            pass.setTitle(title);
            pass.setCaption(caption);
            pass.addRole(ROLE_TYPST);
            pass.addRole(ROLE_DRAWING);
            pass.addRole(role);
            return pass;
        }
        String svg = DecisionFigureSvg.write(figure);
        if (fileBackend) {
            Map<String, Object> imageAttrs = new HashMap<>();
            imageAttrs.put("target", svgFile(role, svg));
            imageAttrs.put("alt", title);
            Block image = processor.createBlock(parent, "image", "", imageAttrs);
            image.setTitle(title);
            image.setCaption(caption);
            image.addRole(role);
            return image;
        }
        String html = "<div class=\"imageblock " + role + "\">\n<div class=\"content\" style=\"overflow-x: auto\">\n"
                + svg + "</div>\n<div class=\"title\">" + DecisionTreeFormat.html(caption + title) + "</div>\n</div>";
        return processor.createBlock(parent, "pass", html, new HashMap<>());
    }

    /** The SVG written to a content-addressed file, so identical figures share one and re-runs are stable. */
    static String svgFile(String role, String svg) {
        try {
            Files.createDirectories(FILE_DIR);
            Path file = FILE_DIR.resolve(role + "-" + shortHash(svg) + ".svg");
            if (!Files.exists(file)) {
                Files.writeString(file, svg, StandardCharsets.UTF_8);
            }
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write the " + role + " figure", e);
        }
    }

    private static String shortHash(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", h[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** How to read the tree: one paragraph after it, in the topics' words. */
    static List<String> legend(DecisionTreeRollback rollback, String symbol, boolean anyHealth) {
        // a tree with no health anywhere says nothing about health
        String health = anyHealth ? ", health counted at " + symbol + DecisionTreeFormat.money(rollback.healthPrice()) + " a year of healthy life" : "";
        return List.of("A square is a choice, a circle is a chance, and a triangle is where the consequences are counted."
                + " The figure after \u21D2 is the expected cost still ahead of a patient who reaches that point" + health + ";"
                + " a branch's own costs are beneath its run, and a percentage is the share who leave a circle by that"
                + " branch. A number in an orange callout is worked in the table beneath, under the same callout.", "");
    }

    /**
     * The sentence that says which decision point the tree decides, when its root decision names a
     * complex concept a {@code [complex-concept]} block earlier in the document stated: the tree is
     * for the cases the concept's prerequisites describe, and its numbers are read for those.
     */
    static Optional<String> decisionPoint(Document doc, Node root) {
        Concept concept = root.concept;
        if (concept == null) {
            return Optional.empty();
        }
        return ComplexConceptBlockProcessor.defined(doc, concept.identifier())
                .map(spec -> {
                    String what = ComplexConceptSource.describe(spec);
                    return "The decision point is the complex concept " + ComplexConceptBlockProcessor.chip(spec.concept())
                            + " stated above" + (what.isEmpty() ? "" : ", " + DecisionTreeFormat.cell(what))
                            + ". The tree is for those cases, and its numbers are read for them.";
                });
    }

    /**
     * The table of the ANF statements the tree's nodes hold by naming the document's {@code [anf]}
     * blocks, {@code statement=anf:req-scan}: each node, the statement it holds, and what the
     * statement says. An alternative holds a request, an event or outcome a performance; a statement
     * of another type, on a decision or chance, or named by no block before the tree is a problem.
     */
    static List<String> statements(Document doc, Node root, List<String> problems) {
        List<String> rows = new ArrayList<>();
        collect(doc, root, rows, problems);
        if (rows.isEmpty()) {
            return rows;
        }
        List<String> source = new ArrayList<>();
        source.add(".The ANF statements the tree's nodes hold: the situation the decision is made in, a request for each course asked for, the situation each event reveals and each outcome ends in");
        source.add("[cols=\"2,3,6\", options=\"header\", role=\"" + AnfBlockProcessor.ROLE + "\"]");
        source.add("|===");
        source.add("| Node | Statement | What it states");
        source.addAll(rows);
        source.add("|===");
        source.add("");
        return source;
    }

    /** The complex concept that states the situation a decision is made in, when the decision's concept is one. */
    static Optional<ComplexConceptSource.Spec> situation(Document doc, Node node) {
        if (node.kind != DecisionTreeSource.Kind.DECISION || node.concept == null) {
            return Optional.empty();
        }
        return ComplexConceptBlockProcessor.defined(doc, node.concept.identifier());
    }

    private static void collect(Document doc, Node node, List<String> rows, List<String> problems) {
        situation(doc, node).ifPresent(spec -> rows.add("| " + DecisionTreeFormat.cell(node.label()) + " (decision) | situation, "
                + "the prerequisites of " + ComplexConceptBlockProcessor.chip(spec.concept()) + " | "
                + DecisionTreeFormat.cell(ComplexConceptSource.describe(spec))));
        for (String reference : node.statements) {
            if (!reference.startsWith("anf:")) {
                continue;
            }
            String name = reference.substring("anf:".length());
            String kind = node.kind.name().toLowerCase(java.util.Locale.ROOT);
            String what = (node.residual ? "residual" : kind) + " \u201C" + node.label() + "\u201D";
            String expected = switch (node.kind) {
                case ALTERNATIVE -> "request";
                case DECISION, EVENT, OUTCOME -> "performance";
                default -> null;
            };
            if (expected == null) {
                problems.add("the " + what + " holds anf:" + name + ", but a chance holds no statement; the events beneath it do");
                continue;
            }
            Optional<AnfBlockProcessor.Statement> statement = AnfBlockProcessor.defined(doc, name);
            if (statement.isEmpty()) {
                problems.add("the " + what + " holds anf:" + name + ", which no anf block before the tree names");
                continue;
            }
            if (!statement.get().type().equals(expected)) {
                problems.add("the " + what + " holds anf:" + name + ", a " + statement.get().type()
                        + " statement; an alternative holds a request, a decision, an event or an outcome a performance");
            }
            rows.add("| " + DecisionTreeFormat.cell(node.label()) + " (" + (node.residual ? "residual" : kind) + ") | "
                    + statement.get().type() + " `" + name + "` | " + statement.get().summary());
        }
        for (Node child : node.children) {
            collect(doc, child, rows, problems);
        }
    }

    /**
     * The sentences that link the outcomes folding in another tree to it, one per tree folded in,
     * and the note on that tree that this one folds it in, which the tree processor turns into a
     * link back once the whole document is read.
     */
    static List<String> foldIns(BlockProcessor processor, DecisionTreeRollback rollback, DocumentTrees trees, String treeId) {
        Map<Node, List<Node>> byTree = new LinkedHashMap<>();
        rollback.foldedAll().forEach((outcome, inner) -> byTree.computeIfAbsent(inner.root(), r -> new ArrayList<>()).add(outcome));
        List<String> source = new ArrayList<>();
        byTree.forEach((folded, outcomes) -> {
            String foldedId = folded.concept == null ? null : folded.concept.identifier();
            List<String> names = new ArrayList<>();
            for (Node outcome : outcomes) {
                String name = "\u201C" + DecisionTreeFormat.cell(outcome.label()) + "\u201D";
                if (!names.contains(name)) {
                    names.add(name);
                }
            }
            String link = foldedId == null ? "\u201C" + folded.label() + "\u201D"
                    : "<<" + DocumentTrees.anchor(foldedId) + "," + DecisionTreeFormat.cell(folded.label()) + ">>";
            String which = outcomes.size() == 1 ? "The outcome " + names.get(0) + " folds"
                    : names.size() == 1 ? "Each outcome " + names.get(0) + ", " + outcomes.size() + " in all, folds"
                    : "The outcomes " + String.join(" and ", names) + ", " + outcomes.size() + " in all, each fold";
            source.add(which + " in " + link + ": its expected cost from its root is added there, and its counts"
                    + " and endpoints join the ledger and the tally.");
            source.add("");
            // the folded tree is drawn already: the link back is appended to its block
            if (trees.foldedBy(foldedId, treeId) && treeId != null) {
                String name = trees.tree(treeId).map(Node::label).orElse(treeId);
                trees.block(foldedId).ifPresent(block -> processor.parseContent(block, List.of("",
                        "Folded in by <<" + DocumentTrees.anchor(treeId) + "," + DecisionTreeFormat.cell(name)
                                + ">>, at the outcomes that end in it.")));
            }
        });
        return source;
    }

    /** Whether any node of the tree carries a health cost. */
    static boolean anyHealth(Node node) {
        for (DecisionTreeSource.Measure measure : node.costs()) {
            if (measure.kind() == DecisionTreeSource.MeasureKind.HEALTH) {
                return true;
            }
        }
        for (Node child : node.children) {
            if (anyHealth(child)) {
                return true;
            }
        }
        return false;
    }

    // ── The assumptions ──────────────────────────────────────────────────────

    /** The stated parameters as the topics' table of model assumptions; nothing when the block states none. */
    static List<String> assumptions(Node root, DecisionTreeRollback rollback, String symbol) {
        List<String> source = new ArrayList<>();
        List<DecisionTreeSource.Measure> stated = root.parameters().stream().filter(DecisionTreeSource.Measure::stated).toList();
        if (stated.isEmpty()) {
            return source;
        }
        source.add(".Model assumptions and default values");
        source.add("[cols=\"3,1,2\", options=\"header\"]");
        source.add("|===");
        source.add("| Quantity | Value | Basis");
        for (DecisionTreeSource.Measure parameter : stated) {
            String value = parameter.semantic() == null
                    ? DecisionTreeFormat.plain(parameter.bounds())
                    : DecisionTreeFormat.plain(parameter.bounds()) + " " + parameter.semantic().shown();
            String basis = parameter.basis() == null ? "" : parameter.basis()
                    + (parameter.source() == null ? "" : ", " + parameter.source());
            source.add("| " + DecisionTreeFormat.cell(parameter.name()) + " | " + DecisionTreeFormat.cell(value)
                    + " | " + DecisionTreeFormat.cell(basis));
        }
        source.add("|===");
        source.add("");
        return source;
    }

    // ── The worked table ─────────────────────────────────────────────────────

    /** The table where each badged number is worked from the inputs, in the order of the computation. */
    static List<String> table(DecisionTreeRollback rollback, String symbol) {
        List<String> source = new ArrayList<>();
        source.add(".Where each number on the tree comes from");
        source.add("[cols=\"1,4,9,4\", options=\"header\"]");
        source.add("|===");
        source.add("| No. | What it is | How it is computed | Result");
        for (Step step : rollback.steps()) {
            String result = step.node() == null
                    ? DecisionTreeFormat.cell(step.share() ? DecisionTreeFormat.percent(step.result())
                            : DecisionTreeFormat.plain(step.result()))
                    : DecisionTreeFormat.cell(DecisionTreeFormat.money(step.result(), symbol)) + " " + unit(step);
            String what = step.node() == null ? DecisionTreeFormat.cell(step.what()) : step.mark() + " " + chip(step.node());
            source.add("| [.figure-callout]#" + step.number() + "# | " + what
                    + " | " + DecisionTreeFormat.cell(step.how()) + " | " + result);
        }
        source.add("|===");
        return source;
    }

    private static String unit(Step step) {
        return switch (step.mark()) {
            case "†" -> "for each who takes the branch";
            case "‡" -> "on average for each who reaches the leaf";
            default -> "on average for each who reaches this point";
        };
    }

    /** The node's concept as a {@code k:} chip when its identifier is a UUID, else its label. */
    private static String chip(Node node) {
        Concept concept = node.concept;
        if (concept != null && concept.identifier() != null && isUuids(concept.identifier())) {
            return "k:uuid=" + concept.identifier() + "[" + concept.shown().replace("]", "\\]") + "]";
        }
        return DecisionTreeFormat.cell(node.label());
    }

    private static boolean isUuids(String identifier) {
        for (String part : identifier.split(",")) {
            try {
                UUID.fromString(part.strip());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
}
