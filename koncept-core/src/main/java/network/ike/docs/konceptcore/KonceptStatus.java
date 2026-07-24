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

import java.util.Locale;

/**
 * The logical-definition status a Koncept badge leads with (ike-issues#742 design amendment,
 * 2026-07-24): the concept's own defining copula from its stated EL++ expression — {@code ≡}
 * for a sufficiently defined concept, {@code ⊑} for a primitive (necessary-conditions-only)
 * concept, {@code ⊤} for a taxonomy root — plus the appended {@value #MULTI_PARENT_GLYPH} fork
 * when the concept has more than one stated parent.
 *
 * <p>Status marks and {@link KonceptKind kind sigils} never co-occur: only Koncepts carry a
 * logical definition, and the Koncept is the one kind with no sigil — so the badge's leading
 * slot always holds exactly one mark class. A Koncept with no stated definition
 * ({@link #NONE}) stays truly bare.
 *
 * <p>Like {@link KonceptKind}, the glyph and colour are <em>data</em> (not styling), so every
 * medium adapter — the adoc renderer, the Zulip/email PNG, the JavaFX badge — renders the same
 * cluster; static media compute the status at generation time. Multi-parent is not a member of
 * this enum: it is an orthogonal, structural property (derived from the parent count, e.g. a
 * {@code broader:} list's size) rendered as an appended fork glyph.
 */
public enum KonceptStatus {

    /** No stated logical definition — the badge stays truly bare. */
    NONE(null, null, "No stated definition"),

    /** A taxonomy root: a stated definition with no is-a parents. Dark-amber {@code ⊤}. */
    ROOT("⊤", "#8a6d00", "Taxonomy root"),

    /** A sufficiently defined (equivalence) concept — the stated expression carries a sufficient set. Green {@code ≡}. */
    DEFINED("≡", "#3b8c2f", "Sufficiently defined"),

    /** A primitive concept — necessary conditions only, no sufficient set. Grey {@code ⊑}. */
    PRIMITIVE("⊑", "#6b7682", "Primitive");

    /** The fork glyph appended after the copula for a concept with more than one stated parent. */
    public static final String MULTI_PARENT_GLYPH = "⋎";

    /** The multi-parent fork colour as a web hex string (data, for cross-medium parity). */
    public static final String MULTI_PARENT_COLOR_HEX = "#185fa5";

    /** The multi-parent accessible name — the non-colour, non-glyph accessibility channel. */
    public static final String MULTI_PARENT_ACCESSIBLE_NAME = "Multiple parents";

    private final String glyph;
    private final String colorHex;
    private final String accessibleName;

    KonceptStatus(String glyph, String colorHex, String accessibleName) {
        this.glyph = glyph;
        this.colorHex = colorHex;
        this.accessibleName = accessibleName;
    }

    /**
     * Resolves a status name (for example a {@code status:} field in {@code koncepts.yml})
     * to a status.
     *
     * @param name the status name, case-insensitive; {@code null} or blank yields {@link #NONE}
     * @return the matching status, or {@link #NONE} when {@code name} is non-blank but
     *         unrecognised (callers that care can detect this by comparing a non-blank name
     *         against the {@link #NONE} result)
     */
    public static KonceptStatus fromString(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    /**
     * The copula glyph this status displays.
     *
     * @return the glyph string ({@code ≡} defined, {@code ⊑} primitive, {@code ⊤} root), or
     *         {@code null} for {@link #NONE}
     */
    public String glyph() {
        return glyph;
    }

    /**
     * The copula colour as a web hex string (data, for cross-medium parity).
     *
     * @return the colour hex (e.g. {@code #3b8c2f}), or {@code null} for {@link #NONE}
     */
    public String colorHex() {
        return colorHex;
    }

    /**
     * A human-readable status name for tooltips / assistive technology — the non-colour,
     * non-glyph accessibility channel.
     *
     * @return the accessible status name
     */
    public String accessibleName() {
        return accessibleName;
    }

    /**
     * Whether this status renders a visible copula glyph.
     *
     * @return {@code true} for every status except {@link #NONE}
     */
    public boolean hasGlyph() {
        return glyph != null;
    }

    /**
     * The full leading cluster for a badge: the copula glyph, with the
     * {@value #MULTI_PARENT_GLYPH} fork appended when {@code multiParent} is set.
     *
     * @param multiParent whether the concept has more than one stated parent
     * @return the cluster text (e.g. {@code ≡}, {@code ⊑⋎}), or the empty string for
     *         {@link #NONE} (the fork is never shown without a copula to follow)
     */
    public String cluster(boolean multiParent) {
        if (glyph == null) {
            return "";
        }
        return multiParent ? glyph + MULTI_PARENT_GLYPH : glyph;
    }
}
