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
package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The inline {@code k:} chip resolves typed ids ({@code k:sctid=…}, {@code k:uuid=…}, …) through
 * {@code koncepts.yml} exactly as the {@code koncept-tree} block does
 * ({@code IKE-Network/ike-issues#837}) — so a typed id in flowing prose renders an identicon chip,
 * not the plain text badge.
 */
class KonceptInlineTypedIdTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(KonceptInlineMacro.class)
                .postprocessor(KonceptGlossaryProcessor.class);
    }

    @AfterAll
    static void teardown() {
        if (asciidoctor != null) {
            asciidoctor.close();
        }
    }

    @Test
    void typedSctidRendersTheConceptChip_html5() {
        // 84114007 is HeartFailure in koncepts.yml — the sctid resolves through the reverse index.
        String html = convert("Patient with k:sctid=84114007[].", "html5");

        assertTrue(html.contains("koncept-identicon"), "a typed sctid renders the identicon chip");
        assertTrue(html.contains("href=\"#koncept-HeartFailure\""),
                "resolved to the concept and linked to its glossary entry, not #koncept-sctid=…");
        assertTrue(html.contains("Heart Failure"), "and shows the concept's name");
    }

    @Test
    void typedSctidAndNameKeyRenderTheSameIdenticon() {
        String expected = IdenticonRenderer.dataUri(
                KonceptIdentity.idString(List.of(SnomedUuids.fromSnomed("84114007"))));
        assertTrue(convert("k:sctid=84114007[].", "html5").contains(expected),
                "the typed sctid resolves to the concept's PublicId identicon");
        assertTrue(convert("k:HeartFailure[].", "html5").contains(expected),
                "identical to the name-key form — the two are interchangeable");
    }

    @Test
    void typedSctidRendersTheConceptChip_docbook() {
        String xml = convert("Patient with k:sctid=84114007[].", "docbook5");

        assertTrue(xml.contains("<inlinemediaobject>"), "a typed sctid renders the DocBook identicon");
        assertTrue(xml.contains("linkend=\"koncept-HeartFailure\""), "linked to the concept");
        assertTrue(xml.contains("<phrase role=\"koncept-label\">Heart Failure</phrase>"),
                "with the visible name (ike-issues#836 on the resolved concept)");
    }

    @Test
    void uncuratedUuidRendersAnIdenticonFromTheIdItself() {
        // A UUID absent from koncepts.yml still carries its own PublicId → identicon, no glossary link.
        String html = convert("An ad-hoc k:uuid=11111111-0000-4000-8000-000000000001[Ad hoc].", "html5");

        assertTrue(html.contains("src=\"data:image/png;base64,"),
                "the carried PublicId still draws an identicon");
        assertTrue(html.contains("Ad hoc"), "with the authoring label");
    }

    @Test
    void uncuratedMultiUuidLinkIsSlugged() {
        // The comma in an uncurated multi-UUID id must not reach the #koncept- link raw — it would
        // break the re-parsed Prawn image macro's attribute list (the ike-issues#836 class).
        String html = convert("A k:uuid=11111111-0000-4000-8000-000000000001,"
                + "22222222-0000-4000-8000-000000000002[Multi].", "html5");

        assertTrue(html.contains("href=\"#koncept-11111111-0000-4000-8000-000000000001-"
                        + "22222222-0000-4000-8000-000000000002\""),
                "the comma is slugged to a dash in the anchor");
        assertTrue(html.contains("Multi"), "the chip still renders with its label");
    }

    @Test
    void nidIsLiveOnly_fallsBackToTheBadge() {
        String html = convert("A live-only k:nid=-2147480000[Live node].", "html5");

        assertFalse(html.contains("src=\"data:image/png;base64,"),
                "a nid has no static identicon");
        assertTrue(html.contains("Live node"), "the authoring label still renders");
    }

    @Test
    void typedIdAndNameKeyDedupeInTheGlossary() {
        String html = convert("Both k:HeartFailure[] and k:sctid=84114007[] name one concept.", "html5");

        long entries = html.lines().filter(l -> l.contains("id=\"koncept-HeartFailure\"")).count();
        assertEquals(1, entries, "the two references share one glossary entry");
        assertTrue(html.contains("2 references"), "counted twice");
    }

    @Test
    void nameKeyStillResolves_regression() {
        String html = convert("Plain k:HeartFailure[].", "html5");
        assertTrue(html.contains("koncept-identicon") && html.contains("href=\"#koncept-HeartFailure\""),
                "the name-key path is unchanged by the shared resolver");
    }

    private String convert(String adoc, String backend) {
        Options options = Options.builder().safe(SafeMode.UNSAFE).backend(backend).build();
        return asciidoctor.convert(adoc, options);
    }
}
