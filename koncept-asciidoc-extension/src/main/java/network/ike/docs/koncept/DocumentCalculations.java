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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import network.ike.docs.koncept.CalculatorSource.Calculator;
import network.ike.docs.koncept.DecisionTreeSource.Concept;
import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Measure;
import network.ike.docs.koncept.DecisionTreeSource.MeasureKind;
import network.ike.docs.koncept.DecisionTreeSource.Node;
import org.asciidoctor.ast.Document;

/**
 * What the blocks of one document know of each other in the build ({@code dev-calculator-blocks}):
 * the calculators its {@code [calculator]} blocks define, and the shares its {@code [calculate]}
 * blocks have computed, each under the calculator's concept and its output's topic. A decision
 * tree later in the document reads such a share as a parameter it states no line for. Held per
 * Asciidoctor document and dropped with it.
 */
final class DocumentCalculations {

    private static final Map<Document, DocumentCalculations> BY_DOCUMENT = new WeakHashMap<>();

    private final Map<String, Calculator> calculators = new HashMap<>();
    private final Map<String, Interval> shares = new HashMap<>();
    private final Map<String, Concept> concepts = new HashMap<>();

    private DocumentCalculations() {
    }

    static synchronized DocumentCalculations of(Document document) {
        return BY_DOCUMENT.computeIfAbsent(document, d -> new DocumentCalculations());
    }

    void define(Calculator calculator) {
        for (String key : calculator.keys()) {
            calculators.put(key, calculator);
        }
    }

    Optional<Calculator> calculator(String key) {
        return Optional.ofNullable(calculators.get(key));
    }

    /** A computed share, readable under each key, with the concept a tree's injected parameter is named by. */
    void publish(Calculator calculator, Interval share) {
        for (String key : calculator.keys()) {
            shares.put(key, share);
        }
        concepts.put(DecisionTreeRollback.quantityKey(calculator.concept()), calculator.concept());
        if (calculator.output() != null && calculator.output().topic() != null) {
            concepts.put(DecisionTreeRollback.quantityKey(calculator.output().topic()), calculator.output().topic());
        }
    }

    Optional<Interval> share(String key) {
        return Optional.ofNullable(shares.get(key));
    }

    /**
     * Adds to a tree's root, as stated parameters, the quantities it reads with {@code from} alone,
     * states no parameter for, and this document has computed: the tree's text never carries the
     * number, and the assumptions table says where it came from.
     */
    void inject(Node root) {
        List<Measure> parameters = root.parameters();
        for (Measure measure : allMeasures(root)) {
            if (!measure.readsAParameter()) {
                continue;
            }
            String key = DecisionTreeRollback.quantityKey(measure.derivationConcept());
            boolean stated = parameters.stream().anyMatch(p -> p.quantity() != null
                    && DecisionTreeRollback.quantityKey(p.quantity()).equals(key));
            boolean injected = root.parameters().stream().anyMatch(p -> p.quantity() != null
                    && DecisionTreeRollback.quantityKey(p.quantity()).equals(key));
            if (stated || injected) {
                continue;
            }
            Interval share = shares.get(key);
            if (share == null) {
                continue;
            }
            Concept quantity = concepts.getOrDefault(key, measure.derivationConcept());
            root.measures.add(new Measure(MeasureKind.PARAMETER, significant(share, 4), null,
                    "computed by the document's calculator", null, quantity, null, null, List.of(), measure.line()));
        }
    }

    private static List<Measure> allMeasures(Node node) {
        List<Measure> all = new java.util.ArrayList<>(node.measures);
        for (Node child : node.children) {
            all.addAll(allMeasures(child));
        }
        return all;
    }

    /** Bounds held to a number of significant figures, the lower down and the upper up. */
    static Interval significant(Interval bounds, int digits) {
        return new Interval(significant(bounds.lower(), digits, false), significant(bounds.upper(), digits, true),
                bounds.lowerOpen(), bounds.upperOpen());
    }

    private static double significant(double value, int digits, boolean up) {
        if (value == 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }
        double magnitude = Math.pow(10, digits - 1 - Math.floor(Math.log10(Math.abs(value))));
        double scaled = value * magnitude;
        return (up ? Math.ceil(scaled - 1e-9) : Math.floor(scaled + 1e-9)) / magnitude;
    }
}
