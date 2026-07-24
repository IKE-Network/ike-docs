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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the {@link KonceptStatus} vocabulary (ike-issues#742 design amendment, #940): the
 * copula glyphs, their web-hex colours, the YAML {@code status:} resolution, and the
 * leading-cluster composition with the multi-parent fork.
 */
class KonceptStatusTest {

    @Test
    void glyphsAndColoursAreTheLockedVocabulary() {
        assertEquals("≡", KonceptStatus.DEFINED.glyph());
        assertEquals("#3b8c2f", KonceptStatus.DEFINED.colorHex());
        assertEquals("⊑", KonceptStatus.PRIMITIVE.glyph());
        assertEquals("#6b7682", KonceptStatus.PRIMITIVE.colorHex());
        assertEquals("⊤", KonceptStatus.ROOT.glyph());
        assertEquals("#8a6d00", KonceptStatus.ROOT.colorHex());
        assertEquals("⋎", KonceptStatus.MULTI_PARENT_GLYPH);
        assertEquals("#185fa5", KonceptStatus.MULTI_PARENT_COLOR_HEX);
        assertNull(KonceptStatus.NONE.glyph());
        assertFalse(KonceptStatus.NONE.hasGlyph());
    }

    @Test
    void fromStringResolvesTheYamlVocabularyCaseInsensitively() {
        assertEquals(KonceptStatus.DEFINED, KonceptStatus.fromString("defined"));
        assertEquals(KonceptStatus.PRIMITIVE, KonceptStatus.fromString("Primitive"));
        assertEquals(KonceptStatus.ROOT, KonceptStatus.fromString(" ROOT "));
        assertEquals(KonceptStatus.NONE, KonceptStatus.fromString(null));
        assertEquals(KonceptStatus.NONE, KonceptStatus.fromString("  "));
        assertEquals(KonceptStatus.NONE, KonceptStatus.fromString("unrecognised"));
    }

    @Test
    void clusterAppendsTheForkOnlyWithACopulaToFollow() {
        assertEquals("≡", KonceptStatus.DEFINED.cluster(false));
        assertEquals("≡⋎", KonceptStatus.DEFINED.cluster(true));
        assertEquals("⊑⋎", KonceptStatus.PRIMITIVE.cluster(true));
        assertEquals("", KonceptStatus.NONE.cluster(false));
        assertEquals("", KonceptStatus.NONE.cluster(true));
        assertTrue(KonceptStatus.ROOT.cluster(false).equals("⊤"));
    }

    @Test
    void accessibleNamesAreTheNonGlyphChannel() {
        assertEquals("Sufficiently defined", KonceptStatus.DEFINED.accessibleName());
        assertEquals("Primitive", KonceptStatus.PRIMITIVE.accessibleName());
        assertEquals("Taxonomy root", KonceptStatus.ROOT.accessibleName());
        assertEquals("Multiple parents", KonceptStatus.MULTI_PARENT_ACCESSIBLE_NAME);
    }
}
