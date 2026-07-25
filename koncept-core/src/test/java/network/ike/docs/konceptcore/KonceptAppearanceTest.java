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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the #742 design-resolution matrix as {@link KonceptAppearance#defaults()} — a value
 * drifting here is a renderer-visible appearance change and must be a deliberate spec edit —
 * and the bundled small-caps face (present, non-trivial, TTF-signed, OFL alongside).
 */
class KonceptAppearanceTest {

    @Test
    void defaultsAreTheSettledMatrix() {
        KonceptAppearance spec = KonceptAppearance.defaults();

        assertEquals("#e9eff6", spec.pillFillHex());
        assertEquals("#ecebe8", spec.pillFillStampHex());
        assertEquals("#2a5a8a", spec.labelColorHex());
        assertEquals("#b00020", spec.labelColorInactiveHex());
        assertEquals("#c8d6e6", spec.floatingBorderHex());
        assertEquals(6, spec.cornerRadiusPx());
        assertEquals(1, spec.floatingBorderWidthPx());
        assertEquals(14, spec.identiconSizePx());
        assertEquals(4, spec.iconLabelGapPx());
        assertEquals(1, spec.padTopPx());
        assertEquals(6, spec.padRightPx());
        assertEquals(1, spec.padBottomPx());
        assertEquals(4, spec.padLeftPx());
        assertEquals(12, spec.labelSizePx());
        assertEquals(LabelCase.SMALL_CAPS, spec.labelCase());
        assertFalse(spec.labelBold(), "normal weight everywhere — boldness belongs to sigils");
        assertTrue(spec.inactiveStrikethrough());
        assertEquals("Alegreya Sans SC Medium", spec.smallCapsFamilyName());
    }

    @Test
    void bundledSmallCapsFaceIsARealTrueTypeFile() throws IOException {
        try (InputStream in = KonceptAppearance.smallCapsFont()) {
            assertNotNull(in, "the small-caps TTF ships in the koncept-core jar");
            byte[] head = in.readNBytes(4);
            // TrueType sfnt version 1.0: 00 01 00 00.
            assertEquals(0x00, head[0]);
            assertEquals(0x01, head[1]);
            assertEquals(0x00, head[2]);
            assertEquals(0x00, head[3]);
            long rest = 0;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                rest += read;
            }
            assertTrue(rest > 50_000, "a real face, not a placeholder");
        }
    }

    @Test
    void licenseShipsBesideTheFace() {
        assertNotNull(KonceptAppearance.class.getResourceAsStream(
                        "/network/ike/docs/konceptcore/AlegreyaSansSC-OFL.txt"),
                "the SIL OFL license travels with the font");
    }
}
