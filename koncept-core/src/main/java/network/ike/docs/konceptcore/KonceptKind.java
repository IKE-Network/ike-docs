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
 * The component-kind a Koncept badge is <em>honest</em> about, with the leading asymmetric sigil
 * scheme "Koncept bare, everything else marked" (ike-issues#638): {@link #CONCEPT} shows no sigil,
 * {@link #DESCRIPTION}/{@link #SEMANTIC}/{@link #PATTERN}/{@link #UNKNOWN} show a coloured letter,
 * and {@link #STAMP} shows the {@link StampSigilGeometry pentagon}.
 *
 * <p>The glyph and colour are <em>data</em> (not styling), so every medium adapter — the adoc SVG
 * macro, the Zulip/email PNG, the JavaFX badge — renders the same sigil. Komet consumes this enum
 * directly ({@code dev.ikm.komet.framework.controls.KonceptKindResolver}, re-pointed under
 * ike-issues#623): this module is the single source of the kind vocabulary.
 */
public enum KonceptKind {

    /**
     * A Koncept — with a K: the formal, identified, versioned construct carrying descriptions and a
     * logical definition, deliberately distinct from a concept in the mind (it is the
     * <em>referent</em> of the semiotic triangle, not the thought). The bare default; rendered with
     * no kind sigil.
     */
    CONCEPT(null, null, "Koncept"),

    /** A semantic whose pattern is one of the view coordinate's description patterns. Amber {@code D}. */
    DESCRIPTION("D", "#b8860b", "Description"),

    /** Any semantic that is not a description. Green {@code S}. */
    SEMANTIC("S", "#3b8c2f", "Semantic"),

    /** A pattern. Violet {@code P}. */
    PATTERN("P", "#7a4fb5", "Pattern"),

    /** A stamp (status · time · author · module · path). The gray {@link StampSigilGeometry} pentagon. */
    STAMP(null, StampSigilGeometry.COLOR, "Stamp"),

    /** Kind could not be positively determined (unresolvable id, presentation-only badge). Red {@code ?}. */
    UNKNOWN("?", "#b00020", "Unknown");

    private final String glyph;
    private final String colorHex;
    private final String accessibleName;

    KonceptKind(String glyph, String colorHex, String accessibleName) {
        this.glyph = glyph;
        this.colorHex = colorHex;
        this.accessibleName = accessibleName;
    }

    /**
     * Resolves a kind name (for example a {@code kind:} field in {@code koncepts.yml}) to a kind.
     *
     * @param name the kind name, case-insensitive; {@code null} or blank yields {@link #CONCEPT}
     * @return the matching kind, or {@link #UNKNOWN} when {@code name} is non-blank but unrecognised
     */
    public static KonceptKind fromString(String name) {
        if (name == null || name.isBlank()) {
            return CONCEPT;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * The single-letter sigil, or {@code null} for {@link #CONCEPT} (bare) and {@link #STAMP} (pentagon).
     *
     * @return the letter sigil ({@code D}/{@code S}/{@code P}/{@code ?}), or {@code null}
     */
    public String glyph() {
        return glyph;
    }

    /**
     * The sigil colour as a web hex string (data, for cross-medium parity); {@code null} for
     * {@link #CONCEPT}.
     *
     * @return the colour hex (e.g. {@code #b8860b}), or {@code null}
     */
    public String colorHex() {
        return colorHex;
    }

    /**
     * A human-readable kind name for tooltips / assistive technology — the non-colour, non-glyph
     * accessibility channel.
     *
     * @return the accessible kind name
     */
    public String accessibleName() {
        return accessibleName;
    }

    /**
     * Whether this is the bare default ({@link #CONCEPT}) — the only kind that shows no sigil.
     *
     * @return {@code true} for {@link #CONCEPT}
     */
    public boolean isBare() {
        return this == CONCEPT;
    }

    /**
     * Whether this kind renders the {@link StampSigilGeometry} pentagon rather than a letter glyph.
     *
     * @return {@code true} for {@link #STAMP}
     */
    public boolean isStamp() {
        return this == STAMP;
    }

    /**
     * Whether this kind renders a single-letter sigil ({@code D}/{@code S}/{@code P}/{@code ?}).
     *
     * @return {@code true} when {@link #glyph()} is non-null
     */
    public boolean hasLetterGlyph() {
        return glyph != null;
    }

    /**
     * Whether this kind shows any sigil at all (everything except the bare {@link #CONCEPT}).
     *
     * @return {@code true} for every kind except {@link #CONCEPT}
     */
    public boolean hasSigil() {
        return this != CONCEPT;
    }
}
