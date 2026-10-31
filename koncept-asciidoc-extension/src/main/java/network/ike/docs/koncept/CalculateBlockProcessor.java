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
import java.util.Optional;
import network.ike.docs.koncept.CalculatorEvaluation.InputState;
import network.ike.docs.koncept.CalculatorEvaluation.Row;
import network.ike.docs.koncept.CalculatorSource.Binding;
import network.ike.docs.koncept.CalculatorSource.Calculation;
import network.ike.docs.koncept.CalculatorSource.Calculator;
import network.ike.docs.koncept.CalculatorSource.Clarify;
import network.ike.docs.koncept.CalculatorSource.Inline;
import network.ike.docs.koncept.CalculatorSource.InlineConcept;
import network.ike.docs.koncept.CalculatorSource.Missing;
import network.ike.docs.koncept.CalculatorSource.Statement;
import network.ike.docs.koncept.DecisionFigureTornado.Bar;
import network.ike.docs.koncept.DecisionFigureTornado.Choice;
import network.ike.docs.koncept.DecisionFigureTornado.Segment;
import network.ike.docs.koncept.DecisionFigureTornado.Tornado;
import network.ike.docs.koncept.DecisionTreeSource.Interval;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code [calculate]} block in the documentation build ({@code dev-calculator-blocks}): the
 * record that binds a document's values to a calculator the document defines. Renders the input
 * rail as a table, one row per input with what binds, the value as read and its status; the result
 * as an interval with its band and the disposition a recording would carry; the tornado of which
 * inputs move the result, solid bars for intervals that enter the computation and hollow ones for
 * the assumed swing of a point reading; and a note of the findings. Publishes the result for the
 * document, so a decision tree that follows reads it as a parameter. The static projection of the
 * calculation card the Komet assistant surface renders live.
 */
