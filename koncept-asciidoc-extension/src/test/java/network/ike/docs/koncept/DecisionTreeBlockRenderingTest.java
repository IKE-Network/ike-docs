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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
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

        assertTrue(html.contains("digraph decision_tree"), "the figure's Graphviz source is emitted");
        assertTrue(html.contains("shape=box"), "a decision is a square");
        assertTrue(html.contains("shape=circle"), "a chance is a circle");
        assertTrue(html.contains("shape=plaintext"), "an outcome is a leaf");
        assertTrue(html.contains("3,880"), "the scan's rolled-back value is on the figure");
        assertTrue(html.contains("4,188"), "and the no-scan value");
        assertTrue(html.contains("Should this patient have a scan in year 2?"), "the title is the figure's");
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
        assertTrue(html.contains("if no scan: $") && html.contains("✓"), "a course is chosen");
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
        assertTrue(gaps.contains("digraph decision_tree"), "and the figure still renders");
    }

    private static String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
