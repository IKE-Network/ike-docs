package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptKind;
import network.ike.docs.konceptcore.StampSigilGeometry;

import java.util.Locale;

/**
 * Generates inline SVG badge markup for Koncept references.
 * <p>
 * The badge renders as a compact, clickable pill that links to the glossary anchor for the
 * referenced Koncept. The component-kind sigil (ike-issues#638) leads the badge: a coloured
 * letter ({@code D}/{@code S}/{@code P}/{@code ?}) for description/semantic/pattern/unknown, the
 * locked {@link StampSigilGeometry} pentagon for a stamp (no name), and nothing for a bare concept.
 * <p>
 * SVG is used rather than HTML/CSS to ensure consistent rendering across HTML and PDF backends.
 */
public final class KonceptSvgRenderer {

    private KonceptSvgRenderer() {}

    /** Approximate character width for the sans-serif font at 12px. */
    private static final double CHAR_WIDTH = 7.2;

    /** Horizontal padding inside the badge. */
    private static final int PADDING_X = 8;

    /** Width of the "K" prefix plus separator space. */
    private static final int PREFIX_WIDTH = 18;

    /** Badge height. */
    private static final int HEIGHT = 22;

    /** Badge corner radius. */
    private static final int CORNER_RADIUS = 4;

    /** Badge background color. */
    private static final String BADGE_COLOR = "#2a5a8a";

    /** Text color. */
    private static final String TEXT_COLOR = "white";

    /** Font family. */
    private static final String FONT_FAMILY = "sans-serif";

    /** Font size. */
    private static final int FONT_SIZE = 12;

    /** Y-offset for text baseline within the badge. */
    private static final int TEXT_BASELINE_Y = 15;

    /** Horizontal space reserved for a leading kind letter sigil (px). */
    private static final int SIGIL_WIDTH = 13;

    /** Inline edge length of the stamp pentagon — matches the JavaFX {@code StampSigil}. */
    private static final int STAMP_BOX = 16;

    /** Pentagon unit-radius as a fraction of the half-box — identical to the JavaFX {@code StampSigil}. */
    private static final double STAMP_RADIUS_FRACTION = 0.92;

    /** Gray provenance chip behind the stamp pentagon + text. */
    private static final String STAMP_CHIP_COLOR = "#ecebe8";

    /** Dark-gray text for the stamp's compact provenance. */
    private static final String STAMP_TEXT_COLOR = "#5a5750";

    /**
     * Render an inline SVG badge for a Koncept reference, honest about its kind.
     *
     * @param target the CamelCase koncept identifier (used for the anchor link)
     * @param label  the human-readable display label
     * @param kind   the component kind; {@code null} is treated as {@link KonceptKind#CONCEPT}
     * @return complete SVG+anchor HTML string suitable for inline passthrough
     */
    public static String render(String target, String label, KonceptKind kind) {
        KonceptKind resolved = (kind == null) ? KonceptKind.CONCEPT : kind;
        if (resolved.isStamp()) {
            return renderStampSigil(target, label);
        }
        int labelWidth = (int) Math.ceil(label.length() * CHAR_WIDTH);
        int sigilW = resolved.hasLetterGlyph() ? SIGIL_WIDTH : 0;
        int totalWidth = PADDING_X + sigilW + PREFIX_WIDTH + labelWidth + PADDING_X;

        String sigil = resolved.hasLetterGlyph()
                ? "<text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"%d\" font-family=\"%s\" font-weight=\"bold\">%s</text>"
                        .formatted(PADDING_X, TEXT_BASELINE_Y, resolved.colorHex(), FONT_SIZE, FONT_FAMILY,
                                escapeHtml(resolved.glyph()))
                : "";

        return """
            <a href="#koncept-%s" class="koncept-ref" title="%s">\
            <svg xmlns="http://www.w3.org/2000/svg" class="koncept-badge" \
            width="%d" height="%d" \
            style="display:inline-block;vertical-align:middle;cursor:pointer;">\
            <rect rx="%d" ry="%d" width="%d" height="%d" fill="%s"/>\
            %s\
            <text x="%d" y="%d" fill="%s" font-size="%d" \
            font-family="%s" font-weight="bold">K</text>\
            <text x="%d" y="%d" fill="%s" font-size="%d" \
            font-family="%s">%s</text>\
            </svg></a>\
            """.formatted(
                escapeHtml(target),
                escapeHtml(label),
                totalWidth, HEIGHT,
                CORNER_RADIUS, CORNER_RADIUS, totalWidth, HEIGHT, BADGE_COLOR,
                sigil,
                PADDING_X + sigilW, TEXT_BASELINE_Y, TEXT_COLOR, FONT_SIZE, FONT_FAMILY,
                PADDING_X + sigilW + PREFIX_WIDTH, TEXT_BASELINE_Y,
                TEXT_COLOR, FONT_SIZE, FONT_FAMILY,
                escapeHtml(label)
        ).strip();
    }

