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
import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Kind;
import network.ike.docs.koncept.DecisionTreeSource.Node;
import network.ike.docs.koncept.DecisionTreeSource.Parsed;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@code [decision-tree]} block in the documentation build ({@code dev-decision-tree-blocks}):
 * the static projection of the block the assistant surface renders live. It reads the block
 * store-free ({@link DecisionTreeSource}), rolls it back at a health price
 * ({@link DecisionTreeRollback}), and re-parses two things in the block's place: the Graphviz
 * figure the regulatory decision analysis topics hand-write, a square where someone chooses, a
 * circle where the world decides, a leaf where the consequences are counted, every number badged
 * with a callout; and the worked table beneath it, where each callout's number is computed from
 * the inputs, generated rather than maintained.
 *
 * <pre>
 * [decision-tree, health-price=100000, symbol="$", title="Should this patient have a scan in year 2?"]
 * ----
 * decision: k:ScanNow[Scan now?] @id=…
 *   …
 * ----
 * </pre>
 *
 * The figure's {@code graphviz} block renders through asciidoctor-diagram where the build has it,
 * as the topics' own figures do, and stands as its source listing where it does not. A block
 * with no root decision renders as a literal block, so the text is visible, never dropped; a
 * block with gaps renders with them named in a note.
 */
@Name("decision-tree")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class DecisionTreeBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionTreeBlockProcessor.class);

    /** The default price of a year of healthy life: the regulatory topics' central value. */
    static final double DEFAULT_HEALTH_PRICE = 100_000;

    private static final String BADGE = "<TD BGCOLOR=\"#eb6834\" STYLE=\"rounded\"><FONT COLOR=\"white\" POINT-SIZE=\"9\"><B>%d</B></FONT></TD>";

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
        String title = attributes.get("title") == null ? null : String.valueOf(attributes.get("title"));
        DecisionTreeRollback rollback = DecisionTreeRollback.of(parsed.root(), price);

        List<String> source = new ArrayList<>();
        source.addAll(figure(parsed.root(), rollback, symbol, title));
        source.add("");
        source.addAll(assumptions(parsed.root(), rollback, symbol));
        source.addAll(table(rollback, symbol));
        List<String> problems = new ArrayList<>(parsed.problems());
        problems.addAll(rollback.findings());
        if (!problems.isEmpty()) {
            source.add("");
            source.add("NOTE: The tree has gaps: " + String.join("; ", problems) + ".");
        }
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
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

    // ── The figure ───────────────────────────────────────────────────────────

    /** The Graphviz block: nodes for decisions, chances and outcomes, branches as edges. */
    static List<String> figure(Node root, DecisionTreeRollback rollback, String symbol, String title) {
        Map<Node, String> ids = new IdentityHashMap<>();
        List<String> body = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        StringBuilder legend = new StringBuilder();
        legend.append("† the cost of what is done on that branch  ")
                .append("‡ the expected cost the leaf is left with  ")
                .append("§ the expected cost still ahead  ")
                .append("% the share who leave a circle by that branch; ")
                .append("health counted at ").append(symbol).append(DecisionTreeFormat.money(rollback.healthPrice()))
                .append(" a year of healthy life; a number in an orange badge is worked in the table");
        emit(root, rollback, symbol, ids, body, edges);

        List<String> source = new ArrayList<>();
        if (title != null && !title.isBlank()) {
            source.add("." + title);
        }
        source.add("[graphviz]");
        source.add("----");
        source.add("digraph decision_tree {");
        source.add("  rankdir=TB; nodesep=0.18; ranksep=0.4;");
        source.add("  node [fontname=\"Helvetica\", fontsize=11, color=\"#52514e\"];");
        source.add("  edge [fontname=\"Helvetica\", fontsize=10, color=\"#52514e\"];");
        source.add("  label=\"" + legend.toString().replace("\"", "\\\"") + "\"; labelloc=b; labeljust=l;"
                + " fontname=\"Helvetica\"; fontsize=10; fontcolor=\"#52514e\";");
        source.addAll(body);
        source.addAll(edges);
        source.add("}");
        source.add("----");
        return source;
    }

    private static String id(Node node, Map<Node, String> ids) {
        return ids.computeIfAbsent(node, n -> "n" + ids.size());
    }

    private static void emit(Node node, DecisionTreeRollback rollback, String symbol, Map<Node, String> ids,
                             List<String> body, List<String> edges) {
        String id = id(node, ids);
        switch (node.kind) {
            case DECISION -> {
                StringBuilder rows = new StringBuilder();
                for (Node alternative : node.children) {
                    if (alternative.kind != Kind.ALTERNATIVE) {
                        continue;
                    }
                    if (!rows.isEmpty()) {
                        rows.append("<BR/>");
                    }
                    rows.append("if ").append(DecisionTreeFormat.html(alternative.label())).append(": ")
                            .append(value(rollback.value(alternative), symbol)).append("§");
                    if (rollback.chosen(node).filter(c -> c == alternative).isPresent()) {
                        rows.append(" ✓");
                    }
                }
                body.add("  " + id + " [shape=box, style=filled, fillcolor=\"#e8f0fb\", label=<"
                        + table(rollback.step(node), node.label(), rows.toString()) + ">];");
                for (Node alternative : node.children) {
                    if (alternative.kind == Kind.ALTERNATIVE) {
                        branch(node, alternative, rollback, symbol, ids, body, edges);
                    }
                }
            }
            case CHANCE -> {
                body.add("  " + id + " [shape=circle, fixedsize=true, width=1.7, margin=0.02, label=<"
                        + table(rollback.step(node), node.label(), "from here:<BR/>" + value(rollback.value(node), symbol) + "§")
                        + ">];");
                for (Node event : node.children) {
                    if (event.kind == Kind.EVENT) {
                        branch(node, event, rollback, symbol, ids, body, edges);
                    }
                }
            }
            case OUTCOME -> body.add("  " + id + " [shape=plaintext, label=<"
                    + table(rollback.step(node), node.label(), value(rollback.value(node), symbol) + "‡") + ">];");
            case ALTERNATIVE, EVENT -> throw new IllegalStateException("a branch is an edge, not a node");
        }
    }

    /** A branch as an edge from its node to what it leads to, labelled with its name, share and cost. */
    private static void branch(Node from, Node branch, DecisionTreeRollback rollback, String symbol, Map<Node, String> ids,
                               List<String> body, List<String> edges) {
        Optional<Node> next = branch.next();
        String target;
        if (next.isPresent()) {
            emit(next.get(), rollback, symbol, ids, body, edges);
            target = id(next.get(), ids);
        } else {
            target = id(branch, ids);
            body.add("  " + target + " [shape=plaintext, label=<" + table(Optional.empty(), "",
                    value(rollback.value(branch), symbol) + "‡") + ">];");
        }
        StringBuilder first = new StringBuilder(DecisionTreeFormat.html(branch.label()));
        if (branch.kind == Kind.EVENT) {
            rollback.share(branch).ifPresent(share -> first.append(' ').append(DecisionTreeFormat.percent(share)));
        } else {
            first.append(':');
        }
        String second = branch.costs().isEmpty() ? ""
                : value(rollback.branchCost(branch), symbol) + "†";
        edges.add("  " + id(from, ids) + " -> " + target + " [label=<" + table(rollback.step(branch), first.toString(), second) + ">];");
    }

    /** A two-row HTML-like label: badge and heading, then the detail. */
    private static String table(Optional<Step> step, String heading, String detail) {
        StringBuilder sb = new StringBuilder("<TABLE BORDER=\"0\" CELLBORDER=\"0\" CELLPADDING=\"1\"><TR>");
        sb.append(step.map(s -> String.format(BADGE, s.number())).orElse("<TD></TD>"));
        sb.append("<TD ALIGN=\"LEFT\">").append(heading).append("</TD></TR>");
        if (detail != null && !detail.isEmpty()) {
            sb.append("<TR><TD></TD><TD ALIGN=\"LEFT\" BALIGN=\"LEFT\">").append(detail).append("</TD></TR>");
        }
        return sb.append("</TABLE>").toString();
    }

    private static String value(Optional<Interval> value, String symbol) {
        return value.map(v -> DecisionTreeFormat.html(DecisionTreeFormat.money(v, symbol))).orElse("?");
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
                    ? DecisionTreeFormat.cell(DecisionTreeFormat.plain(step.result()))
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
