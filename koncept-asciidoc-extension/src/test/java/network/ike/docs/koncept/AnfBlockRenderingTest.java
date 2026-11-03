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

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An ANF statement drawn as its table, and the statements a decision tree's nodes hold, listed and marked. */
class AnfBlockRenderingTest {

    static final String STATEMENTS = """
            [anf,type=request,id=req-scan]
            --
            topic::   k:MraHead[Magnetic resonance angiography of head without contrast] @id=437731000119100
            timing::  [2,2] k:Year[year] @id=258707000
            purpose:: candidate[detection of reopening of the sealed aneurysm] disp=PENDING
            --

            [anf,type=performance,id=perf-positive]
            --
            topic::  candidate[reopening of the sealed aneurysm] disp=PENDING
            result:: [1,1] k:Presence[presence] @id=705057003
            --

            """;

    static final String TREE = """
            [decision-tree]
            ----
            decision: k:ScanNow[Scan now?] @id=rda-scan-now
              alternative: k:Scan[scan] @id=437731000119100 statement=anf:req-scan
                money = [700,700] k:Usd[US dollar] @id=rda-usd
                chance: k:MraResult[MRA result] @id=rda-mra-result
                  event: k:Positive[positive] @id=rda-positive statement=anf:perf-positive
                    p = [0.2,0.2]
                    outcome: k:Found[found] @id=rda-found statement=anf:req-scan
                      money = [5000,5000] k:Usd[US dollar] @id=rda-usd
                  residual: k:Negative[negative] @id=rda-negative statement=anf:perf-negative
                    outcome: k:Clear[clear] @id=rda-clear
              alternative: k:NoScan[no scan] @id=rda-no-scan
                outcome: k:Untreated[untreated] @id=rda-untreated
                  money = [2000,2000] k:Usd[US dollar] @id=rda-usd
            ----
            """;

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(AnfBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @Test
    @DisplayName("A statement renders as its type and name above one row per field, a candidate in italics")
    void rendersTheStatement() {
        String html = convert(STATEMENTS);
        assertTrue(html.contains("<strong>ANF request statement</strong> <code>req-scan</code>"), html);
        assertTrue(html.contains(">Timing<"), html);
        assertTrue(html.contains("[2, 2]"), html);
        assertTrue(html.contains("<em class=\"anf-candidate\">detection of reopening of the sealed aneurysm</em> (candidate)"), html);
        assertTrue(html.contains(">Purpose<"), html);
    }

    @Test
    @DisplayName("Beneath a tree, the statements its nodes hold are listed and marked, and a wrong or missing one is a gap")
    void listsTheTreesStatements() {
        String html = convert(STATEMENTS + TREE);
        assertTrue(html.contains("The ANF statements the tree&#8217;s nodes hold") || html.contains("The ANF statements the tree’s nodes hold"), html);
        assertTrue(html.contains("scan (alternative)"), html);
        assertTrue(html.contains("request <code>req-scan</code>"), html);
        assertTrue(html.contains("positive (event)"), html);
        assertTrue(html.contains(">ANF</tspan>") || html.contains(">ANF<"), "the figure marks the nodes that hold a statement");
        assertTrue(html.contains("the outcome “found” holds anf:req-scan, a request statement"), html);
        assertTrue(html.contains("the residual “negative” holds anf:perf-negative, which no anf block before the tree names")
                || html.contains("the event “negative” holds anf:perf-negative, which no anf block before the tree names"), html);
    }

    @Test
    @DisplayName("A tree whose nodes hold no anf statement lists none")
    void noStatementsNoList() {
        String html = convert(TREE.replaceAll(" statement=anf:\\S+", ""));
        assertFalse(html.contains("The ANF statements the tree"), html);
        assertFalse(html.contains(">ANF<"), html);
    }

    private static String convert(String adoc) {
        return asciidoctor.convert(adoc, Options.builder().safe(SafeMode.UNSAFE).backend("html5").build());
    }
}