    /**
     * Render the STAMP kind sigil: the locked gray pentagon (computed from the shared
     * {@link StampSigilGeometry}) as inline SVG — five asymmetric reading dots and a centre hub,
     * no name. The geometry is the same numbers the JavaFX node and the Java2D PNG use, so the
     * pentagon is identical in every medium.
     *
     * @param target the CamelCase koncept identifier (used for the anchor link)
     * @param label  the compact stamp text ({@code status · date-time · author}) shown beside the
     *               pentagon — provenance, in place of a name
     * @return complete SVG+anchor HTML string for the stamp pentagon + compact text
     * @implNote {@code label} is the caller-formatted, verbatim provenance string; the renderer only
     *           HTML-escapes it and never reformats or re-spells it. In particular the status token
     *           within {@code label} must already be the verbatim closed-vocabulary value
     *           (ike-issues feedback: render closed vocabularies verbatim).
     */
    public static String renderStampSigil(String target, String label) {
        double centerX = PADDING_X + STAMP_BOX / 2.0;
        double centerY = HEIGHT / 2.0;
        double unitRadius = (STAMP_BOX / 2.0) * STAMP_RADIUS_FRACTION;

        StringBuilder points = new StringBuilder();
        for (int i = 0; i < StampSigilGeometry.AXIS_COUNT; i++) {
            double px = centerX + StampSigilGeometry.VERTICES[i][0] * unitRadius;
            double py = centerY + StampSigilGeometry.VERTICES[i][1] * unitRadius;
            if (i > 0) {
                points.append(' ');
            }
            points.append(fmt(px)).append(',').append(fmt(py));
        }

        double dotRadius = Math.max(StampSigilGeometry.DOT_RADIUS * unitRadius, 1.0);
        double hubRadius = dotRadius * (StampSigilGeometry.HUB_RADIUS / StampSigilGeometry.DOT_RADIUS);
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < StampSigilGeometry.AXIS_COUNT; i++) {
            double reading = StampSigilGeometry.AXIS_DOT_RADII[i] * unitRadius;
            double dx = centerX + StampSigilGeometry.VERTICES[i][0] * reading;
            double dy = centerY + StampSigilGeometry.VERTICES[i][1] * reading;
            dots.append("<circle cx=\"%s\" cy=\"%s\" r=\"%s\" fill=\"%s\"/>"
                    .formatted(fmt(dx), fmt(dy), fmt(dotRadius), StampSigilGeometry.COLOR));
        }
        dots.append("<circle cx=\"%s\" cy=\"%s\" r=\"%s\" fill=\"%s\"/>"
                .formatted(fmt(centerX), fmt(centerY), fmt(hubRadius), StampSigilGeometry.COLOR));

        int labelWidth = (int) Math.ceil(label.length() * CHAR_WIDTH);
        int textX = PADDING_X + STAMP_BOX + 6;
        int totalWidth = textX + labelWidth + PADDING_X;

        return """
            <a href="#koncept-%s" class="koncept-ref stamp-sigil" title="%s">\
            <svg xmlns="http://www.w3.org/2000/svg" class="koncept-stamp" width="%d" height="%d" \
            style="display:inline-block;vertical-align:middle;cursor:pointer;">\
            <rect rx="%d" ry="%d" width="%d" height="%d" fill="%s"/>\
            <polygon points="%s" fill="none" stroke="%s" stroke-width="%s" stroke-linejoin="round"/>\
            %s\
            <text x="%d" y="%d" fill="%s" font-size="%d" font-family="%s">%s</text>\
            </svg></a>\
            """.formatted(
                escapeHtml(target), escapeHtml(label),
                totalWidth, HEIGHT,
                CORNER_RADIUS, CORNER_RADIUS, totalWidth, HEIGHT, STAMP_CHIP_COLOR,
                points, StampSigilGeometry.COLOR, fmt(StampSigilGeometry.STROKE_WIDTH_PX),
                dots,
                textX, TEXT_BASELINE_Y, STAMP_TEXT_COLOR, FONT_SIZE, FONT_FAMILY, escapeHtml(label)
        ).strip();
    }

    /** Formats a coordinate compactly: integers without a decimal point, else two places. */
    private static String fmt(double v) {
        return (v == Math.rint(v)) ? Long.toString((long) v) : String.format(Locale.ROOT, "%.2f", v);
    }

    /**
     * Minimal HTML escaping for attribute values and text content.
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
