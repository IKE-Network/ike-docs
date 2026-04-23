package network.ike.docs.koncept;

/**
 * Generates inline SVG badge markup for Koncept references.
 * <p>
 * The badge renders as a compact, clickable pill that links to the
 * glossary anchor for the referenced Koncept. The "K" prefix is
 * rendered in a contrasting weight to visually distinguish Koncept
 * references from surrounding text.
 * <p>
 * SVG is used rather than HTML/CSS to ensure consistent rendering
 * across HTML and PDF backends.
 */
public final class KonceptSvgRenderer {

    private KonceptSvgRenderer() {}

    /**
     * Approximate character width for the sans-serif font at 12px.
     * Used to compute SVG element width dynamically.
     */
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

    /**
     * Render an inline SVG badge for a Koncept reference.
     *
     * @param target  the CamelCase koncept identifier (used for anchor link)
     * @param label   the human-readable display label
     * @return complete SVG+anchor HTML string suitable for inline passthrough
     */
    public static String render(String target, String label) {
        int labelWidth = (int) Math.ceil(label.length() * CHAR_WIDTH);
        int totalWidth = PADDING_X + PREFIX_WIDTH + labelWidth + PADDING_X;

        return """
            <a href="#koncept-%s" class="koncept-ref" title="%s">\
            <svg xmlns="http://www.w3.org/2000/svg" class="koncept-badge" \
            width="%d" height="%d" \
            style="display:inline-block;vertical-align:middle;cursor:pointer;">\
            <rect rx="%d" ry="%d" width="%d" height="%d" fill="%s"/>\
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
                PADDING_X, TEXT_BASELINE_Y, TEXT_COLOR, FONT_SIZE, FONT_FAMILY,
                PADDING_X + PREFIX_WIDTH, TEXT_BASELINE_Y,
                TEXT_COLOR, FONT_SIZE, FONT_FAMILY,
                escapeHtml(label)
        ).strip();
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