@Name("calculate")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class CalculateBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CalculateBlockProcessor.class);

    public CalculateBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> lines = reader.readLines();
        Calculation calculation = CalculatorSource.parseCalculation(lines);
        if (calculation.calculator() == null) {
            LOG.debug("calculate: no root; rendering raw content as literal: {}", calculation.problems());
            return createBlock(parent, "literal", lines);
        }
        Document doc = parent.getDocument();
        DocumentCalculations known = DocumentCalculations.of(doc);
        Optional<Calculator> calculator = known.calculator(DecisionTreeRollback.quantityKey(calculation.calculator()));
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        List<String> source = new ArrayList<>();
        if (calculator.isEmpty()) {
            source.add("NOTE: No calculator block earlier in this document defines " + DecisionTreeFormat.cell(calculation.calculator().shown())
                    + "; the calculation cannot be evaluated.");
            parseContent(container, source);
            return container;
        }
        CalculatorEvaluation evaluation = CalculatorEvaluation.of(calculator.get(), calculation);
        evaluation.share().ifPresent(share -> known.publish(calculator.get(), share));
        String title = attributes.get("title") == null
                ? "What binds to each input of the " + calculator.get().concept().shown() + " calculator, and how"
                : String.valueOf(attributes.get("title"));

        source.add("." + title);
        source.add("[cols=\"2,3,3,2\", options=\"header\"]");
        source.add("|===");
        source.add("| Input | Binds | Value as read | Status");
        for (InputState state : evaluation.inputs()) {
            source.add("| " + DecisionTreeFormat.cell(state.input().name()) + " | " + DecisionTreeFormat.cell(binds(state, calculation))
                    + " | " + DecisionTreeFormat.cell(value(state)) + " | " + DecisionTreeFormat.cell(state.statusWord()));
        }
        source.add("|===");
        source.add("");
        StringBuilder result = new StringBuilder();
        String outputName = calculator.get().output() == null || calculator.get().output().topic() == null
                ? "Result" : calculator.get().output().topic().shown();
        result.append("*").append(DecisionTreeFormat.cell(outputName)).append("*: ");
        if (evaluation.result().isPresent()) {
            result.append("*").append(evaluation.shown(evaluation.result().get())).append("*");
            evaluation.stratum().ifPresent(s -> result.append(", by the ").append(DecisionTreeFormat.cell(s.concept().shown())).append(" equation"));
            if (!evaluation.bands().isEmpty()) {
                result.append("; band ").append(String.join(" to ", evaluation.bands().stream().map(b -> b.concept().shown()).toList()));
            }
        } else {
            result.append("not determinable");
        }
        result.append("; if recorded now, the disposition is _").append(evaluation.disposition()).append("_.");
        List<String> wide = new ArrayList<>();
        for (InputState state : evaluation.inputs()) {
            if (state.interval()) {
                wide.add(state.input().name() + " " + DecisionTreeFormat.plain(state.bounds())
                        + (state.status() == CalculatorEvaluation.Status.PLAUSIBLE ? " (no statement; swept)" : ""));
            }
        }
        if (!wide.isEmpty() && evaluation.result().isPresent()) {
            result.append(" The width comes from ").append(String.join("; ", wide))
                    .append(", computed at every corner of those bounds.");
        }
        source.add(result.toString());
        parseContent(container, source);

        List<Row> rows = evaluation.rows();
        if (!rows.isEmpty()) {
            Optional<Interval> midpoint = evaluation.midpoint();
            double mid = midpoint.map(CalculatorEvaluation::mid).orElse(0.0);
            List<Bar> bars = new ArrayList<>();
            double least = mid;
            double greatest = mid;
            for (Row row : rows) {
                Interval span = row.span();
                least = Math.min(least, span.lower());
                greatest = Math.max(greatest, span.upper());
                bars.add(new Bar(row.input().name(), row.swing().lower(), row.swing().upper(), row.solid(),
                        CalculatorEvaluation.mid(row.low()), CalculatorEvaluation.mid(row.high()), span.lower(), span.upper(),
                        List.of(new Segment(span.lower(), span.upper(), Choice.BASE)), List.of()));
            }
            List<String> notes = List.of("One input at a time, the rest at their midpoints. A solid bar is an interval that enters the"
                    + " computation, a stated bound or a missing input's plausible range; a hollow bar is the assumed swing of a point"
                    + " reading. The hairline is the result with every input at its midpoint.");
            Tornado tornado = new Tornado(mid, least, greatest, bars, notes);
            CalculatorEvaluation shown = evaluation;
            DecisionFigure figure = DecisionFigureTornado.build(tornado,
                    DecisionTreeBlockProcessor.isTypst(doc) ? DecisionTreeBlockProcessor.TYPST_TORNADO_WIDTH : DecisionTreeBlockProcessor.TORNADO_WIDTH,
                    value -> shown.shown(Interval.point(value)));
            container.append(DecisionTreeBlockProcessor.figure(this, container, doc, figure,
                    "How far each input, moved on its own across its swing, moves the " + outputName));
        }
        List<String> problems = new ArrayList<>(calculation.problems());
        problems.addAll(evaluation.findings());
        if (!problems.isEmpty()) {
            parseContent(container, List.of("", "NOTE: The calculation has gaps: " + String.join("; ", problems) + "."));
        }
        return container;
    }

    private static String binds(InputState state, Calculation calculation) {
        String key = state.input().key();
        if (calculation.assumptions().containsKey(key)) {
            return "assumed in the block";
        }
        Binding binding = calculation.bindings().get(key);
        if (binding == null) {
            return "no statement";
        }
        return switch (binding) {
            case Inline inline -> "stated in the block";
            case InlineConcept concept -> "stated in the block";
            case Missing missing -> "no statement";
            case Clarify clarify -> "clarify: " + clarify.question();
            case Statement statement -> "statement " + statement.identifier() + (statement.policy() == null ? "" : " by " + statement.policy());
        };
    }

    private static String value(InputState state) {
        if (state.conceptValue() != null) {
            return state.conceptValue().shown();
        }
        if (state.bounds() == null) {
            return state.note() == null ? "" : state.note();
        }
        String unit = state.input().semantic() == null ? "" : " " + state.input().semantic().shown();
        String text = DecisionTreeFormat.plain(state.bounds()) + unit;
        if (state.status() == CalculatorEvaluation.Status.PLAUSIBLE) {
            text = "swept over " + text;
        }
        return state.note() != null && state.note().startsWith("converted") ? text + " · " + state.note() : text;
    }
}
