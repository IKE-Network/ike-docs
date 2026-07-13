package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@code koncept-narrative::Identifier[]} block macro
 * (IKE-Network/ike-issues#879): a koncept's curated {@code narrative} field is
 * spliced into the document and re-parsed as real AsciiDoc, so an embedded
 * {@code k:} chip resolves exactly as it would if hand-typed in place.
 */
class KonceptNarrativeBlockMacroTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
        asciidoctor.javaExtensionRegistry().blockMacro(KonceptNarrativeBlockMacro.class);
    }

    @Test
    void embeddedChipResolvesAfterReparsing() {
        String html = convert("koncept-narrative::WithNarrative[]");

        assertTrue(html.contains("curated prose"), "The narrative's own text renders");
        assertTrue(html.contains("href=\"#koncept-OtherKoncept\""),
                "The embedded k:OtherKoncept[] chip resolves to a real glossary cross-reference");
        assertTrue(html.contains("Other Koncept"), "…carrying the referenced koncept's label");
        assertFalse(html.contains("k:OtherKoncept[]"),
                "The raw k: token text must not survive un-parsed");
    }

    @Test
    void multiParagraphNarrative_preservesParagraphBreaks() {
        String html = convert("koncept-narrative::WithNarrative[]");

        assertTrue(html.contains("second paragraph"), "The second paragraph's text renders");
        long paragraphCount = html.lines().filter(line -> line.contains("<p>")).count();
        assertTrue(paragraphCount >= 2, "The blank line between paragraphs produces two <p> blocks");
    }

    @Test
    void missingNarrativeField_rendersPlaceholderNotBlank() {
        String html = convert("koncept-narrative::NoNarrative[]");

        assertTrue(html.contains("No curated narrative"),
                "A koncept with no narrative field renders a visible placeholder");
    }

    @Test
    void unknownIdentifier_rendersPlaceholderNotError() {
        String html = convert("koncept-narrative::NoSuchKoncept[]");

        assertTrue(html.contains("No curated narrative"),
                "An unresolvable identifier degrades to the same placeholder, not a crash");
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(org.asciidoctor.Attributes.builder()
                        .attribute("koncept-definitions-classpath", "/koncept-narrative-test.yml").build())
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
