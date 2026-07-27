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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #953 coverage gate: the bundled <em>IKE Koncept Glyphs</em> face must cover the whole
 * {@link KonceptGlyphs} inventory — the invariant is that no inventoried symbol ever reaches
 * ambient OS font fallback. Fails the build the moment the inventory outgrows the checked-in
 * TTF, which is the cue to rerun {@code src/build/regen-koncept-glyphs.py}.
 *
 * <p>The TTF is read with a minimal {@code cmap}/{@code name} parser over {@code java.base}
 * alone — this module deliberately reads neither JavaFX nor {@code java.desktop}, and the gate
 * keeps it that way rather than widening the test module graph for one font load.
 */
class KonceptGlyphsTest {

    @Test
    void bundledFaceCoversTheWholeInventory() throws Exception {
        Set<Integer> covered = coveredCodePoints(bundledFace());
        List<String> missing = new ArrayList<>();
        for (int codePoint : KonceptGlyphs.codePoints()) {
            if (!covered.contains(codePoint)) {
                missing.add("U+%04X %s".formatted(codePoint, Character.toString(codePoint)));
            }
        }
        assertTrue(missing.isEmpty(),
                "inventory outgrew the bundled face — rerun src/build/regen-koncept-glyphs.py; missing: "
                        + String.join(", ", missing));
    }

    @Test
    void bundledFaceCarriesTheSpecFamilyName() throws Exception {
        assertEquals(KonceptAppearance.defaults().glyphFamilyName(), familyName(bundledFace()),
                "the family renderers request is the family the bundled face registers as");
    }

    @Test
    void statusVocabularyIsInventoried() {
        String cluster = KonceptStatus.DEFINED.glyph() + KonceptStatus.PRIMITIVE.glyph()
                + KonceptStatus.ROOT.glyph() + KonceptStatus.MULTI_PARENT_GLYPH;
        Set<Integer> inventory = new HashSet<>();
        for (int codePoint : KonceptGlyphs.codePoints()) {
            inventory.add(codePoint);
        }
        cluster.codePoints().forEach(codePoint -> assertTrue(inventory.contains(codePoint),
                "status glyph U+%04X is missing from the KonceptGlyphs inventory"
                        .formatted(codePoint)));
    }

    /** The bundled face as a byte buffer, asserted present. */
    private static ByteBuffer bundledFace() throws Exception {
        try (InputStream stream = KonceptAppearance.glyphFont()) {
            assertNotNull(stream, "the glyph face TTF is bundled in the jar");
            return ByteBuffer.wrap(stream.readAllBytes());
        }
    }

    /** Offset of the sfnt table with {@code tag}, or {@code -1} when absent. */
    private static int tableOffset(ByteBuffer font, String tag) {
        int tables = font.getShort(4) & 0xFFFF;
        for (int i = 0; i < tables; i++) {
            int record = 12 + i * 16;
            byte[] recordTag = new byte[4];
            font.get(record, recordTag);
            if (new String(recordTag, StandardCharsets.US_ASCII).equals(tag)) {
                return font.getInt(record + 8);
            }
        }
        return -1;
    }

    /** Every codepoint mapped by the face's Unicode {@code cmap} subtables (formats 4 and 12). */
    private static Set<Integer> coveredCodePoints(ByteBuffer font) {
        int cmap = tableOffset(font, "cmap");
        assertTrue(cmap >= 0, "the face has a cmap table");
        Set<Integer> covered = new HashSet<>();
        int subtables = font.getShort(cmap + 2) & 0xFFFF;
        for (int i = 0; i < subtables; i++) {
            int subtable = cmap + font.getInt(cmap + 4 + i * 8 + 4);
            int format = font.getShort(subtable) & 0xFFFF;
            if (format == 4) {
                int segments = (font.getShort(subtable + 6) & 0xFFFF) / 2;
                int ends = subtable + 14;
                int starts = ends + segments * 2 + 2;
                for (int segment = 0; segment < segments; segment++) {
                    int start = font.getShort(starts + segment * 2) & 0xFFFF;
                    int end = font.getShort(ends + segment * 2) & 0xFFFF;
                    if (start == 0xFFFF && end == 0xFFFF) {
                        continue;
                    }
                    for (int codePoint = start; codePoint <= end; codePoint++) {
                        covered.add(codePoint);
                    }
                }
            } else if (format == 12) {
                int groups = font.getInt(subtable + 12);
                for (int group = 0; group < groups; group++) {
                    int record = subtable + 16 + group * 12;
                    for (int codePoint = font.getInt(record); codePoint <= font.getInt(record + 4);
                            codePoint++) {
                        covered.add(codePoint);
                    }
                }
            }
        }
        return covered;
    }

    /** The face's family name (name ID 1, Windows Unicode BMP encoding, UTF-16BE). */
    private static String familyName(ByteBuffer font) {
        int name = tableOffset(font, "name");
        assertTrue(name >= 0, "the face has a name table");
        int count = font.getShort(name + 2) & 0xFFFF;
        int strings = name + (font.getShort(name + 4) & 0xFFFF);
        for (int i = 0; i < count; i++) {
            int record = name + 6 + i * 12;
            int platform = font.getShort(record) & 0xFFFF;
            int nameId = font.getShort(record + 6) & 0xFFFF;
            if (platform == 3 && nameId == 1) {
                byte[] value = new byte[font.getShort(record + 8) & 0xFFFF];
                font.get(strings + (font.getShort(record + 10) & 0xFFFF), value);
                return new String(value, StandardCharsets.UTF_16BE);
            }
        }
        return null;
    }
}
