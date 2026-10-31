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
import java.util.List;
import java.util.Map;
import network.ike.docs.koncept.CalculatorSource.Calculator;
import network.ike.docs.koncept.CalculatorSource.Input;
import network.ike.docs.koncept.CalculatorSource.Parsed;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code [calculator]} block in the documentation build ({@code dev-calculator-blocks}): the
 * method, rendered as what it is and what binds to it, a table of the inputs with the semantic
 * each expects, the domain the equation is valid over, and what the evaluator does when nothing
 * binds. The calculator is defined for the document, so a {@code [calculate]} block that follows
 * binds to it. The static projection of the method card the Komet assistant surface renders live.
 */
@Name("calculator")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class CalculatorBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CalculatorBlockProcessor.class);

    public CalculatorBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> lines = reader.readLines();
        Parsed parsed = CalculatorSource.parse(lines);
        if (parsed.calculator() == null || parsed.calculator().concept() == null) {
            LOG.debug("calculator: no root; rendering raw content as literal: {}", parsed.problems());
            return createBlock(parent, "literal", lines);
        }
        Calculator calculator = parsed.calculator();
        DocumentCalculations.of(parent.getDocument()).define(calculator);
        String title = attributes.get("title") == null
                ? "The " + calculator.concept().shown() + " calculator: its inputs and what binds to them"
                : String.valueOf(attributes.get("title"));
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        List<String> source = new ArrayList<>();
        StringBuilder intro = new StringBuilder();
        intro.append("*Calculator* ").append(DecisionTreeFormat.cell(calculator.concept().shown()));
        intro.append(" — ").append(calculator.inputs().size()).append(" inputs, ").append(calculator.strata().size())
                .append(calculator.strata().size() == 1 ? " stratum, " : " strata, ").append(calculator.bands().size()).append(" bands");
        if (calculator.source() != null) {
            intro.append(", from ").append(DecisionTreeFormat.cell(calculator.source()));
        }
        intro.append('.');
        source.add(intro.toString());
        source.add("");
        source.add("." + title);
        source.add("[cols=\"2,3,3\", options=\"header\"]");
        source.add("|===");
        source.add("| Input | Expects | When nothing binds");
        for (Input input : calculator.inputs()) {
            StringBuilder expects = new StringBuilder();
            if (input.conceptValued()) {
                expects.append("one of ").append(String.join(", ", input.values().stream().map(v -> v.shown()).toList()));
            } else {
                expects.append("in ").append(input.semantic() == null ? "?" : input.semantic().shown());
                if (input.domain() != null) {
                    expects.append(" · valid ").append(DecisionTreeFormat.plain(input.domain()));
                }
                for (CalculatorSource.Conversion conversion : input.conversions()) {
                    expects.append(" · also ").append(conversion.fromSemantic().shown()).append(" × ")
                            .append(DecisionTreeFormat.plain(conversion.factor()));
                }
            }
            StringBuilder policy = new StringBuilder(input.required() ? "required" : "optional");
            if (input.whenMissing() != null) {
                policy.append(" · ").append(input.whenMissing().replace('-', ' '));
                if (input.missingBounds() != null) {
                    policy.append(' ').append(DecisionTreeFormat.plain(input.missingBounds()));
                }
                if (input.missingConcept() != null) {
                    policy.append(' ').append(input.missingConcept().shown());
                }
            } else {
                policy.append(" · not determinable");
            }
            source.add("| " + DecisionTreeFormat.cell(input.name()) + " | " + DecisionTreeFormat.cell(expects.toString())
                    + " | " + DecisionTreeFormat.cell(policy.toString()));
        }
        source.add("|===");
        if (!parsed.problems().isEmpty()) {
            source.add("");
            source.add("NOTE: The calculator has gaps: " + String.join("; ", parsed.problems()) + ".");
        }
        parseContent(container, source);
        return container;
    }
}
