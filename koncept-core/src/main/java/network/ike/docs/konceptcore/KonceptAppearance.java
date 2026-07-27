/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.konceptcore;

import java.io.InputStream;

/**
 * The one Koncept badge appearance specification (ike-issues#742/#860): the palette, reference
 * geometry, and label policy every badge renderer draws from — the JavaFX chip and drag glyph,
 * the adoc SVG/CSS, the Java2D Zulip/email PNG, and the HTML/email fragment. Pure data, like the
 * rest of this module: colours as web-hex strings, geometry as reference pixels at 1× (each
 * renderer maps hex→its colour type and px→its units/DPI at its own layer), no JavaFX, no
 * {@code java.awt}, no CSS.
 *
 * <p><b>Border policy</b> (settled under #742): the record carries the <em>floating</em> border —
 * a renderer applies it only in a floating context (the drag glyph, the Zulip/email PNG, HTML
 * pasted into a mail client), where the badge sits on a surface we do not control. Inline
 * contexts (the on-screen chip, adoc HTML under our own stylesheet) are borderless; there is no
 * per-context toggle field, because the context is the renderer's own, static knowledge.
 *
 * <p><b>Marks</b> are not fields here: the kind sigil vocabulary is {@link KonceptKind} and the
 * pentagon {@link StampSigilGeometry}; the logical-status cluster is {@link KonceptStatus},
 * rendered in every medium (the #742 amendment). This record carries what those vocabularies
 * sit on.
 *
 * <p><b>True small caps</b> need a family whose glyphs already are small capitals — CSS has
 * {@code font-variant}, but JavaFX and Java2D do not. The bundled <em>Alegreya Sans SC
 * Medium</em> face (SIL OFL, license alongside the resource) ships in this jar so every raster
 * consumer loads the same file: {@link #smallCapsFont()} opens it as a stream (stream, not URL —
 * classpath-URL string forms break under exotic installation paths).
 *
 * <p><b>Symbol glyphs</b> must never reach ambient OS font fallback (ike-issues#953: fallback
 * differs per requested base font and per platform, and macOS's Apple SD Gothic Neo draws the
 * {@code ⋎} fork upside-down). The bundled <em>IKE Koncept Glyphs</em> face — a merged subset of
 * pinned Noto releases covering the whole {@link KonceptGlyphs} inventory — ships in this jar the
 * same way: {@link #glyphFont()} opens it, {@link #glyphFamilyName()} names the family a renderer
 * requests for every glyph run.
 *
 * @param pillFillHex            the soft pill fill behind identicon + label ({@code #e9eff6})
 * @param pillFillStampHex       the gray provenance-chip fill for the STAMP kind ({@code #ecebe8})
 * @param labelColorHex          the label (IKE blue, {@code #2a5a8a})
 * @param labelColorInactiveHex  the retired label ({@code #b00020}); rendered with
 *                               {@link #inactiveStrikethrough()}
 * @param floatingBorderHex      the shared floating-context border ({@code #c8d6e6})
 * @param cornerRadiusPx         the pill corner radius (reference px at 1×)
 * @param floatingBorderWidthPx  the floating border width (reference px)
 * @param identiconSizePx        the identicon edge (reference px), sized to sit beside the label
 * @param iconLabelGapPx         the gap between the identicon and the label (reference px)
 * @param padTopPx               pill padding, top (reference px)
 * @param padRightPx             pill padding, right (reference px)
 * @param padBottomPx            pill padding, bottom (reference px)
 * @param padLeftPx              pill padding, left (reference px)
 * @param labelSizePx            the label font size (reference px) in the small-caps family
 * @param labelCase              the label rendering case ({@link LabelCase#SMALL_CAPS} per spec)
 * @param labelBold              whether the label is bold ({@code false} — normal weight
 *                               everywhere; the kind sigil's boldness is the sigil's own)
 * @param inactiveStrikethrough  whether a retired component's label is struck through
 * @param smallCapsFamilyName    the registered family name of the bundled small-caps face
 * @param glyphFamilyName        the registered family name of the bundled symbol-glyph face
 *                               (ike-issues#953)
 */
public record KonceptAppearance(
        String pillFillHex,
        String pillFillStampHex,
        String labelColorHex,
        String labelColorInactiveHex,
        String floatingBorderHex,
        double cornerRadiusPx,
        double floatingBorderWidthPx,
        double identiconSizePx,
        double iconLabelGapPx,
        double padTopPx,
        double padRightPx,
        double padBottomPx,
        double padLeftPx,
        double labelSizePx,
        LabelCase labelCase,
        boolean labelBold,
        boolean inactiveStrikethrough,
        String smallCapsFamilyName,
        String glyphFamilyName
) {

    /**
     * The classpath resource of the bundled small-caps face (SIL OFL;
     * {@code AlegreyaSansSC-OFL.txt} sits alongside it).
     */
    public static final String SMALL_CAPS_FONT_RESOURCE =
            "/network/ike/docs/konceptcore/AlegreyaSansSC-Medium.ttf";

    /**
     * The classpath resource of the bundled symbol-glyph face (SIL OFL;
     * {@code IKEKonceptGlyphs-OFL.txt} sits alongside it), regenerated by
     * {@code koncept-core/src/build/regen-koncept-glyphs.py} (ike-issues#953).
     */
    public static final String GLYPH_FONT_RESOURCE =
            "/network/ike/docs/konceptcore/IKEKonceptGlyphs-Regular.ttf";

    /** The one agreed appearance (the #742 design-resolution matrix). */
    private static final KonceptAppearance DEFAULTS = new KonceptAppearance(
            "#e9eff6",
            "#ecebe8",
            "#2a5a8a",
            "#b00020",
            "#c8d6e6",
            6,
            1,
            14,
            4,
            1, 6, 1, 4,
            12,
            LabelCase.SMALL_CAPS,
            false,
            true,
            "Alegreya Sans SC Medium",
            "IKE Koncept Glyphs");

    /**
     * The agreed badge appearance — the values of the #742 design-resolution matrix. Renderers
     * read this one instance; a divergent value hardcoded renderer-side is a #742 regression.
     *
     * @return the shared appearance specification
     */
    public static KonceptAppearance defaults() {
        return DEFAULTS;
    }

    /**
     * Opens the bundled small-caps face ({@value #SMALL_CAPS_FONT_RESOURCE}) for registration
     * with the consumer's font system — JavaFX {@code Font.loadFont(InputStream, double)}, AWT
     * {@code Font.createFont(TRUETYPE_FONT, InputStream)}. A fresh stream per call; the caller
     * closes it.
     *
     * @return the TTF stream, or {@code null} if the resource is missing from the jar
     */
    public static InputStream smallCapsFont() {
        return KonceptAppearance.class.getResourceAsStream(SMALL_CAPS_FONT_RESOURCE);
    }

    /**
     * Opens the bundled symbol-glyph face ({@value #GLYPH_FONT_RESOURCE}) for registration with
     * the consumer's font system — JavaFX {@code Font.loadFont(InputStream, double)}, AWT
     * {@code Font.createFont(TRUETYPE_FONT, InputStream)}. A fresh stream per call; the caller
     * closes it.
     *
     * @return the TTF stream, or {@code null} if the resource is missing from the jar
     */
    public static InputStream glyphFont() {
        return KonceptAppearance.class.getResourceAsStream(GLYPH_FONT_RESOURCE);
    }
}
