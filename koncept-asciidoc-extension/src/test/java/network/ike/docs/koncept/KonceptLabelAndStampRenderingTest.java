package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end (real AsciidoctorJ) checks that the inline macro (a) resolves a koncept's declared
 * {@code koncepts.yml} label rather than a camelCase split of the identifier, and (b) renders a
 * STAMP provenance-shaped in every backend: the pentagon sigil precedes the STAMP's own identicon —
 * a sigil is never bare, and the identicon tells one STAMP from another at a glance — with the
 * compact provenance text in place of a name, never a small-caps name-pill. These exercise the full
 * macro — the gap the renderer unit tests masked by feeding the formatted string directly. The
 * seeded ExampleStamp carries a PublicId (uuids), so each assertion proves the identicon-bearing
 * stamp path.
 */
class KonceptLabelAndStampRenderingTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @AfterAll
    static void teardown() {
        if (asciidoctor != null) {
            asciidoctor.close();
        }
    }

    @Test
    void html5_stamp_showsPentagonThenIdenticonThenProvenance_notTheIdentifier() {
        String html = convert("A coordinate k:ExampleStamp[].", "html5");
        assertTrue(html.contains("Active · 2024-09-30 14:30 · KEC"),
                "the stamp shows its seeded koncepts.yml provenance label");
        assertTrue(html.contains("<polygon"), "the stamp shows the pentagon sigil");
        assertTrue(html.contains("koncept-identicon"),
                "the sigil precedes the STAMP's own identicon — never bare; the identicon "
                        + "tells one STAMP from another at a glance");
        assertTrue(html.indexOf("<polygon") < html.indexOf("koncept-identicon"),
                "…and the pentagon comes first");
        assertFalse(html.contains("Example Stamp"),
                "the stamp must NOT fall back to the camelCase split of the identifier");
        assertFalse(html.contains("font-variant:small-caps"),
                "provenance text stands in place of a name — no small-caps name-pill");
    }

    @Test
    void html5_description_showsSeededLabel_notTheIdentifierSplit() {
        String html = convert("The k:HeartFailurePreferredName[].", "html5");
        assertTrue(html.contains("Heart Failure (en)"),
                "the description shows its seeded label, not 'Heart Failure Preferred Name'");
        assertFalse(html.contains("Heart Failure Preferred Name"),
                "the camelCase split of the identifier must not leak through");
        assertTrue(html.contains("#b8860b"), "the amber D sigil colour is present");
    }

    @Test
    void docbook5_stamp_embedsTheIdenticonWithThePentagonGlyphPrefix() {
        String xml = convert("A coordinate k:ExampleStamp[].", "docbook5");
        assertTrue(xml.contains("<inlinemediaobject>"),
                "a stamp with a PublicId embeds its identicon in DocBook/FO too");
        assertTrue(xml.contains("⬠"),
                "…with the pentagon glyph preceding it — a sigil is never bare");
        assertTrue(xml.contains("Active · 2024-09-30 14:30 · KEC"),
                "the provenance text is shown");
    }

    @Test
    void prawn_stamp_embedsTheIdenticonImage() throws Exception {
        File out = Files.createTempFile("koncept-stamp", ".pdf").toFile();
        out.deleteOnExit();
        Options options = Options.builder().safe(SafeMode.UNSAFE).backend("pdf").toFile(out).build();
        asciidoctor.convert("A coordinate k:ExampleStamp[].", options);
        String body = new String(Files.readAllBytes(out.toPath()), StandardCharsets.ISO_8859_1);
        assertTrue(body.startsWith("%PDF"), "renders a PDF");
        assertTrue(body.contains("/Width 128") && body.contains("/Height 128"),
                "a stamp with a PublicId embeds its 128x128 identicon image in Prawn too");
    }

    private String convert(String adoc, String backend) {
        Options options = Options.builder().safe(SafeMode.UNSAFE).backend(backend).build();
        return asciidoctor.convert(adoc, options);
    }
}
