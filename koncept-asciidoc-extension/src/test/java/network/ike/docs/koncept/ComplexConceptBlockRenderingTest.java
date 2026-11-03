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

import network.ike.docs.koncept.ComplexConceptSource.Focused;
import network.ike.docs.koncept.ComplexConceptSource.Parsed;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The site inspection decision point of {@code rda-site-inspection-tree} as a block: read
 * store-free, drawn as the document form of the complex concept editor, and named by the tree
 * that decides it.
 */
class ComplexConceptBlockRenderingTest {

    static final String SITE = """
            concept: k:SiteInspectionDecision[Site inspection decision for a first-of-a-kind submission] @id=bimo-site-decision
              expansion: focused k:InspectionDecision[Clinical site inspection decision] @id=bimo-inspection-decision
                prerequisite: k:FirstOfAKind[First-of-a-kind device] @id=bimo-first-of-a-kind
                  value = [1,1] k:Presence[presence] @id=bimo-presence
                prerequisite: k:ConductConcern[Concern about the conduct of the study] @id=bimo-conduct-concern
                  value = [1,1] k:Presence[] @id=bimo-presence
                prerequisite: k:PriorSubmissions[Marketing submissions the applicant has made before] @id=bimo-prior-submissions
                  value = [0,2] k:Count[count] @id=bimo-count
                prerequisite: k:Symptoms[Symptoms] @id=268935007
                  value = [0,0] k:Presence[] @id=bimo-presence
            """;

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(ComplexConceptBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @Test
    @DisplayName("The block reads store-free: the focus, four prerequisites, a label carried to k:Key[]")
    void reads() {
        Parsed parsed = ComplexConceptSource.parse(List.of(SITE.split("\n")));
        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        assertNotNull(parsed.spec());
        Focused focused = (Focused) parsed.spec().shape();
        assertEquals(4, focused.prerequisites().size());
        assertEquals("presence", focused.prerequisites().get(1).unit().shown(), "k:Presence[] names what the first mention named");
        assertEquals("present", ComplexConceptSource.valueWords(focused.prerequisites().get(0)));
        assertEquals("0 to 2", ComplexConceptSource.valueWords(focused.prerequisites().get(2)));
        assertEquals("absent", ComplexConceptSource.valueWords(focused.prerequisites().get(3)));
        assertEquals("Clinical site inspection decision", parsed.spec().definitionParent().orElseThrow().shown(),
                "the parent defaults to the focus");
        assertTrue(parsed.localSymbols().contains("bimo-site-decision"));
        assertFalse(parsed.localSymbols().contains("268935007"), "a SNOMED CT identifier is not local");
        assertTrue(ComplexConceptSource.completeness(parsed.spec()).isEmpty());
    }

    @Test
    @DisplayName("The block renders the concept's sentence and one statement row per prerequisite")
    void renders() {
        String html = convert("""
                [complex-concept]
                ----
                %s----
                """.formatted(SITE));
        assertTrue(html.contains("class=\"openblock complex-concept\""), html);
        assertTrue(html.contains("<strong>Complex concept</strong>"), html);
        assertTrue(html.contains("<em class=\"koncept-local\">Site inspection decision for a first-of-a-kind submission</em>"), html);
        assertTrue(html.contains("each an ANF performance statement"), html);
        assertTrue(html.contains("<th class=\"tableblock halign-left valign-top\">Statement topic</th>"), html);
        String body = html.substring(html.indexOf("<tbody>"), html.indexOf("</tbody>"));
        assertEquals(4, count(body, "<tr>"), () -> "a row per prerequisite under the header: " + body);
        assertTrue(html.contains("[0, 2]"), "the value as written");
        assertTrue(html.contains(">absent<"), "the result in words");
        assertTrue(html.contains("koncept-chip") || html.contains("koncept"), "a SNOMED CT topic is a Koncept chip");
        assertTrue(html.contains("A name in italics is a concept local to the document"), html);
        assertFalse(html.contains("still needs"), "a complete specification says nothing of what it lacks");
    }

    @Test
    @DisplayName("A tree whose decision names the concept says which decision point it decides")
    void treeNamesItsDecisionPoint() {
        String html = convert("""
                [complex-concept]
                ----
                %s----

                [decision-tree, cohort=100]
                ----
                decision: k:SiteInspectionDecision[Inspect the site?] @id=bimo-site-decision
                  alternative: k:Inspect[inspect] @id=bimo-inspect
                    money = [60000,60000] k:Usd[US dollar] @id=bimo-usd
                    outcome: k:Done[done] @id=bimo-done
                  alternative: k:NoInspection[do not inspect] @id=bimo-no-inspection
                    money = [70000,70000] k:Usd[US dollar] @id=bimo-usd
                    outcome: k:Done[done] @id=bimo-done-2
                ----
                """.formatted(SITE));
        assertTrue(html.contains("The decision point is the complex concept"), html);
        assertTrue(html.contains("a kind of clinical site inspection decision, given first-of-a-kind device present,"), html);
    }

    @Test
    @DisplayName("A block with no readable root is shown as written; an incomplete one says what it lacks")
    void literalAndIncomplete() {
        String literal = convert("""
                [complex-concept]
                ----
                something else
                ----
                """);
        assertTrue(literal.contains("class=\"literalblock\""), literal);
        String incomplete = convert("""
                [complex-concept]
                ----
                concept: k:A[A thing] @id=a-thing
                  expansion: focused k:B[B] @id=b
                    prerequisite: k:C[C] @id=c
                ----
                """);
        assertTrue(incomplete.contains("The specification still needs a fixed value for C."), incomplete);
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + 1)) {
            n++;
        }
        return n;
    }

    private static String convert(String adoc) {
        return asciidoctor.convert(adoc, Options.builder().safe(SafeMode.UNSAFE).backend("html5").build());
    }
}
