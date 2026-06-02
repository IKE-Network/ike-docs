package network.ike.docs.koncept;

import com.sparrowwallet.toucan.LifeHash;
import com.sparrowwallet.toucan.LifeHashVersion;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

/**
 * Renders the Komet identicon for a koncept as a PNG.
 * <p>
 * The image is byte-identical to the one Komet draws
 * ({@code dev.ikm.komet.framework.Identicon}) because it uses the same
 * library ({@code com.sparrowwallet:toucan}), the same algorithm version
 * ({@link LifeHashVersion#VERSION2}), and the same input string (the
 * Tinkar {@code idString} produced by {@link KonceptIdentity}). LifeHash is
 * a deterministic visual hash: the SHA-256 of the UTF-8 input drives a
 * cellular-automaton render whose pattern depends only on the input and the
 * version — {@code moduleSize} merely scales pixel density (nearest
 * neighbour), never the pattern.
 * <p>
 * VERSION2 produces a 32&times;32 native grid; at {@link #DISPLAY_MODULE_SIZE}
 * that is the 128&times;128 Komet renders at. Two output forms are provided:
 * <ul>
 *   <li>{@link #dataUri} — a base64 {@code data:} URI for inline HTML
 *       ({@code html5} and CSS-PDF backends).</li>
 *   <li>{@link #pngFile} — a PNG written to a content-addressed file, for
 *       backends that need a file reference (DocBook&rarr;FO, Prawn PDF).</li>
 * </ul>
 * Both are memoized by {@code idString}.
 */
public final class IdenticonRenderer {

    private IdenticonRenderer() {
    }

    /**
     * Module size for displayed identicons. VERSION2's 32&times;32 native
     * grid &times; 4 = 128&times;128, matching Komet's displayed size.
     */
    public static final int DISPLAY_MODULE_SIZE = 4;

    private static final Map<String, String> DATA_URI_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> FILE_CACHE = new ConcurrentHashMap<>();

    /** Content-addressed output directory for file-based identicons. */
    private static final Path FILE_DIR =
            Path.of(System.getProperty("java.io.tmpdir"), "ike-koncept-identicons");

    /**
     * Renders the identicon for an {@code idString} as PNG bytes.
     *
     * @param idString   the Tinkar {@code idString} (LifeHash input)
     * @param moduleSize the per-cell pixel size (must be &gt; 0); the output
     *                   is {@code 32 * moduleSize} square for VERSION2
     * @return the PNG-encoded identicon
     * @throws UncheckedIOException if PNG encoding fails
     */
    public static byte[] png(String idString, int moduleSize) {
        LifeHash.Image image =
                LifeHash.makeFromUTF8(idString, LifeHashVersion.VERSION2, moduleSize, false);
        BufferedImage buffered = LifeHash.getBufferedImage(image);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(buffered, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode identicon PNG for " + idString, e);
        }
    }

    /**
     * Renders the identicon for an {@code idString} as a base64
     * {@code data:image/png} URI suitable for an inline HTML {@code <img src>}.
     * Memoized per {@code idString}.
     *
     * @param idString the Tinkar {@code idString} (LifeHash input)
     * @return a {@code data:image/png;base64,…} URI
     * @throws UncheckedIOException if PNG encoding fails
     */
    public static String dataUri(String idString) {
        return DATA_URI_CACHE.computeIfAbsent(idString, id ->
                "data:image/png;base64,"
                        + Base64.getEncoder().encodeToString(png(id, DISPLAY_MODULE_SIZE)));
    }

    /**
     * Writes the identicon for an {@code idString} to a content-addressed PNG
     * file and returns its absolute path. Backends that cannot embed a
     * {@code data:} URI (DocBook&rarr;FO via FOP/XEP, Prawn PDF) reference the
     * file directly; with {@code SafeMode.UNSAFE} (used by the doc pipeline)
     * absolute image paths resolve in all of them. The filename is derived
     * from a hash of the {@code idString}, so identical concepts share one
     * file and re-runs are stable. Memoized per {@code idString}.
     *
     * @param idString the Tinkar {@code idString} (LifeHash input)
     * @return the absolute filesystem path of the written PNG
     * @throws UncheckedIOException if the file cannot be written
     */
    public static String pngFile(String idString) {
        return FILE_CACHE.computeIfAbsent(idString, id -> {
            try {
                Files.createDirectories(FILE_DIR);
                Path file = FILE_DIR.resolve("identicon-" + shortHash(id) + ".png");
                if (!Files.exists(file)) {
                    Files.write(file, png(id, DISPLAY_MODULE_SIZE));
                }
                return file.toAbsolutePath().toString();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write identicon PNG for " + id, e);
            }
        });
    }

    /**
     * A short hex hash of the input, for content-addressed filenames.
     *
     * @param s the input string
     * @return the first 16 hex chars of its SHA-256
     */
    private static String shortHash(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", h[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
