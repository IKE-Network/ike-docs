package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the DocBook5 backend (input to the FOP/XEP XSL-FO pipeline) emits
 * an inline media object referencing a generated identicon PNG, and that the
 * PNG file is actually written to disk.
 */
class KonceptDocbookRenderingTest {

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
    void docbook_identicon_emitsInlineMediaObjectAndWritesPng() {
        String xml = convert("The patient has k:HeartFailure[].");

        assertTrue(xml.contains("<inlinemediaobject>"),
                "DocBook should embed an inline media object");
        assertTrue(xml.contains("format=\"PNG\""),
                "Image data should be a PNG");
        assertTrue(xml.contains("linkend=\"koncept-HeartFailure\""),
                "Identicon should link to the glossary entry");

        Matcher m = Pattern.compile("fileref=\"(file:[^\"]+)\"").matcher(xml);
        assertTrue(m.find(), "imagedata should carry a file: reference");
        Path png = Path.of(URI.create(m.group(1)));
        assertTrue(Files.exists(png), "identicon PNG should be written: " + png);
    }

    @Test
    void docbook_unknownKoncept_fallsBackToPhrase() {
        String xml = convert("The patient has k:UnknownCondition[].");
        assertTrue(xml.contains("role=\"koncept\""),
                "A koncept without an identicon should fall back to the phrase badge");
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("docbook5")
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
