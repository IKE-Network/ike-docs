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

import network.ike.docs.koncept.DecisionTreeSource.Concept;
import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Measure;
import network.ike.docs.koncept.DecisionTreeSource.Node;
import network.ike.docs.koncept.DecisionTreeSource.Parsed;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code [graphlet-model]} block in the documentation build ({@code dev-decision-point-knowledge},
 * settled 2026-11-06): the model of a decision as one versioned property graph, the tree's structure
 * and its parameters together, in the decision tree's grammar ({@link DecisionTreeSource}) plus
 * parameters that read the document's ANF statements by name and hold the value the tree runs at.
 * The block keeps its text under its id for the renderings after it, {@code [decision-tree,
 * model=<id>]} and {@code [tornado, model=<id>]}, which hold no structure and no number, and puts in
 * its own place the model's parameters as a table: each at the value the tree runs at, with the
 * range its statement determined, and the statement or basis it comes from.
 *
 * <pre>
 * [graphlet-model, id=bimo-site-model]
 * ----
 * decision: k:SiteInspectionDecision[Inspect the site?] @id=bimo-site-decision
 *   parameter = [0.15,0.15] anf:est-material-problem
 *   parameter = anf:est-inspection-sensitivity
 *   alternative: k:Inspect[inspect] @id=bimo-inspect statement=anf:req-inspection
 *     money = from anf:est-inspection-cost
 *     …
 * ----
 * </pre>
 *
 * A model names itself with {@code id=}, or by its root's {@code @id} when it names none. Its tree
 * is known to the document by its root as a tree drawn in a block is, so a tree after it may fold it
 * in at an outcome, {@code tree=…}. A block with no root renders as a literal block, so the text is
 * visible, never dropped; a block with gaps renders with them named in a note. The output is
 * AsciiDoc re-parsed in place, so it is backend-agnostic.
 */
@Name("graphlet-model")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class GraphletModelBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GraphletModelBlockProcessor.class);

    /** The role the model's table carries. */
    static final String ROLE = "graphlet-model";

    /** Creates a new block processor instance. */
    public GraphletModelBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> lines = reader.readLines();
        Document doc = parent.getDocument();
        Parsed parsed = DecisionTreeSource.parse(lines, AnfBlockProcessor.measures(doc));
        if (parsed.root() == null) {
            LOG.debug("graphlet-model: no root; rendering raw content as literal: {}", parsed.problems());
            return createBlock(parent, "literal", lines);
        }
        Node root = parsed.root();
        Optional<String> id = modelId(attributes, root);
        id.ifPresent(name -> DocumentModels.of(doc).define(name, lines));
        // a share a calculate block earlier in the document computed becomes a parameter the model
        // states no line for (dev-calculator-blocks)
        DocumentCalculations.of(doc).inject(root);
        DocumentTrees trees = DocumentTrees.of(doc);
        String treeId = root.concept == null ? null : root.concept.identifier();
        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, DecisionTreeBlockProcessor.DEFAULT_HEALTH_PRICE,
                Map.of(), trees::tree);
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>(), new HashMap<>(Map.of("role", ROLE)));
        id.ifPresent(name -> container.setId(anchor(name)));
        trees.define(treeId, root, container);
        List<String> source = new ArrayList<>(parameters(root, rollback, attributes.get("title")));
        List<String> problems = new ArrayList<>(parsed.problems());
        problems.addAll(rollback.findings());
        if (!problems.isEmpty()) {
            source.add("NOTE: The model has gaps: " + String.join("; ", problems) + ".");
            source.add("");
        }
        parseContent(container, source);
        return container;
    }

    /** The anchor a model's block carries, for links to it. */
    static String anchor(String id) {
        return "graphlet-model-" + id.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    /** The id a model gives itself, {@code id=…}, or its root's identifier when it gives none. */
    static Optional<String> modelId(Map<String, Object> attributes, Node root) {
        Object own = attributes.get("id");
        if (own != null && !String.valueOf(own).isBlank()) {
            return Optional.of(String.valueOf(own));
        }
        return Optional.ofNullable(root.concept == null ? null : root.concept.identifier());
    }

    /**
     * The model's parameters as a table: each at the value the tree runs at, with the range its
     * statement determined, or the range a sweep assumes, and where the value comes from. Nothing
     * when the model states no parameter.
     */
    static List<String> parameters(Node root, DecisionTreeRollback rollback, Object title) {
        List<String> source = new ArrayList<>();
        List<Measure> parameters = root.parameters();
        if (parameters.isEmpty()) {
            source.add("The model states no parameter: every value is on the tree itself.");
            source.add("");
            return source;
        }
        source.add("." + (title == null ? "The model’s parameters, each at the value the tree runs at, with the statement’s range"
                : String.valueOf(title)));
        source.add("[cols=\"4,2,2,3\", options=\"header\", role=\"" + ROLE + "\"]");
        source.add("|===");
        source.add("| Parameter | Value the tree runs at | Range | From");
        for (Measure parameter : parameters) {
            source.add("| " + DecisionTreeFormat.cell(parameter.name()) + " | " + DecisionTreeFormat.cell(value(parameter, rollback))
                    + " | " + DecisionTreeFormat.cell(range(parameter)) + " | " + DecisionTreeFormat.cell(from(parameter)));
        }
        source.add("|===");
        source.add("");
        return source;
    }

    private static String value(Measure parameter, DecisionTreeRollback rollback) {
        Optional<Interval> value = rollback.measure(parameter);
        if (value.isEmpty() || Double.isNaN(value.get().lower())) {
            return parameter.readsAStatement() && parameter.range() == null ? "waits for the statement" : "not determinable";
        }
        return scaled(value.get(), parameter.semantic());
    }

    private static String range(Measure parameter) {
        if (parameter.readsAStatement()) {
            Interval range = parameter.range();
            if (range == null) {
                return "not stated";
            }
            if (range.isPoint()) {
                return "a point, swept half to double";
            }
            return scaled(Interval.point(range.lower()), parameter.semantic()) + " to "
                    + scaled(Interval.point(range.upper()), parameter.semantic());
        }
        if (!parameter.stated()) {
            return "derived";
        }
        if (parameter.bounds().isPoint()) {
            return "assumed, half to double";
        }
        return scaled(parameter.bounds(), parameter.semantic());
    }

    private static String from(Measure parameter) {
        List<String> parts = new ArrayList<>();
        if (parameter.readsAStatement()) {
            parts.add("statement `" + parameter.statement() + "`");
        } else if (parameter.derived()) {
            StringBuilder how = new StringBuilder(parameter.derivation() == null
                    ? parameter.derivationConcept().shown() : parameter.derivation().label);
            if (!parameter.operands().isEmpty()) {
                how.append(" of ");
                List<String> names = new ArrayList<>();
                for (Concept operand : parameter.operands()) {
                    names.add(operand.shown());
                }
                how.append(String.join(", ", names));
            }
            parts.add(how.toString());
        }
        if (parameter.basis() != null && !(parameter.derived() && "derived".equals(parameter.basis()))) {
            parts.add(parameter.basis());
        }
        if (parameter.source() != null) {
            parts.add(parameter.source());
        }
        return String.join(", ", parts);
    }

    /** A value on its scale: a share as a percentage, anything else as a number with the scale's name. */
    private static String scaled(Interval value, Concept scale) {
        if (scale == null) {
            return DecisionTreeFormat.percent(value);
        }
        return DecisionTreeFormat.plain(value) + " " + scale.shown();
    }
}
