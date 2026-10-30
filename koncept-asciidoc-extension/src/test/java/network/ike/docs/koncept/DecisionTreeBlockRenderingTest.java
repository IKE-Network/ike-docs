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
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.converter.ConverterFor;
import org.asciidoctor.converter.StringConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The year-2 scan decision of {@code rda-decision-tree} as a block: the store-free reader and
 * rollback land on the topic's hand-worked numbers, and the processor renders the figure and
 * the worked table in its place.
 */
class DecisionTreeBlockRenderingTest {

    private static final String USD = "11111111-0000-4000-8000-00000000aa01";
    private static final String QALY = "11111111-0000-4000-8000-00000000aa02";

    /** The scan tree in the block grammar, at the topic's worked probabilities. */
    static final String SCAN_TREE = """
            decision: k:ScanNow[Scan now?] @id=11111111-0000-4000-8000-000000000001
              alternative: k:Scan[scan] @id=11111111-0000-4000-8000-000000000002
                money = [700,700] k:Usd[US dollar] @id=%1$s basis=estimated
                health = [0.002,0.002] k:Qaly[QALY] @id=%2$s
                chance: k:MraResult[MRA result] @id=11111111-0000-4000-8000-000000000003
                  event: k:Positive[positive] @id=11111111-0000-4000-8000-000000000004
                    p = [0.214,0.214]
                    money = [5000,5000] k:Usd[US dollar] @id=%1$s
                    health = [0.01,0.01] k:Qaly[QALY] @id=%2$s
                    chance: k:Angiogram[Angiogram confirms?] @id=11111111-0000-4000-8000-000000000005
                      event: k:Reopening[reopening] @id=11111111-0000-4000-8000-000000000006
                        p = [0.08411214953271028,0.08411214953271028]
                        decision: k:Retreat[Retreat?] @id=11111111-0000-4000-8000-000000000007
                          alternative: k:DoRetreat[retreat] @id=11111111-0000-4000-8000-000000000008
                            money = [35000,35000] k:Usd[US dollar] @id=%1$s
                            health = [0.15,0.15] k:Qaly[QALY] @id=%2$s
                            outcome: k:RiskCut[risk cut to 1.8%%] @id=11111111-0000-4000-8000-000000000009
                              money = [2731.4,2731.4] k:Usd[US dollar] @id=%1$s
                              health = [0.182093,0.182093] k:Qaly[QALY] @id=%2$s
                          alternative: k:Leave[leave] @id=11111111-0000-4000-8000-000000000010
                            outcome: k:Untreated[18.2%% rupture risk] @id=11111111-0000-4000-8000-000000000011
                              money = [27314,27314] k:Usd[US dollar] @id=%1$s
                              health = [1.82093,1.82093] k:Qaly[QALY] @id=%2$s
                      residual: k:NoReopening[no reopening] @id=11111111-0000-4000-8000-000000000012
                        outcome: k:Clear[clear] @id=11111111-0000-4000-8000-000000000013
                  residual: k:Negative[negative] @id=11111111-0000-4000-8000-000000000014
                    chance: k:Missed[Reopening missed?] @id=11111111-0000-4000-8000-000000000015
                      event: k:MissedReopening[reopening] @id=11111111-0000-4000-8000-000000000016
                        p = [0.002544529262086514,0.002544529262086514]
                        outcome: k:Untreated2[18.2%% rupture risk] @id=11111111-0000-4000-8000-000000000017
                          money = [27314,27314] k:Usd[US dollar] @id=%1$s
                          health = [1.82093,1.82093] k:Qaly[QALY] @id=%2$s
                      residual: k:Fine[no reopening] @id=11111111-0000-4000-8000-000000000018
                        outcome: k:Clear2[clear] @id=11111111-0000-4000-8000-000000000019
              alternative: k:NoScan[no scan] @id=11111111-0000-4000-8000-000000000020
                chance: k:Present[Reopening present?] @id=11111111-0000-4000-8000-000000000021
                  event: k:Yes[reopening] @id=11111111-0000-4000-8000-000000000022
                    p = [0.02,0.02]
                    outcome: k:Untreated3[18.2%% rupture risk] @id=11111111-0000-4000-8000-000000000023
                      money = [27314,27314] k:Usd[US dollar] @id=%1$s
                      health = [1.82093,1.82093] k:Qaly[QALY] @id=%2$s
                  residual: k:No[no reopening] @id=11111111-0000-4000-8000-000000000024
                    outcome: k:Clear3[clear] @id=11111111-0000-4000-8000-000000000025
            """.formatted(USD, QALY);

