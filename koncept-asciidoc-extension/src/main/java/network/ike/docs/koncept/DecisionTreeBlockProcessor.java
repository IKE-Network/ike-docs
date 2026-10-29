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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * The figures are self-contained SVG from the same geometry the live diagram lays out, with label
 * widths estimated rather than measured: inline on the html5 family, so the text stays text and a
 * wide tree scrolls; as a content-addressed file on Prawn, DocBook and Typst, where an image block
 * is what those converters place. A block with no root decision renders as a literal block, so the
 * text is visible, never dropped; a block with gaps renders with them named in a note.
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

    private static final Path FILE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "ike-koncept-figures");

    /** Creates a new block processor instance. */
    public DecisionTreeBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> lines = reader.readLines();
        Parsed parsed = DecisionTreeSource.parse(lines);
        if (parsed.root() == null) {
            LOG.debug("decision-tree: no root decision; rendering raw content as literal: {}", parsed.problems());
            return createBlock(parent, "literal", lines);
        }
        double price = price(attributes.get("health-price"));
        String symbol = attributes.get("symbol") == null ? "$" : String.valueOf(attributes.get("symbol"));
        String title = attributes.get("title") == null ? "The decision as a tree" : String.valueOf(attributes.get("title"));
        Node root = parsed.root();
        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, price);
        Optional<DecisionTreeSweep> sweep = rollback.value(root).isPresent()
                ? Optional.of(DecisionTreeSweep.of(root, price, DecisionTreeSweep.DEFAULT_STEPS))
                : Optional.empty();

        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        Document doc = parent.getDocument();
        container.append(figure(container, doc,
                DecisionFigureSvg.write(DecisionTreeFigures.tree(root, rollback, symbol, sweep)), title, "decision-tree"));
        parseContent(container, legend(rollback, symbol));
        sweep.filter(s -> !s.swings().isEmpty()).ifPresent(s -> {
            DecisionFigure tornado = DecisionFigureTornado.build(DecisionTreeFigures.tornado(s), TORNADO_WIDTH,
                    cost -> DecisionTreeFigures.money(cost, symbol));
            container.append(figure(container, doc, DecisionFigureSvg.write(tornado),
                    "How far each value, moved on its own across its range, swings the expected cost at the root",
                    "decision-tornado"));
        });
        List<String> source = new ArrayList<>();
        source.addAll(assumptions(root, rollback, symbol));
        source.addAll(table(rollback, symbol));
        List<String> problems = new ArrayList<>(parsed.problems());
        problems.addAll(rollback.findings());
        if (!problems.isEmpty()) {
            source.add("");
            source.add("NOTE: The tree has gaps: " + String.join("; ", problems) + ".");
        }
        parseContent(container, source);
        return container;
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

    /**
     * A captioned, numbered figure of an SVG: inline on the html5 family, in a block that scrolls
     * when the tree is wider than the column; an image block over a content-addressed file where
     * the converter places files (Prawn, DocBook, Typst).
     */
    private Block figure(StructuralNode parent, Document doc, String svg, String title, String role) {
        String backend = doc.getAttribute("backend", "html5").toString();
        boolean fileBackend = "pdf".equals(backend) || "docbook5".equals(backend) || "docbook".equals(backend)
                || "typst".equals(backend);
        String caption = doc.getAttribute("figure-caption", "Figure") + " " + doc.getAndIncrementCounter("figure-number") + ". ";
        if (fileBackend) {
            Map<String, Object> imageAttrs = new HashMap<>();
            imageAttrs.put("target", svgFile(role, svg));
            imageAttrs.put("alt", title);
            Block image = createBlock(parent, "image", "", imageAttrs);
            image.setTitle(title);
            image.setCaption(caption);
            image.addRole(role);
            return image;
        }
        String html = "<div class=\"imageblock " + role + "\">\n<div class=\"content\" style=\"overflow-x: auto\">\n"
                + svg + "</div>\n<div class=\"title\">" + DecisionTreeFormat.html(caption + title) + "</div>\n</div>";
        return createBlock(parent, "pass", html, new HashMap<>());
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
    static List<String> legend(DecisionTreeRollback rollback, String symbol) {
        return List.of("A square is a choice, a circle is a chance, and a triangle is where the consequences are counted."
                + " The figure after \u21D2 is the expected cost still ahead of a patient who reaches that point, health"
                + " counted at " + symbol + DecisionTreeFormat.money(rollback.healthPrice()) + " a year of healthy life;"
                + " a branch's own costs are beneath its run, and a probability is the share who leave a circle by that"
                + " branch. The heavy branch is the alternative the rollback chooses, and a barred one is rejected."
                + " A number in an orange callout is worked in the table beneath, under the same callout; a small"
                + " triangle marks a value whose range holds a flip of the choice at the root.", "");
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
