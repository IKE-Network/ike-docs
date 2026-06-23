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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the <em>locked</em> stamp-sigil constants (ike-issues#638) at their released values. This is
 * the only in-repo guard against silent drift: ike-docs cannot depend on Komet, so the duplicate copy
 * in {@code dev.ikm.komet.framework.controls.StampSigilGeometry} carries the same geometry value-for-
 * value until Komet re-points to this module (ike-issues#623). Any change to the geometry here must be
 * a deliberate, test-breaking act — and mirrored in Komet's copy until that re-point lands.
 */
class StampSigilGeometryTest {

    private static final double EPS = 0.0;

    @Test
    void verticesAreTheLockedPointUpPentagon() {
        double[][] expected = {
                {0.0, -1.0},
                {0.951, -0.309},
                {0.588, 0.809},
                {-0.588, 0.809},
                {-0.951, -0.309}
        };
        assertEquals(expected.length, StampSigilGeometry.VERTICES.length, "five vertices");
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], StampSigilGeometry.VERTICES[i], EPS,
                    "vertex V" + i + " is locked");
        }
    }

    @Test
    void axisDotRadiiAreTheLockedAsymmetricReading() {
        assertArrayEquals(new double[]{0.78, 0.48, 0.86, 0.56, 0.66},
                StampSigilGeometry.AXIS_DOT_RADII, EPS, "the asymmetric reading is locked");
    }

    @Test
    void scalarGeometryAndColorAreLocked() {
        assertEquals(0.10, StampSigilGeometry.DOT_RADIUS, EPS, "axis-dot radius");
        assertEquals(0.12, StampSigilGeometry.HUB_RADIUS, EPS, "hub radius");
        assertEquals(1.4, StampSigilGeometry.STROKE_WIDTH_PX, EPS, "stroke width");
        assertEquals(5, StampSigilGeometry.AXIS_COUNT, "five axes");
        assertEquals("#888780", StampSigilGeometry.COLOR, "the locked provenance gray");
    }
}
