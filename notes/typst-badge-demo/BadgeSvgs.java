import com.sparrowwallet.toucan.LifeHash;
import com.sparrowwallet.toucan.LifeHashVersion;
import network.ike.docs.koncept.IdenticonRenderer;
import network.ike.docs.koncept.KonceptSvgRenderer;
import network.ike.docs.koncept.SnomedUuids;
import network.ike.docs.konceptcore.KonceptAppearance;
import network.ike.docs.konceptcore.KonceptKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Emits the demo's badge assets by calling the renderers that ship in
 * koncept-asciidoc-extension today (ike-issues#1307 Typst print target).
 *
 * <p>Per badge it writes, into {@code badges/}:
 * <ul>
 *   <li>{@code <name>.text.svg} — exactly what {@link KonceptSvgRenderer#render}
 *       emits (the no-identicon fallback / stamp sigil), minus the HTML {@code <a>}
 *       wrapper, which is not SVG and which Typst would reject.</li>
 *   <li>{@code <name>.identicon.png} — {@link IdenticonRenderer#png} at Komet's
 *       display module size (128×128), byte-identical to Komet's identicon.</li>
 *   <li>{@code <name>.chip.svg} — the identicon-bearing chip as one SVG: the pill,
 *       the identicon PNG embedded as a data URI, and the small-caps label — the
 *       SVG twin of {@code KonceptInlineMacro#renderHtmlIdenticon}.</li>
 *   <li>{@code <name>.cells.json} — the identicon's 32×32 cell colours, for the
 *       Typst-native rect-grid drawing.</li>
 * </ul>
 *
 * Run from the demo directory (after javac'ing koncept-core + the three renderer
 * classes into {@code CLASSES}):
 * <pre>
 *   java --enable-preview -cp CLASSES:toucan-0.9.0.jar BadgeSvgs.java badges
 * </pre>
 */
public final class BadgeSvgs {

    private static final KonceptAppearance SPEC = KonceptAppearance.defaults();

    record Badge(String name, String label, KonceptKind kind, String idString) {}

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "badges");
        Files.createDirectories(out);

        List<Badge> badges = List.of(
                new Badge("heart-failure", "Heart Failure", KonceptKind.CONCEPT,
                        snomed("84114007")),
                new Badge("mmhg", "mmHg", KonceptKind.CONCEPT, snomed("259018001")),
                new Badge("mild-npdr", "Mild Non-Proliferative Diabetic Retinopathy",
                        KonceptKind.CONCEPT, snomed("312903003")),
                // Tinkar's "Description pattern" — an IKE pattern.
                new Badge("description-pattern", "Description pattern", KonceptKind.PATTERN,
                        idString(List.of(UUID.fromString("a4de0039-2625-5842-8a4c-d1ce6aebf0ab")))),
                new Badge("heart-failure-en", "Heart failure (en)", KonceptKind.DESCRIPTION,
                        snomed("84114007")),
                new Badge("example-stamp", "Active · 2024-09-30 14:30 · KEC", KonceptKind.STAMP,
                        idString(List.of(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001")))));

        StringBuilder manifest = new StringBuilder("[\n");
        for (Badge b : badges) {
            // 1. The existing renderer's SVG, verbatim (anchor stripped).
            String rendered = KonceptSvgRenderer.render(b.name(), b.label(), b.kind());
            String svg = rendered.substring(rendered.indexOf("<svg"), rendered.lastIndexOf("</svg>") + 6);
            write(out.resolve(b.name() + ".text.svg"), svg);

            // 2. The identicon PNG, as Komet draws it.
            byte[] png = IdenticonRenderer.png(b.idString(), IdenticonRenderer.DISPLAY_MODULE_SIZE);
            Files.write(out.resolve(b.name() + ".identicon.png"), png);

            // 3. The identicon-bearing chip as a single SVG.
            write(out.resolve(b.name() + ".chip.svg"), chipSvg(b, png));

            // 4. The identicon's native 32×32 cell colours.
            write(out.resolve(b.name() + ".cells.json"), cellsJson(b.idString()));

            manifest.append(String.format(Locale.ROOT,
                    "  {\"name\": \"%s\", \"label\": \"%s\", \"kind\": \"%s\", \"glyph\": %s, \"glyphColor\": %s, \"idString\": %s},\n",
                    b.name(), json(b.label()), b.kind().name(),
                    b.kind().glyph() == null ? "null" : "\"" + b.kind().glyph() + "\"",
                    b.kind().colorHex() == null ? "null" : "\"" + b.kind().colorHex() + "\"",
                    "\"" + json(b.idString()) + "\""));
        }
        manifest.setLength(manifest.length() - 2);
        manifest.append("\n]\n");
        write(out.resolve("badges.json"), manifest.toString());
        System.out.println("wrote " + badges.size() + " badges to " + out.toAbsolutePath());
    }

    /** The chip SVG: pill + optional kind sigil + identicon (data URI) + small-caps label. */
    static String chipSvg(Badge b, byte[] png) {
        double scale = 1.0; // reference px, as the spec gives them
        double h = SPEC.padTopPx() + SPEC.identiconSizePx() + SPEC.padBottomPx() + 4; // 20 px tall
        double fontSize = SPEC.labelSizePx();
        double charW = fontSize * 0.55; // Alegreya Sans SC Medium average advance, small caps
        double sigilW = b.kind().hasLetterGlyph() ? fontSize * 0.75 : 0;
        double pentW = b.kind().isStamp() ? 16 : 0;
        double iconX = SPEC.padLeftPx() + sigilW + pentW;
        double labelX = iconX + SPEC.identiconSizePx() + SPEC.iconLabelGapPx();
        double w = Math.ceil(labelX + b.label().length() * charW + SPEC.padRightPx());
        double iconY = (h - SPEC.identiconSizePx()) / 2;
        double baseline = h / 2 + fontSize * 0.33;
        String fill = b.kind().isStamp() ? SPEC.pillFillStampHex() : SPEC.pillFillHex();
        String labelColor = b.kind().isStamp() ? "#5a5750" : SPEC.labelColorHex();

        StringBuilder s = new StringBuilder();
        s.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "viewBox=\"0 0 %.0f %.0f\" width=\"%.0f\" height=\"%.0f\">\n", w, h, w, h));
        s.append(String.format(Locale.ROOT,
                "  <rect rx=\"%.0f\" ry=\"%.0f\" width=\"%.0f\" height=\"%.0f\" fill=\"%s\"/>\n",
                SPEC.cornerRadiusPx(), SPEC.cornerRadiusPx(), w, h, fill));
        if (b.kind().hasLetterGlyph()) {
            s.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" fill=\"%s\" font-size=\"%.0f\" font-family=\"Noto Sans, sans-serif\" font-weight=\"bold\">%s</text>\n",
                    SPEC.padLeftPx(), baseline, b.kind().colorHex(), fontSize, b.kind().glyph()));
        }
        if (b.kind().isStamp()) {
            // Reuse the renderer's locked pentagon by extracting it from the stamp sigil SVG.
            String pent = KonceptSvgRenderer.renderSigil(KonceptKind.STAMP, 1.0);
            String inner = pent.substring(pent.indexOf("<polygon"), pent.lastIndexOf("</svg>"));
            // The sigil's viewBox is 22×22 centred at 11,11; shift so it sits at padLeft, centred on h/2.
            s.append(String.format(Locale.ROOT, "  <g transform=\"translate(%.1f,%.1f) scale(%.3f)\">%s</g>\n",
                    SPEC.padLeftPx() - 3, (h - 22 * 0.8) / 2, 0.8, inner));
        }
        s.append(String.format(Locale.ROOT,
                "  <image x=\"%.1f\" y=\"%.1f\" width=\"%.0f\" height=\"%.0f\" "
                + "image-rendering=\"optimizeSpeed\" style=\"image-rendering:pixelated\" "
                + "xlink:href=\"data:image/png;base64,%s\"/>\n",
                iconX, iconY, SPEC.identiconSizePx(), SPEC.identiconSizePx(),
                Base64.getEncoder().encodeToString(png)));
        s.append(String.format(Locale.ROOT,
                "  <text x=\"%.1f\" y=\"%.1f\" fill=\"%s\" font-size=\"%.0f\" "
                + "font-family=\"%s, Alegreya Sans SC, Noto Sans, sans-serif\" font-weight=\"500\" letter-spacing=\"0.24\">%s</text>\n",
                labelX, baseline, labelColor, fontSize, SPEC.smallCapsFamilyName(), escape(b.label())));
        s.append("</svg>\n");
        return s.toString();
    }

    /** 32×32 rows of hex colours from LifeHash at module size 1. */
    static String cellsJson(String idString) {
        LifeHash.Image img = LifeHash.makeFromUTF8(idString, LifeHashVersion.VERSION2, 1, false);
        List<Byte> c = img.colors();
        int stride = img.hasAlpha() ? 4 : 3;
        StringBuilder s = new StringBuilder("[\n");
        for (int y = 0; y < img.height(); y++) {
            s.append("  [");
            for (int x = 0; x < img.width(); x++) {
                int i = (y * img.width() + x) * stride;
                s.append(String.format(Locale.ROOT, "\"#%02x%02x%02x\"",
                        c.get(i) & 0xff, c.get(i + 1) & 0xff, c.get(i + 2) & 0xff));
                if (x < img.width() - 1) s.append(',');
            }
            s.append(']').append(y < img.height() - 1 ? ",\n" : "\n");
        }
        return s.append("]\n").toString();
    }

    static String snomed(String sctid) {
        return idString(List.of(SnomedUuids.fromSnomed(sctid)));
    }

    /** Tinkar PublicId.idString formatting — mirrors the package-private KonceptIdentity. */
    static String idString(List<UUID> uuids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < uuids.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(uuids.get(i)).append('"');
        }
        return sb.append(']').toString();
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String json(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static void write(Path p, String content) throws IOException {
        Files.writeString(p, content, StandardCharsets.UTF_8);
    }
}
