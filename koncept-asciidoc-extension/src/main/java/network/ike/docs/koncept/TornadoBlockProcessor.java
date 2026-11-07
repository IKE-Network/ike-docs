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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code [tornado]} block in the documentation build ({@code dev-decision-point-knowledge},
 * settled 2026-11-06): a rendering of a graphlet model that holds no structure and no number, only
 * how the model is viewed. It reads the model the block names, {@code model=<id>}, from the
 * {@code [graphlet-model]} block before it, rolls the tree back at a health price
 * ({@link DecisionTreeRollback}), sweeps every stated value one way ({@link DecisionTreeSweep}),
 * and puts in the block's place the tornado of the sweep, one bar per value, as the decision-tree
 * block drew it beneath the tree before the model had a block of its own.
 *
 * <pre>
 * [tornado, model=bimo-site-model, health-price=100000, title="How far each value swings the net value"]
 * ----
 * ----
 * </pre>
 *
 * A block that names no model reads its own body as a tree, so a tornado can be drawn for a tree
 * written in place. The figure is drawn as the decision-tree block draws its figures: inline SVG on
 * the html5 family, a content-addressed file where the converter places files, native drawing code
 * on the Typst backend. A model the document does not have, or a tree that is not determinable, is
 * named in a note, never drawn blank.
 */
@Name("tornado")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class TornadoBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TornadoBlockProcessor.class);

    /** Creates a new block processor instance. */
    public TornadoBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> body = reader.readLines();
        Document doc = parent.getDocument();
        Optional<String> model = DecisionTreeBlockProcessor.modelReference(attributes);
        List<String> lines = body;
        if (model.isPresent()) {
            Optional<List<String>> text = DocumentModels.of(doc).model(model.get());
            if (text.isEmpty()) {
                LOG.warn("tornado: no graphlet-model block before it is named {}", model.get());
                return note(parent, "No graphlet-model block earlier in this document is named `" + model.get()
                        + "`, so there is no model to sweep.");
            }
            lines = text.get();
        }
        Parsed parsed = DecisionTreeSource.parse(lines, AnfBlockProcessor.measures(doc));
        if (parsed.root() == null) {
            if (model.isEmpty()) {
                LOG.debug("tornado: no root decision; rendering raw content as literal: {}", parsed.problems());
                return createBlock(parent, "literal", body);
            }
            return note(parent, "The model `" + model.get() + "` has no tree to sweep: " + String.join("; ", parsed.problems()) + ".");
        }
        double price = DecisionTreeBlockProcessor.price(attributes.get("health-price"));
        String symbol = attributes.get("symbol") == null ? "$" : String.valueOf(attributes.get("symbol"));
        Node root = parsed.root();
        DocumentCalculations.of(doc).inject(root);
        DocumentTrees trees = DocumentTrees.of(doc);
        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, price, Map.of(), trees::tree);
        List<String> problems = new ArrayList<>(parsed.problems());
        problems.addAll(rollback.findings());
        if (rollback.value(root).isEmpty()) {
            return note(parent, "The tree is not determinable, so nothing is swept: " + String.join("; ", problems) + ".");
        }
        DecisionTreeSweep sweep = DecisionTreeSweep.of(root, price, DecisionTreeSweep.DEFAULT_STEPS, trees::tree);
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        if (sweep.swings().isEmpty()) {
            parseContent(container, List.of("NOTE: No value of the tree can be swept, so there is no tornado to draw.", ""));
            return container;
        }
        boolean typst = DecisionTreeBlockProcessor.isTypst(doc);
        String title = attributes.get("title") != null ? String.valueOf(attributes.get("title")) : sweep.netValued()
                ? "How far each value, moved on its own across its range, swings the net value of the chosen alternative"
                : "How far each value, moved on its own across its range, swings the expected cost at the root";
        DecisionFigure tornado = DecisionFigureTornado.build(DecisionTreeFigures.tornado(sweep, symbol),
                typst ? DecisionTreeBlockProcessor.TYPST_TORNADO_WIDTH : DecisionTreeBlockProcessor.TORNADO_WIDTH,
                cost -> DecisionTreeFigures.money(cost, symbol));
        container.append(DecisionTreeBlockProcessor.figure(this, container, doc, tornado, title));
        if (!problems.isEmpty()) {
            parseContent(container, List.of("NOTE: The tree has gaps: " + String.join("; ", problems) + ".", ""));
        }
        return container;
    }

    private StructuralNode note(StructuralNode parent, String sentence) {
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        parseContent(container, List.of("NOTE: " + sentence, ""));
        return container;
    }
}