    /** The scan tree stated from its model assumptions alone: parameters, and every other number derived. */
    static final String ASSUMED_TREE = """
            decision: k:ScanNow[Scan now?] @id=11111111-0000-4000-8000-000000000001
              parameter = [0.02,0.02] k:Reopening[Late reopening hazard in year 2] @id=q1 basis=estimated
              parameter = [0.9,0.9] k:Sensitivity[MRA sensitivity] @id=q2 basis=cited source=kwee2007
              parameter = [0.8,0.8] k:Specificity[MRA specificity] @id=q3 basis=cited source=kwee2007
              parameter = k:FalseNegative[MRA false-negative rate] @id=q4 from k:Complement[] of k:Sensitivity[] @id=q2
              parameter = k:FalsePositive[MRA false-positive rate] @id=q5 from k:Complement[] of k:Specificity[] @id=q3
              parameter = [0.01,0.01] k:Hazard[Annual rupture risk of an untreated reopening] @id=q6
              parameter = [20,20] k:Years[Years an untreated reopening stays at risk] @id=q7
              parameter = k:Risk20[Twenty-year rupture risk] @id=q8 from k:CumulativeHazard[Cumulative hazard] of k:Hazard[] @id=q6 k:Years[] @id=q7
              parameter = [150000,150000] k:RuptureCost[Cost of a rupture] @id=q9 in k:Usd[US dollar] @id=%1$s
              parameter = [10,10] k:RuptureQaly[QALYs lost per rupture] @id=q10 in k:Qaly[QALY] @id=%2$s
              parameter = [0.9,0.9] k:Removes[Share of rupture risk retreatment removes] @id=q11
              parameter = k:Remains[Share of rupture risk retreatment leaves] @id=q12 from k:Complement[] of k:Removes[] @id=q11
              alternative: k:Scan[scan] @id=11111111-0000-4000-8000-000000000002
                money = [700,700] k:Usd[US dollar] @id=%1$s
                health = [0.002,0.002] k:Qaly[QALY] @id=%2$s
                chance: k:MraResult[MRA result] @id=11111111-0000-4000-8000-000000000003
                  event: k:Positive[positive] @id=11111111-0000-4000-8000-000000000004
                    p = from k:BayesMarginal[Bayes marginal] of k:Reopening[] @id=q1 k:Sensitivity[] @id=q2 k:FalsePositive[] @id=q5
                    money = [5000,5000] k:Usd[US dollar] @id=%1$s
                    health = [0.01,0.01] k:Qaly[QALY] @id=%2$s
                    chance: k:Angiogram[Angiogram confirms?] @id=11111111-0000-4000-8000-000000000005
                      event: k:Confirmed[reopening] @id=11111111-0000-4000-8000-000000000006
                        p = from k:BayesPosterior[Bayes posterior] of k:Reopening[] @id=q1 k:Sensitivity[] @id=q2 k:FalsePositive[] @id=q5
                        decision: k:Retreat[Retreat?] @id=11111111-0000-4000-8000-000000000007
                          alternative: k:DoRetreat[retreat] @id=11111111-0000-4000-8000-000000000008
                            money = [35000,35000] k:Usd[US dollar] @id=%1$s
                            health = [0.15,0.15] k:Qaly[QALY] @id=%2$s
                            outcome: k:RiskCut[risk cut] @id=11111111-0000-4000-8000-000000000009
                              money = from k:Product[] of k:Remains[] @id=q12 k:Risk20[] @id=q8 k:RuptureCost[] @id=q9 in k:Usd[US dollar] @id=%1$s
                              health = from k:Product[] of k:Remains[] @id=q12 k:Risk20[] @id=q8 k:RuptureQaly[] @id=q10 in k:Qaly[QALY] @id=%2$s
                          alternative: k:Leave[leave] @id=11111111-0000-4000-8000-000000000010
                            outcome: k:Untreated[rupture risk] @id=11111111-0000-4000-8000-000000000011
                              money = from k:Product[] of k:Risk20[] @id=q8 k:RuptureCost[] @id=q9 in k:Usd[US dollar] @id=%1$s
                              health = from k:Product[] of k:Risk20[] @id=q8 k:RuptureQaly[] @id=q10 in k:Qaly[QALY] @id=%2$s
                      residual: k:NoReopening[no reopening] @id=11111111-0000-4000-8000-000000000012
                        outcome: k:Clear[clear] @id=11111111-0000-4000-8000-000000000013
                  residual: k:Negative[negative] @id=11111111-0000-4000-8000-000000000014
                    chance: k:Missed[Reopening missed?] @id=11111111-0000-4000-8000-000000000015
                      event: k:MissedReopening[reopening] @id=11111111-0000-4000-8000-000000000016
                        p = from k:BayesPosterior[Bayes posterior] of k:Reopening[] @id=q1 k:FalseNegative[] @id=q4 k:Specificity[] @id=q3
                        outcome: k:Untreated2[rupture risk] @id=11111111-0000-4000-8000-000000000017
                          money = from k:Product[] of k:Risk20[] @id=q8 k:RuptureCost[] @id=q9 in k:Usd[US dollar] @id=%1$s
                          health = from k:Product[] of k:Risk20[] @id=q8 k:RuptureQaly[] @id=q10 in k:Qaly[QALY] @id=%2$s
                      residual: k:Fine[no reopening] @id=11111111-0000-4000-8000-000000000018
                        outcome: k:Clear2[clear] @id=11111111-0000-4000-8000-000000000019
              alternative: k:NoScan[no scan] @id=11111111-0000-4000-8000-000000000020
                chance: k:Present[Reopening present?] @id=11111111-0000-4000-8000-000000000021
                  event: k:Yes[reopening] @id=11111111-0000-4000-8000-000000000022
                    p = [0.02,0.02]
                    outcome: k:Untreated3[rupture risk] @id=11111111-0000-4000-8000-000000000023
                      money = from k:Product[] of k:Risk20[] @id=q8 k:RuptureCost[] @id=q9 in k:Usd[US dollar] @id=%1$s
                      health = from k:Product[] of k:Risk20[] @id=q8 k:RuptureQaly[] @id=q10 in k:Qaly[QALY] @id=%2$s
                  residual: k:No[no reopening] @id=11111111-0000-4000-8000-000000000024
                    outcome: k:Clear3[clear] @id=11111111-0000-4000-8000-000000000025
            """.formatted(USD, QALY);

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
        asciidoctor.javaConverterRegistry().register(PassthroughConverter.class);
    }

    /**
     * A stand-in for the {@code typst} backend, which lives in {@code typst-asciidoc-converter}
     * above this module in the reactor: it writes every passthrough block as its roles, its title
     * and its content, and everything else as its content, so a test can read what the processor
     * hands the real converter. The real pipeline, end to end, is {@code DecisionTreeTypstTest}
     * beside the converter.
     */
    @ConverterFor(value = "typst", suffix = ".typ")
    public static class PassthroughConverter extends StringConverter {

        public PassthroughConverter(String backend, Map<String, Object> opts) {
            super(backend, opts);
        }

        @Override
        public String convert(ContentNode node, String transform, Map<Object, Object> opts) {
            if (!(node instanceof StructuralNode block)) {
                return "";
            }
            String kind = transform != null ? transform : node.getNodeName();
            Object content = block.getContent();
            if ("pass".equals(kind)) {
                return "pass[" + String.join(" ", block.getRoles()) + "][" + block.getTitle() + "]\n" + content + "\n";
            }
            return content == null ? "" : content + "\n";
        }
    }

    @Test
    @DisplayName("The reader builds the scan tree with no problems")
    void readsTheScanTree() {
        Parsed parsed = DecisionTreeSource.parse(List.of(SCAN_TREE.split("\n")));

        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        Node root = parsed.root();
        assertEquals("Scan now?", root.label());
        assertEquals(2, root.children.size(), "two alternatives");
        assertEquals(2, root.children.get(0).measures.size(), "the scan carries its money and health costs");
    }

    @Test
    @DisplayName("The rollback lands on the topic's numbers: $3,880 against $4,188, the scan saving $308")
    void rollsBackToTheWorkedNumbers() {
        Node root = DecisionTreeSource.parse(List.of(SCAN_TREE.split("\n"))).root();
        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, 100_000);

        assertTrue(rollback.findings().isEmpty(), () -> "findings: " + rollback.findings());
        double scan = rollback.value(root.children.get(0)).orElseThrow().lower();
        double noScan = rollback.value(root.children.get(1)).orElseThrow().lower();
        assertEquals(3_880, scan, 1.0);
        assertEquals(4_188, noScan, 1.0);
        assertEquals(308, noScan - scan, 1.0);
        assertEquals(root.children.get(0), rollback.chosen(root).orElseThrow(), "scanning is chosen");
        assertEquals("§", rollback.step(root).orElseThrow().mark());
        assertEquals(rollback.steps().size(), rollback.step(root).orElseThrow().number(), "the root is worked last");
    }

    @Test
    @DisplayName("Below the break-even price the other course is chosen")
    void breakEven() {
        Node root = DecisionTreeSource.parse(List.of(SCAN_TREE.split("\n"))).root();
        assertEquals(root.children.get(1), DecisionTreeRollback.of(root, 50_000).chosen(root).orElseThrow());
        assertEquals(root.children.get(0), DecisionTreeRollback.of(root, 150_000).chosen(root).orElseThrow());
    }

    @Test
    @DisplayName("The block renders the Graphviz figure and the worked table in its place")
    void rendersFigureAndTable() {
        String html = convert("""
                [decision-tree, title="Should this patient have a scan in year 2?"]
                ----
                %s----
                """.formatted(SCAN_TREE));

        assertTrue(html.contains("<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"decision-tree\""), "the tree is inline SVG");
        assertEquals(2, count(html, "class=\"glyph decision\""), "a square for each decision");
        assertEquals(4, count(html, "class=\"glyph chance\""), "a circle for each chance");
        assertEquals(7, count(html, "class=\"glyph outcome\""), "a triangle for each outcome");
        assertEquals(12, count(html, "<polyline class=\"run"), "an elbow for each branch");
        assertEquals(2, count(html, "class=\"run chosen\""), "the chosen alternatives are heavy");
        assertEquals(4, count(html, "class=\"barred\""), "the rejected alternatives are barred");
        assertTrue(html.contains("\u21D2 $3,880"), "the scan's rolled-back value is on the figure");
        assertTrue(html.contains("\u21D2 $4,188"), "and the no-scan value");
        assertTrue(html.contains("class=\"callout-number\""), "every worked number carries its callout");
        assertTrue(html.contains("Figure 1. Should this patient have a scan in year 2?"), "the title is the figure's");
        assertTrue(html.contains("class=\"decision-tornado\""), "the tornado follows the tree");
        assertTrue(html.contains("Figure 2. How far each value"), "as a numbered figure");
        assertTrue(html.contains("flips at 1,008"), "the scan's cost flips the choice where the lab says");
        assertTrue(html.contains("overflow-x: auto"), "a wide tree scrolls");
        assertTrue(html.contains("A square is a choice"), "the legend follows the tree");
        assertTrue(html.contains("Where each number on the tree comes from"), "the worked table follows");
        assertTrue(html.contains("figure-callout"), "with the topic's callouts");
        assertTrue(html.contains("The square takes the cheaper"), "and the decision's working");
        assertTrue(html.contains("koncept-identicon") || html.contains("data:image"),
                "a node with a UUID identity draws its identicon chip in the table");
        assertFalse(html.contains("The tree has gaps"), "no gaps are reported");
    }

    @Test
    @DisplayName("The health price attribute moves the figure")
    void healthPriceAttribute() {
        String html = convert("""
                [decision-tree, health-price=50000]
                ----
                %s----
                """.formatted(SCAN_TREE));

        assertTrue(html.contains("50,000 a year of healthy life"), "the legend names the price");
        assertTrue(html.contains("\u2713 chosen"), "a course is chosen");
        Node root = DecisionTreeSource.parse(List.of(SCAN_TREE.split("\n"))).root();
        assertEquals(root.children.get(1), DecisionTreeRollback.of(root, 50_000).chosen(root).orElseThrow(), "not scanning");
        assertEquals(50_000, DecisionTreeBlockProcessor.price("50,000"));
        assertEquals(100_000, DecisionTreeBlockProcessor.price(null));
    }

    @Test
    @DisplayName("A block with no root decision renders as a literal block, and a gap is named")
    void declinesAndNames() {
        String literal = convert("""
                [decision-tree]
                ----
                chance: k:X[x] @id=11111111-0000-4000-8000-000000000001
                ----
                """);
        assertTrue(literal.contains("literalblock"), "no root decision renders as literal");

        String gaps = convert("""
                [decision-tree]
                ----
                decision: k:ScanNow[Scan now?] @id=11111111-0000-4000-8000-000000000001
                  alternative: k:Scan[scan] @id=11111111-0000-4000-8000-000000000002
                    money = [700,700]
                ----
                """);
        assertTrue(gaps.contains("The tree has gaps"), "a cost without a semantic is named");
        assertTrue(gaps.contains("class=\"decision-tree\""), "and the figure still renders");
        assertTrue(gaps.contains("stroke-dasharray"), "with the undeterminable node dashed");
        assertFalse(gaps.contains("decision-tornado"), "nothing to sweep until the tree is determinable");
    }

    @Test
    @DisplayName("Stated from its assumptions alone, the block derives the worked numbers and tables the assumptions")
    void assumptionsDeriveTheWorkedNumbers() {
        Parsed parsed = DecisionTreeSource.parse(List.of(ASSUMED_TREE.split("\n")));
        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        Node root = parsed.root();
        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, 100_000);

        assertTrue(rollback.findings().isEmpty(), () -> "findings: " + rollback.findings());
        assertEquals(3_880, rollback.value(root.children.get(0)).orElseThrow().lower(), 1.0);
        assertEquals(4_188, rollback.value(root.children.get(1)).orElseThrow().lower(), 1.0);
        assertEquals(7, rollback.steps().stream().filter(step -> step.node() == null).count(),
                "the four derived parameters and the three derived shares are worked");
        assertTrue(rollback.steps().subList(0, 4).stream().allMatch(step -> step.node() == null && !step.what().startsWith("% ")),
                "the parameters come first");
        assertTrue(rollback.steps().stream().anyMatch(step -> step.what().equals("% positive") && step.share()),
                "the positive-scan share is worked under its event");

        String html = convert("""
                [decision-tree]
                ----
                %s----
                """.formatted(ASSUMED_TREE));
        assertTrue(html.contains("Model assumptions and default values"), "the stated parameters are tabled");
        assertTrue(html.contains("cited, kwee2007"), "with their basis and source");
        assertTrue(html.contains("Cumulative hazard of"), "a derived parameter is worked in the table");
        assertTrue(html.contains("3,880"), "and the figure carries the rolled-back value");
        assertFalse(html.contains("The tree has gaps"), () -> html);
    }

    @Test
    @DisplayName("A derivation kind written bare after from, as the live surface's assistant writes it, reads as the kind")
    void readsBareDerivationKinds() {
        String bare = ASSUMED_TREE
                .replace("from k:Complement[] of", "from Complement of")
                .replace("from k:CumulativeHazard[Cumulative hazard] of", "from CumulativeHazard of")
                .replace("from k:BayesMarginal[Bayes marginal] of", "from BayesMarginal of")
                .replace("from k:BayesPosterior[Bayes posterior] of", "from BayesPosterior of")
                .replace("from k:Product[] of", "from Product of");
        assertFalse(bare.contains("from k:"), "every kind is written bare");

        Parsed parsed = DecisionTreeSource.parse(List.of(bare.split("\n")));

        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        DecisionTreeRollback rollback = DecisionTreeRollback.of(parsed.root(), 100_000);
        assertTrue(rollback.findings().isEmpty(), () -> "findings: " + rollback.findings());
        assertEquals(3_880, rollback.value(parsed.root().children.get(0)).orElseThrow().lower(), 1.0);
        assertEquals(4_188, rollback.value(parsed.root().children.get(1)).orElseThrow().lower(), 1.0);
    }

    @Test
    @DisplayName("The sweep moves every stated value one way, finds the flips, and the Typst twin draws the same bars")
    void sweepAndTornado() {
        Node root = DecisionTreeSource.parse(List.of(SCAN_TREE.split("\n"))).root();
        DecisionTreeSweep sweep = DecisionTreeSweep.of(root, 100_000, DecisionTreeSweep.DEFAULT_STEPS);
        assertEquals(3_880, sweep.baseRoot(), 1.0);
        assertEquals(root.children.get(0), sweep.baseChosen().orElseThrow());
        DecisionTreeSource.Measure scanCost = root.children.get(0).costs().get(0);
        DecisionTreeSweep.Swing swing = sweep.swing(scanCost).orElseThrow();
        assertFalse(swing.stated(), "a point swings over an assumed range");
        assertEquals(350, swing.range().lower(), 1e-9);
        assertEquals(1_400, swing.range().upper(), 1e-9);
        DecisionTreeSweep.Flip flip = swing.flipsAt(root).get(0);
        assertEquals(1_008, flip.at(), 2.0, "the choice flips when the scan is dearer by 308");
        assertEquals(root.children.get(0), flip.chosenBelow());
        assertEquals(root.children.get(1), flip.chosenAbove());
        DecisionTreeSweep.Swing price = sweep.swing(DecisionTreeSweep.HEALTH_PRICE).orElseThrow();
        double breakEven = price.flipsAt(root).get(0).at();
        assertTrue(breakEven > 80_000 && breakEven < 90_000, "break-even " + breakEven);
        for (int i = 1; i < sweep.swings().size(); i++) {
            assertTrue(sweep.swings().get(i - 1).width() >= sweep.swings().get(i).width(), "longest first");
        }
        DecisionFigure tornado = DecisionFigureTornado.build(DecisionTreeFigures.tornado(sweep), 640,
                cost -> DecisionTreeFigures.money(cost, "$"));
        String typst = DecisionFigureTypst.write(tornado);
        assertTrue(typst.startsWith("// decision-tornado\n#box(width: 640pt"), typst.substring(0, 40));
        assertTrue(typst.contains("\"flips at 1,008\""));
        assertTrue(typst.contains("\"base $3,880\""));
        String tree = DecisionFigureTypst.write(DecisionTreeFigures.tree(root, DecisionTreeRollback.of(root, 100_000), "$",
                Optional.of(sweep)));
        assertTrue(tree.contains("thickness: 2.6pt"), "the chosen alternative is heavy in print too");
        assertTrue(tree.contains("\"\u25B2\""), "a value that flips the root is marked");
    }

    @Test
    @DisplayName("On the typst backend the block hands the converter its figures as Typst drawing code, never an image")
    void typstBackendHandsOverNativeDrawings() {
        String typst = asciidoctor.convert("""
                [decision-tree, title="Should this patient have a scan in year 2?"]
                ----
                %s----
                """.formatted(SCAN_TREE), Options.builder().safe(SafeMode.UNSAFE).backend("typst").build());

        assertTrue(typst.contains("pass[typst drawing decision-tree][Should this patient have a scan in year 2?]\n// decision-tree\n#box(width: "),
                "the tree is a titled passthrough of Typst code with the roles the converter reads:\n" + typst);
        assertTrue(typst.contains("\"scan\""), "the alternatives are named");
        assertTrue(typst.contains("\"no scan\""));
        assertTrue(typst.contains("\"\u21D2 $3,880\""), "the scan's rolled-back value is on the tree");
        assertTrue(typst.contains("\"\u21D2 $4,188\""), "and the no-scan value");
        assertTrue(typst.contains("thickness: 2.6pt"), "the chosen alternative is heavy");
        assertTrue(typst.contains("pass[typst drawing decision-tornado][How far each value"), "the tornado follows the tree");
        assertTrue(typst.contains("#box(width: 470pt"), "drawn to the Typst page's column");
        assertTrue(typst.contains("\"flips at 1,008\""), "and marks the flip at the scan cost the lab says");
        assertFalse(typst.contains(".svg"), "no SVG file stands in for a figure:\n" + typst);
        assertTrue(typst.contains("A square is a choice"), "the legend still follows the tree");
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + part.length())) {
            count++;
        }
        return count;
    }

    private static String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
