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

import static org.junit.jupiter.api.Assertions.assertTrue;

/** A tree of consequences drawn once and folded in at another tree's outcome, linked both ways. */
class FoldInRenderingTest {

    static final String CONSEQUENCES = """
            [decision-tree, cohort=1000, title="What a rupture leads to"]
            ----
            chance: k:RuptureConsequences[Consequences of a rupture] @id=rda-rupture-consequences
              parameter = [0.35,0.35] k:CaseFatality[Case fatality of a rupture] @id=rda-case-fatality basis=estimated
              event: k:Death[death] @id=rda-death
                p = from k:CaseFatality[] @id=rda-case-fatality
                money = [50000,50000] k:Usd[US dollar] @id=rda-usd
                count = [1,1] k:Deaths[deaths] @id=rda-deaths
                outcome: k:Dead[death] @id=rda-dead
              residual: k:Survival[survival] @id=rda-survival
                money = [200000,200000] k:Usd[US dollar] @id=rda-usd
                outcome: k:Survived[survival] @id=rda-survived
            ----

            """;

    static final String CHOICE = """
            [decision-tree, cohort=1000, title="Treat?"]
            ----
            decision: k:Treat[Treat?] @id=rda-treat
              alternative: k:DoTreat[treat] @id=rda-do-treat
                money = [10000,10000] k:Usd[US dollar] @id=rda-usd
                outcome: k:Safe[safe] @id=rda-safe
              alternative: k:Wait[wait] @id=rda-wait
                chance: k:Ruptures[Rupture?] @id=rda-ruptures
                  event: k:Rupture[rupture] @id=rda-rupture
                    p = [0.1,0.1]
                    outcome: k:Ruptured[rupture] @id=rda-ruptured tree=rda-rupture-consequences
                  residual: k:Intact[intact] @id=rda-intact
                    outcome: k:Fine[fine] @id=rda-fine
            ----
            """;

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @Test
    @DisplayName("A tree of consequences rolls back from its chance root, with a one-row ledger and a tally of who reaches it")
    void consequencesAlone() {
        String html = convert(CONSEQUENCES);
        assertTrue(html.contains("Per 1,000 who reach it"), html);
        assertTrue(html.contains("Of 1,000 who reach “Consequences of a rupture”"), html);
        assertTrue(!html.contains("The tree has gaps"), html);
    }

    @Test
    @DisplayName("The outcome folds the tree in: its value, its deaths in the ledger, its endpoints in the tally, links both ways")
    void foldsInAndLinks() {
        String html = convert(CONSEQUENCES + CHOICE);
        assertTrue(!html.contains("The tree has gaps"), html);
        assertTrue(html.contains("folds in “Consequences of a rupture”"), "the figure names what the outcome folds in");
        assertTrue(html.contains("href=\"#decision-tree-rda-rupture-consequences\""), "the folding tree links to it");
        assertTrue(html.contains("id=\"decision-tree-rda-rupture-consequences\""), html);
        assertTrue(html.contains("Folded in by <a href=\"#decision-tree-rda-treat\">Treat?</a>"), "and it links back");
        // wait: 10% rupture × (0.35 × 50,000 + 0.65 × 200,000) = 14,750 a case
        assertTrue(html.contains("14,750"), "the folded tree's expected cost reaches the outcome");
        assertTrue(html.contains(">deaths<"), "the folded tree's counted unit is a ledger column");
        assertTrue(html.contains("35 death"), "of 1,000 who wait, 35 die");
        // a long name is cut to the tornado's label column, so the start of it is what shows
        assertTrue(html.contains("Case fatality of a rupture in \u201CConsequences"),
                "the folding tree's tornado swings the folded tree's values too");
    }

    @Test
    @DisplayName("A tree that folds in one the document has not drawn yet says so among its gaps")
    void missing() {
        String html = convert(CHOICE);
        assertTrue(html.contains("folds in a tree that no block before it in the document draws"), html);
    }

    private static String convert(String adoc) {
        return asciidoctor.convert(adoc, Options.builder().safe(SafeMode.UNSAFE).backend("html5").build());
    }
}
