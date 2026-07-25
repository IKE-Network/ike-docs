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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #865 parity gate for the adoc medium: the chip styles every adoc renderer draws from
 * ({@link KonceptChipStyles}) embed exactly the shared {@link KonceptAppearance} golden values —
 * pill fill, label colours, the spec radius at its label-relative em ratio, the unified pads,
 * borderless-inline policy, small caps, and the retired treatment. Drift between the spec and
 * the adoc output fails this build.
 */
class KonceptChipStylesParityTest {

    private static final KonceptAppearance SPEC = KonceptAppearance.defaults();

    @Test
    void pillEmbedsTheGoldenValues() {
        String pill = KonceptChipStyles.pillStyle();

        assertTrue(pill.contains("background:" + SPEC.pillFillHex()), "spec pill fill");
        assertTrue(pill.contains("border-radius:0.50em"),
                "spec radius at the label-relative ratio (6/12)");
        assertTrue(pill.contains("padding:0.08em 0.50em 0.08em 0.33em"),
                "the spec's unified 1/6/1/4 pads at label ratios");
        assertFalse(pill.contains("border:"),
                "adoc HTML is an INLINE context — borderless by the #742 border policy");
    }

    @Test
    void stampChipEmbedsTheStampFill() {
        assertTrue(KonceptChipStyles.stampChipStyle().contains("background:" + SPEC.pillFillStampHex()));
    }

    @Test
    void labelIsSmallCapsNormalWeightInTheSpecColours() {
        String active = KonceptChipStyles.labelStyle(false);
        assertTrue(active.contains("color:" + SPEC.labelColorHex()), "spec label colour");
        assertTrue(active.contains("font-variant:small-caps"), "spec label case");
        assertFalse(active.contains("font-weight"), "normal weight — no weight declaration");

        String retired = KonceptChipStyles.labelStyle(true);
        assertTrue(retired.contains("color:" + SPEC.labelColorInactiveHex()), "retired colour");
        assertTrue(retired.contains("text-decoration:line-through"), "retired strikethrough");
    }

    @Test
    void svgFallbackAgreesWithTheChip() {
        String svg = KonceptSvgRenderer.render("X", "X", null);
        assertTrue(svg.contains("fill=\"" + SPEC.pillFillHex() + "\""), "same pill fill");
        assertTrue(svg.contains("fill=\"" + SPEC.labelColorHex() + "\""), "same label colour");
        assertTrue(svg.contains("rx=\"" + (int) SPEC.cornerRadiusPx() + "\""), "same radius");
    }
}
