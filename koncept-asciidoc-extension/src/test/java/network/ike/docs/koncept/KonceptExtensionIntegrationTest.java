package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that exercises the full Koncept extension pipeline:
 * inline macro parsing → registry population → glossary generation.
 */
class KonceptExtensionIntegrationTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        // Register extensions explicitly (SPI also works but explicit is clearer in tests).
        // The treeprocessor is included because it now owns koncept-glossary-all for the
        // html5 family (IKE-Network/ike-issues#877); the postprocessor early-returns for
        // that combination, so comprehensiveGlossary_listsAllKnownKoncepts needs both
        // registered to see real production behavior.
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(KonceptInlineMacro.class)
                .treeprocessor(KonceptGlossaryTreeprocessor.class)
                .postprocessor(KonceptGlossaryProcessor.class);
    }

    @Test
    void singleKonceptReference_rendersIdenticon() {
        String adoc = "The patient has k:HeartFailure[].";
        String html = convert(adoc);

        assertTrue(html.contains("koncept-identicon"),
                "Should render the Komet identicon image");
        assertTrue(html.contains("src=\"data:image/png;base64,"),
                "Identicon should be embedded as a data URI PNG");
        assertTrue(html.contains("href=\"#koncept-HeartFailure\""),
                "Identicon should link to glossary anchor");
        assertTrue(html.contains("Heart Failure"),
                "Should split camelCase for display label");
        assertTrue(html.contains("koncept-chip"),
                "Reference should be wrapped in the self-contained soft chip");
        assertTrue(html.contains("font-variant:small-caps"),
                "Label should carry the small-caps inline style (no koncept.css needed)");
    }

    @Test
    void explicitLabel_overridesDefault() {
        String adoc = "Diagnosed with k:HeartFailure[CHF].";
        String html = convert(adoc);

        assertTrue(html.contains("CHF"), "Should use explicit label");
    }

    @Test
    void unknownKoncept_fallsBackToBadge() {
        String adoc = "The patient has k:UnknownCondition[].";
        String html = convert(adoc);

        // No definition → no PublicId → no identicon → SVG text badge.
        assertTrue(html.contains("koncept-badge"),
                "Koncept without a resolvable identicon should fall back to the SVG badge");
    }

    @Test
    void multipleReferences_deduplicateInGlossary() {
        String adoc = """
                First mention of k:HeartFailure[].
                Second mention of k:HeartFailure[].
                Also k:AorticStenosis[].
                """;
        String html = convert(adoc);

        // Count glossary entries (dt elements with koncept- id)
        long glossaryEntries = html.lines()
                .filter(line -> line.contains("id=\"koncept-"))
                .count();

        assertEquals(2, glossaryEntries,
                "Should have exactly 2 glossary entries despite 3 references");
        assertTrue(html.contains("2 references"),
                "HeartFailure should show reference count");
    }

    @Test
    void glossaryContainsDefinitionAndAxiom() {
        String adoc = "Patient presents with k:HeartFailure[].";
        String html = convert(adoc);

        assertTrue(html.contains("koncept-def-text"),
                "Glossary should contain definition text");
        assertTrue(html.contains("koncept-axiom"),
                "Glossary should contain DL axiom");
        assertTrue(html.contains("SCTID"),
                "Glossary should contain SNOMED CT identifier");
    }

    @Test
    void glossaryShowsIdenticon() {
        String adoc = "Patient presents with k:HeartFailure[].";
        String html = convert(adoc);

        assertTrue(html.contains("koncept-glossary-icon"),
                "Glossary entry should display the concept's identicon");
    }

    @Test
    void glossaryShowsParentsChildrenAndIdentifiers() {
        // DiabetesMellitus broader: [EndocrineDisorder, MetabolicDisorder]
        String html = convert("Patient with k:DiabetesMellitus[] and k:EndocrineDisorder[].");

        assertTrue(html.contains("koncept-taxonomy-parents"),
                "DiabetesMellitus entry should show a Parents line");
        assertTrue(html.contains("href=\"#koncept-EndocrineDisorder\""),
                "Parent should link to EndocrineDisorder's glossary entry");
        assertTrue(html.contains("koncept-taxonomy-children"),
                "EndocrineDisorder entry should show a Children line (inverted broader)");
        assertTrue(html.contains("href=\"#koncept-DiabetesMellitus\""),
                "Child link back to DiabetesMellitus");
        assertTrue(html.contains("koncept-identifier") && html.contains("UUID:") || html.contains("id: <code>"),
                "Entry should show identifiers (koncept id, UUID)");
    }

    @Test
    void comprehensiveGlossary_listsAllKnownKoncepts() {
        // koncept-glossary-all lists every definition, not only the one referenced.
        Options options = Options.builder().safe(SafeMode.UNSAFE).backend("html5")
                .attributes(org.asciidoctor.Attributes.builder()
                        .attribute("koncept-glossary-all", "").build())
                .build();
        String html = asciidoctor.convert("Only k:HeartFailure[] is referenced.", options);

        assertTrue(html.contains("id=\"koncept-DiabetesMellitus\""),
                "Comprehensive glossary should include an unreferenced koncept");
        assertTrue(html.contains("id=\"koncept-HeartFailure\""),
                "and the referenced one too");
    }

    @Test
    void unknownKoncept_showsMissingDefinition() {
        String adoc = "The patient has k:UnknownCondition[].";
        String html = convert(adoc);

        assertTrue(html.contains("koncept-def-missing"),
                "Unknown koncept should show missing definition indicator");
    }

    @Test
    void noKoncepts_noGlossary() {
        String adoc = "A document with no koncept references.";
        String html = convert(adoc);

        assertFalse(html.contains("Referenced Koncepts"),
                "Should not generate glossary when no koncepts referenced");
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
