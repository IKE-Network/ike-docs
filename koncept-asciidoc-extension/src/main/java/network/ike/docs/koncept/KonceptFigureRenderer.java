package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptKind;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
 * Composes figure-grade PNGs for the guide's front matter from the same primitives the
 * badges render with — the {@link IdenticonRenderer} LifeHash image and the
 * {@link KonceptKind} sigil glyph/colour data — so an illustration in prose can never
 * drift from what a real badge draws. Two figures are supported:
 * <ul>
 *   <li>{@link #anatomyPng(KonceptKind, String, String)} — a badge dissected: the kind
 *       sigil, the identicon, and the name, each with a muted part label beneath.</li>
 *   <li>the bare identicon itself, via {@link IdenticonRenderer} directly.</li>
 * </ul>
 * All output is drawn at 2&times; and displayed at half width for crispness. Files are
 * content-addressed in the same temp directory scheme {@link IdenticonRenderer#pngFile}
 * uses, so re-runs are stable and every backend (data URI on html, absolute file on
 * FO/Prawn) has a form it can consume.
 */
final class KonceptFigureRenderer {

    private KonceptFigureRenderer() {
    }

    /** Draw scale — everything is drawn at 2&times; and displayed at half width. */
    private static final int SCALE = 2;

    /** Chip background, matching the badge chip. */
    private static final Color CHIP_COLOR = new Color(0xe9, 0xef, 0xf6);

    /** Badge label blue, matching the badge chip. */
    private static final Color LABEL_COLOR = new Color(0x2a, 0x5a, 0x8a);

    /** Muted annotation gray, matching the guide's muted rows. */
    private static final Color ANNOTATION_COLOR = new Color(0x6a, 0x73, 0x7d);

    private static final Map<String, String> FILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> DATA_URI_CACHE = new ConcurrentHashMap<>();

    /**
     * Composes the badge-anatomy figure: the kind sigil (when the kind has one), the
     * identicon, and the name laid out as a real badge chip, with a connector and a
     * muted part label under each part.
     *
     * @param kind     the component kind whose sigil leads the badge
     * @param idString the Tinkar idString driving the identicon
     * @param label    the badge's display name
     * @return the PNG bytes
     * @throws UncheckedIOException if PNG encoding fails
     */
    static byte[] anatomyPng(KonceptKind kind, String idString, String label) {
        int pad = 12 * SCALE;
        int chipPad = 8 * SCALE;
        int identiconSize = 32 * SCALE;
        Font sigilFont = new Font(Font.SANS_SERIF, Font.BOLD, 15 * SCALE);
        Font nameFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12 * SCALE);
        Font partFont = new Font(Font.SANS_SERIF, Font.PLAIN, 9 * SCALE);

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        FontMetrics sigilFm = pg.getFontMetrics(sigilFont);
        FontMetrics nameFm = pg.getFontMetrics(nameFont);
        FontMetrics partFm = pg.getFontMetrics(partFont);
        String name = label.toUpperCase(java.util.Locale.ROOT);
        boolean hasSigil = kind.hasLetterGlyph();
        int sigilW = hasSigil ? sigilFm.stringWidth(kind.glyph()) : 0;
        int nameW = nameFm.stringWidth(name);
        pg.dispose();

        int gap = 8 * SCALE;
        int chipH = identiconSize + 2 * chipPad;
        int chipW = chipPad + (hasSigil ? sigilW + gap : 0) + identiconSize + gap + nameW + chipPad;
        int partH = partFm.getHeight() + 14 * SCALE;
        int width = chipW + 2 * pad;
        int height = pad + chipH + partH + pad;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int chipX = pad;
        int chipY = pad;
        g.setColor(CHIP_COLOR);
        g.fillRoundRect(chipX, chipY, chipW, chipH, 10 * SCALE, 10 * SCALE);

        int centerY = chipY + chipH / 2;
        int x = chipX + chipPad;
        int sigilCenter = 0;
        if (hasSigil) {
            g.setFont(sigilFont);
            g.setColor(Color.decode(kind.colorHex()));
            int baseline = centerY + (sigilFm.getAscent() - sigilFm.getDescent()) / 2;
            g.drawString(kind.glyph(), x, baseline);
            sigilCenter = x + sigilW / 2;
            x += sigilW + gap;
        }

        int identiconX = x;
        try {
            BufferedImage identicon = ImageIO.read(new ByteArrayInputStream(
                    IdenticonRenderer.png(idString, IdenticonRenderer.DISPLAY_MODULE_SIZE)));
            g.drawImage(identicon, identiconX, centerY - identiconSize / 2,
                    identiconSize, identiconSize, null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read identicon PNG for " + idString, e);
        }
        int identiconCenter = identiconX + identiconSize / 2;
        x += identiconSize + gap;

        g.setFont(nameFont);
        g.setColor(LABEL_COLOR);
        int nameBaseline = centerY + (nameFm.getAscent() - nameFm.getDescent()) / 2;
        g.drawString(name, x, nameBaseline);
        int nameCenter = x + nameW / 2;

        // Connectors and muted part labels beneath the chip, placed left to right so
        // neighbouring labels can never overlap even when their parts sit close together.
        g.setFont(partFont);
        g.setStroke(new BasicStroke(SCALE));
        g.setColor(ANNOTATION_COLOR);
        int lineTop = chipY + chipH + 2 * SCALE;
        int lineBottom = lineTop + 6 * SCALE;
        int partBaseline = lineBottom + partFm.getAscent() + SCALE;
        int[] centers = hasSigil
                ? new int[] {sigilCenter, identiconCenter, nameCenter}
                : new int[] {identiconCenter, nameCenter};
        String[] texts = hasSigil
                ? new String[] {"kind sigil", "identicon", "name"}
                : new String[] {"identicon", "name"};
        int cursor = 2;
        for (int i = 0; i < centers.length; i++) {
            int textW = partFm.stringWidth(texts[i]);
            int textX = Math.min(Math.max(cursor, centers[i] - textW / 2), width - textW - 2);
            g.drawLine(centers[i], lineTop, centers[i], lineBottom);
            g.drawString(texts[i], textX, partBaseline);
            cursor = textX + textW + 6 * SCALE;
        }
        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode anatomy PNG for " + label, e);
        }
    }

    /**
     * The anatomy figure as a content-addressed PNG file, for file-referencing backends.
     *
     * @param kind     the component kind
     * @param idString the Tinkar idString driving the identicon
     * @param label    the badge's display name
     * @return the absolute path of the written PNG
     * @throws UncheckedIOException if the file cannot be written
     */
    static String anatomyPngFile(KonceptKind kind, String idString, String label) {
        String key = kind.name() + '|' + idString + '|' + label;
        return FILE_CACHE.computeIfAbsent(key, k -> {
            try {
                Path dir = Path.of(System.getProperty("java.io.tmpdir"), "ike-koncept-identicons");
                Files.createDirectories(dir);
                // Content-addressed by the PNG BYTES, not the key: the composition depends
                // on this class's layout code, so a code change must mint a new file rather
                // than resurrect a stale one from an earlier build.
                byte[] png = anatomyPng(kind, idString, label);
                Path file = dir.resolve("anatomy-" + shortHash(Base64.getEncoder()
                        .encodeToString(png)) + ".png");
                if (!Files.exists(file)) {
                    Files.write(file, png);
                }
                return file.toAbsolutePath().toString();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write anatomy PNG for " + label, e);
            }
        });
    }

    /**
     * The anatomy figure as a base64 {@code data:} URI, for inline-capable backends.
     *
     * @param kind     the component kind
     * @param idString the Tinkar idString driving the identicon
     * @param label    the badge's display name
     * @return a {@code data:image/png;base64,…} URI
     */
    static String anatomyDataUri(KonceptKind kind, String idString, String label) {
        String key = kind.name() + '|' + idString + '|' + label;
        return DATA_URI_CACHE.computeIfAbsent(key, k -> "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(anatomyPng(kind, idString, label)));
    }

    /**
     * A short hex hash for content-addressed filenames.
     *
     * @param s the input string
     * @return the first 16 hex chars of its SHA-256
     */
    private static String shortHash(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
