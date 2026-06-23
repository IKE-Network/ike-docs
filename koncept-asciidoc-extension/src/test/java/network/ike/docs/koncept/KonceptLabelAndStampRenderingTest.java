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
 * {@code koncepts.yml} label rather than a camelCase split of the identifier, and (b) never renders a
 * STAMP as an identicon / name-pill in ANY backend (ike-issues#638). These exercise the full macro —
 * the gap the renderer unit tests masked by feeding the formatted string directly. The seeded
 * ExampleStamp carries a PublicId (uuids), so each assertion proves a stamp-WITH-an-id still avoids the
 * identicon path.
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
    void html5_stamp_showsSeededProvenanceLabelAndPentagon_notTheIdentifier() {
        String html = convert("A coordinate k:ExampleStamp[].", "html5");
        assertTrue(html.contains("Active · 2024-09-30 14:30 · KEC"),
                "the stamp shows its seeded koncepts.yml provenance label");
        assertTrue(html.contains("<polygon"), "the stamp shows the pentagon sigil");
        assertFalse(html.contains("Example Stamp"),
                "the stamp must NOT fall back to the camelCase split of the identifier");
        assertFalse(html.contains("koncept-identicon"),
                "a stamp is provenance, never an identicon — even with a PublicId");
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
    void docbook5_stamp_isAGrayPhrase_notAnInlineMediaObject() {
        String xml = convert("A coordinate k:ExampleStamp[].", "docbook5");
        assertFalse(xml.contains("<inlinemediaobject>"),
                "a stamp must not embed an identicon image in DocBook/FO");
        assertTrue(xml.contains("role=\"koncept-stamp\""),
                "a stamp renders the gray provenance phrase");
        assertTrue(xml.contains("Active · 2024-09-30 14:30 · KEC"),
                "the provenance text is shown");
    }

    @Test
    void prawn_stamp_embedsNoIdenticonImage() throws Exception {
        File out = Files.createTempFile("koncept-stamp", ".pdf").toFile();
        out.deleteOnExit();
        Options options = Options.builder().safe(SafeMode.UNSAFE).backend("pdf").toFile(out).build();
        asciidoctor.convert("A coordinate k:ExampleStamp[].", options);
        String body = new String(Files.readAllBytes(out.toPath()), StandardCharsets.ISO_8859_1);
        assertTrue(body.startsWith("%PDF"), "renders a PDF");
        assertFalse(body.contains("/Width 128") && body.contains("/Height 128"),
                "a stamp embeds no 128x128 identicon image XObject in Prawn");
    }

    private String convert(String adoc, String backend) {
        Options options = Options.builder().safe(SafeMode.UNSAFE).backend(backend).build();
        return asciidoctor.convert(adoc, options);
    }
}
