package network.ike.docs.koncept;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Byte-for-byte parity guard against Komet's identicon. The golden is the
 * SHA-256 of the native 32&times;32 RGB pixels LifeHash VERSION2 produces for
 * the HeartFailure idString — the exact pixels Komet renders. Reading it back
 * through the real {@link IdenticonRenderer#png} PNG path proves the rendered
 * image is unchanged; a {@code toucan} version bump that altered the algorithm
 * would fail this test.
 */
class IdenticonRendererTest {

    private static final String HEART_FAILURE_ID =
            "[\"f05fae71-345a-5f4b-9a3c-4588409fa692\"]";
    private static final String GOLDEN_RGB_SHA256 =
            "166b0451d285963b5f925f071aa5ee6c8d09095a8b43e947e4044a95e48f3172";

    @Test
    void png_nativePixels_matchKometGolden() throws Exception {
        byte[] png = IdenticonRenderer.png(HEART_FAILURE_ID, 1);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(32, img.getWidth());
        assertEquals(32, img.getHeight());
        assertEquals(GOLDEN_RGB_SHA256, sha256Rgb(img));
    }

    @Test
    void displayModuleSize_yields128Square() throws Exception {
        byte[] png = IdenticonRenderer.png(HEART_FAILURE_ID, IdenticonRenderer.DISPLAY_MODULE_SIZE);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(128, img.getWidth());
        assertEquals(128, img.getHeight());
    }

    @Test
    void dataUri_isPngBase64() {
        assertTrue(IdenticonRenderer.dataUri(HEART_FAILURE_ID)
                .startsWith("data:image/png;base64,"));
    }

    private static String sha256Rgb(BufferedImage img) throws Exception {
        int w = img.getWidth();
        int h = img.getHeight();
        byte[] rgb = new byte[w * h * 3];
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                rgb[i++] = (byte) ((argb >> 16) & 0xff);
                rgb[i++] = (byte) ((argb >> 8) & 0xff);
                rgb[i++] = (byte) (argb & 0xff);
            }
        }
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(rgb);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
