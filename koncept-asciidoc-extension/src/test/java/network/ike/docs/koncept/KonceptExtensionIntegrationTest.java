package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
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
        // Register extensions explicitly (SPI also works but explicit is clearer in tests)
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(KonceptInlineMacro.class)
                .postprocessor(KonceptGlossaryProcessor.class);
    }

    @Test
    void singleKonceptReference_rendersInlineBadge() {
        String adoc = "The patient has k:HeartFailure[].";
        String html = convert(adoc);

        assertTrue(html.contains("koncept-badge"), "Should contain SVG badge");
        assertTrue(html.contains("href=\"#koncept-HeartFailure\""),
                "Badge should link to glossary anchor");
        assertTrue(html.contains("Heart Failure"),
                "Should split camelCase for display label");
    }

    @Test
    void explicitLabel_overridesDefault() {
        String adoc = "Diagnosed with k:HeartFailure[CHF].";
        String html = convert(adoc);

        assertTrue(html.contains("CHF"), "Should use explicit label");
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
