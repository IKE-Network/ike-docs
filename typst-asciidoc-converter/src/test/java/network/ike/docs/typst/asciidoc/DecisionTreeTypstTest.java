/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.typst.asciidoc;

import network.ike.docs.koncept.DecisionTreeBlockProcessor;
import network.ike.docs.koncept.KonceptInlineMacro;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The {@code [decision-tree]} block through the {@code typst} backend, end to end: the block
 * processor hands over the tree and the tornado as Typst drawing code, and the converter places
 * them natively, captioned and fitted to the column, where the earlier pipeline embedded an SVG
 * image. The tree is the year-2 scan decision of {@code rda-decision-tree}, the same block
 * {@code DecisionTreeBlockRenderingTest} renders on html5, so the numbers are the topic's: the
 * scan at 3,880 against 4,188 without, and the choice flipping where the scan costs 1,008.
 */
class DecisionTreeTypstTest {

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

    /** The block as the regulatory lab topic writes it, in a document with a title and a sentence before it. */
    static final String DOCUMENT = """
            = The year-2 scan decision

            Whether to scan is the tree beneath.

            [decision-tree, title="Should this patient have a scan in year 2?"]
            ----
            %s----

            After the tree.
            """.formatted(SCAN_TREE);

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaConverterRegistry().register(TypstConverter.class);
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @AfterAll
    static void teardown() {
        if (asciidoctor != null) {
            asciidoctor.close();
        }
    }

    private static String convert(boolean standalone) {
        return asciidoctor.convert(DOCUMENT, Options.builder()
                .backend("typst").standalone(standalone).safe(SafeMode.UNSAFE).build());
    }

    private static void assertContains(String haystack, String needle, String why) {
        assertTrue(haystack.contains(needle), why + "; expected:\n" + needle + "\nin:\n" + haystack);
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + part.length())) {
            count++;
        }
        return count;
    }

    @Test
    @DisplayName("The tree and the tornado are placed as native Typst drawings in captioned figures")
    void treeAndTornadoAreNative() {
        String typst = convert(false);

        assertContains(typst, "#figure(\n  [\n#ike-drawing[\n// decision-tree\n#box(width: ",
                "the tree is a drawing in a figure, fitted to the column");
        assertContains(typst, "  ],\n  caption: [Should this patient have a scan in year 2?],\n)", "with the block's title as its caption");
        assertContains(typst, "#ike-drawing[\n// decision-tornado\n#box(width: 470pt", "the tornado follows, drawn to the column");
        assertContains(typst, "caption: [How far each value, moved on its own across its range, swings the expected cost at the root]",
                "as a captioned figure");
        assertEquals(2, count(typst, "#ike-drawing["), "one drawing each");
        assertFalse(typst.contains("image("), "no figure is an embedded image:\n" + typst);
        assertFalse(typst.contains("#ike-unconverted"), "nothing falls back:\n" + typst);
    }

    @Test
    @DisplayName("The drawing names the alternatives, carries the worked numbers and marks the flip")
    void drawingCarriesTheNumbers() {
        String typst = convert(false);

        assertContains(typst, "\"scan\"", "the scan alternative is named");
        assertContains(typst, "\"no scan\"", "and the other");
        assertContains(typst, "\"Scan now?\"", "under the root's question");
        assertContains(typst, "\"⇒ $3,880\"", "the scan's rolled-back cost is on the tree");
        assertContains(typst, "\"⇒ $4,188\"", "and the cost without");
        assertContains(typst, "\"✓ chosen\"", "the scan is chosen");
        assertEquals(2, count(typst, "thickness: 2.6pt"), "both chosen alternatives are heavy");
        assertContains(typst, "\"base $3,880\"", "the tornado's axis names the base cost");
        assertContains(typst, "\"flips at 1,008\"", "and the scan cost's bar marks the flip where the lab says");
        assertContains(typst, "\"▲\"", "the tree marks the value whose range holds the flip");
        assertContains(typst, "A square is a choice", "the legend follows the tree as text");
        assertContains(typst, "Where each number on the tree comes from", "and the worked table follows");
    }

    @Test
    @DisplayName("The whole document compiles under Typst, when it is on the PATH")
    void compilesUnderTypst(@TempDir Path dir) throws IOException, InterruptedException {
        assumeTrue(typstOnPath(), "typst is not on the PATH: the drawing is checked as text only");
        Path source = dir.resolve("scan.typ");
        Files.writeString(source, convert(true), StandardCharsets.UTF_8);
        Path pdf = dir.resolve("scan.pdf");
        Path log = dir.resolve("typst.log");

        Process typst = new ProcessBuilder("typst", "compile", "--root", dir.toString(), source.toString(), pdf.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        assertTrue(typst.waitFor(2, TimeUnit.MINUTES), "typst finished");
        String output = Files.readString(log, StandardCharsets.UTF_8);

        assertEquals(0, typst.exitValue(), "typst compiled the document:\n" + output);
        assertFalse(output.contains("error"), "without an error:\n" + output);
        assertTrue(Files.size(pdf) > 0, "and wrote the PDF");
    }

    private static boolean typstOnPath() {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String entry : List.of(path.split(java.io.File.pathSeparator))) {
            if (Files.isExecutable(Path.of(entry, "typst"))) {
                return true;
            }
        }
        return false;
    }
}
