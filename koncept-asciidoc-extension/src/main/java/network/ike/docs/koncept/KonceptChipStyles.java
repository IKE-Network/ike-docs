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
package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptAppearance;

import java.util.Locale;

/**
 * The adoc HTML chip's inline-style strings, computed from the one shared
 * {@link KonceptAppearance} spec (ike-issues#860/#864) — the single place the extension's
 * renderers (inline macro, taxonomy tree, glossary) take their pill, label, and identicon
 * styling from, so the adoc paths cannot drift from the spec or from each other.
 *
 * <p>The spec's geometry is reference pixels against its {@code labelSizePx}; the HTML chip
 * inherits the surrounding font size, so every length here is emitted in {@code em} at the
 * spec's ratio (e.g. corner radius {@code 6/12 = 0.5em}) and scales with the prose.
 * Inline styles, not classes: consuming documents may not link {@code koncept.css}, which
 * only mirrors these values.
 */
final class KonceptChipStyles {

    private static final KonceptAppearance SPEC = KonceptAppearance.defaults();

    private KonceptChipStyles() {
    }

    /** The soft rounded concept pill (fill, radius, unified pads), {@code display:inline}. */
    static String pillStyle() {
        return "display:inline;background:" + SPEC.pillFillHex() + ";"
                + "border-radius:" + em(SPEC.cornerRadiusPx()) + ";"
                + "padding:" + em(SPEC.padTopPx()) + " " + em(SPEC.padRightPx())
                + " " + em(SPEC.padBottomPx()) + " " + em(SPEC.padLeftPx()) + ";"
                + "-webkit-box-decoration-break:clone;box-decoration-break:clone;";
    }

    /** The gray STAMP provenance chip — same geometry, the stamp fill. */
    static String stampChipStyle() {
        return "display:inline;background:" + SPEC.pillFillStampHex() + ";"
                + "border-radius:" + em(SPEC.cornerRadiusPx()) + ";"
                + "padding:" + em(SPEC.padTopPx()) + " " + em(SPEC.padRightPx())
                + " " + em(SPEC.padBottomPx()) + " " + em(SPEC.padLeftPx()) + ";"
                + "-webkit-box-decoration-break:clone;box-decoration-break:clone;";
    }

    /**
     * The small-caps IKE-blue label, or its retired form — struck through in the inactive
     * colour (the #742 retired-parity decision; the strikethrough is the dedicated signal).
     *
     * @param inactive whether the referent's latest version is inactive in the set
     * @return the label's inline style
     */
    static String labelStyle(boolean inactive) {
        String caseAndTracking = "font-variant:small-caps;letter-spacing:0.02em;";
        if (inactive && SPEC.inactiveStrikethrough()) {
            return "color:" + SPEC.labelColorInactiveHex() + ";" + caseAndTracking
                    + "text-decoration:line-through;";
        }
        return "color:" + (inactive ? SPEC.labelColorInactiveHex() : SPEC.labelColorHex()) + ";"
                + caseAndTracking;
    }

    /**
     * The inline identicon image. The edge stays the medium-tuned {@code 0.9em}: the adoc
     * chip's label inherits the prose size (unlike the literal-px media the spec's
     * {@code identiconSizePx} ratio governs), and 0.9× the inherited size is the balance
     * against small caps that matches the spec's ~14px identicon at typical prose sizes.
     * The icon–label gap is the spec ratio.
     */
    static String identiconStyle() {
        return "height:0.9em;width:0.9em;vertical-align:-0.12em;"
                + "border-radius:2px;image-rendering:pixelated;"
                + "margin-right:" + em(SPEC.iconLabelGapPx()) + ";";
    }

    /** A reference-px length as {@code em} at the spec's label-relative ratio, two decimals. */
    private static String em(double referencePx) {
        return String.format(Locale.ROOT, "%.2fem",
                referencePx / KonceptAppearance.defaults().labelSizePx());
    }
}
