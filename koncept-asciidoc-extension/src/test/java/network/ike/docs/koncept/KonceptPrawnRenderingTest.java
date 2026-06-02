package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
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
 * Verifies the Prawn (asciidoctorj-pdf) backend embeds the identicon as a real
 * inline image node. Compares a PDF rendered with the identicon against one
 * with {@code :koncept-identicon: false} (text fallback): the embedded PNG
 * adds an image XObject and materially enlarges the PDF.
 */
class KonceptPrawnRenderingTest {

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
    void prawn_embedsIdenticonImage() throws Exception {
        byte[] withIcon = toPdf("The patient has k:HeartFailure[].", true);
        byte[] textOnly = toPdf("The patient has k:HeartFailure[].", false);

        assertTrue(isPdf(withIcon), "identicon render should be a PDF");
        assertTrue(isPdf(textOnly), "fallback render should be a PDF");

        // The identicon embeds as a 128x128 image XObject (DISPLAY_MODULE_SIZE
        // = 4 → 32×4). The text fallback embeds no image.
        String iconBody = new String(withIcon, StandardCharsets.ISO_8859_1);
        String textBody = new String(textOnly, StandardCharsets.ISO_8859_1);
        assertTrue(iconBody.contains("/Width 128") && iconBody.contains("/Height 128"),
                "Prawn PDF should embed the 128x128 identicon image XObject");
        assertFalse(textBody.contains("/Width 128"),
                "text-fallback PDF should embed no identicon image");
    }

    private byte[] toPdf(String adoc, boolean identicon) throws Exception {
        File out = Files.createTempFile("koncept-prawn", ".pdf").toFile();
        out.deleteOnExit();
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("pdf")
                .toFile(out)
                .attributes(Attributes.builder()
                        .attribute("koncept-identicon", identicon ? "true" : "false")
                        .build())
                .build();
        asciidoctor.convert(adoc, options);
        return Files.readAllBytes(out.toPath());
    }

    private static boolean isPdf(byte[] b) {
        return b.length > 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
    }
}
