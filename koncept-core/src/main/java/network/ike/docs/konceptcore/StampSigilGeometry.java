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

/**
 * The <em>locked</em> geometry of the STAMP kind sigil (ike-issues#638): a point-up pentagon with
 * one "reading" dot per axis plus a centre hub — a five-dimensional radar that reads as provenance,
 * not a clock (time is only one of the five stamp dimensions) and not a busy rubber-stamp.
 *
 * <p>Pure constants, no rendering dependency, so the same numbers drive every medium — the adoc SVG
 * badge, the Zulip/email PNG, and the JavaFX {@code StampSigil}. Coordinates are in a <em>y-down</em>
 * (screen / SVG) unit space centred at the origin, so {@link #VERTICES}{@code [0]} = {@code (0, -1)}
 * is the top point.
 *
 * <p><b>DUPLICATED (ike-issues#623):</b> Komet's
 * {@code dev.ikm.komet.framework.controls.StampSigilGeometry} carries the same geometry until Komet
 * re-points downstream to this module. Only the <em>constants</em> below ({@link #VERTICES},
 * {@link #AXIS_DOT_RADII}, {@link #DOT_RADIUS}, {@link #HUB_RADIUS}, {@link #STROKE_WIDTH_PX},
 * {@link #COLOR}, {@link #AXIS_COUNT}) must stay value-for-value identical across the two copies — the
 * package, javadoc, and surrounding code legitimately differ. This module is the intended single
 * source; {@code StampSigilGeometryTest} pins these released values so an edit can never drift silently.
 */
public final class StampSigilGeometry {

    private StampSigilGeometry() {
    }

    /**
     * The five pentagon vertices as unit vectors (radius 1) from the centre, point-up, y-down:
     * {@code V0(0,-1) V1(0.951,-0.309) V2(0.588,0.809) V3(-0.588,0.809) V4(-0.951,-0.309)}.
     */
    public static final double[][] VERTICES = {
            {0.0, -1.0},
            {0.951, -0.309},
            {0.588, 0.809},
            {-0.588, 0.809},
            {-0.951, -0.309}
    };

    /**
     * The radius (0..1) of the single "reading" dot on each axis {@code V0..V4} — an asymmetric
     * reading: {@code [0.78, 0.48, 0.86, 0.56, 0.66]}.
     */
    public static final double[] AXIS_DOT_RADII = {0.78, 0.48, 0.86, 0.56, 0.66};

    /** Axis-dot radius in unit space ({@code ≈ 0.10}); floor it in px so it is not a speck. */
    public static final double DOT_RADIUS = 0.10;

    /** Centre hub-dot radius in unit space ({@code ≈ 0.12}). */
    public static final double HUB_RADIUS = 0.12;

    /** Pentagon outline stroke width in pixels ({@code ≈ 1.4}, non-scaling, round joins). */
    public static final double STROKE_WIDTH_PX = 1.4;

    /** The single gray ({@value}) used for the outline, the dots, and the hub (metadata/provenance). */
    public static final String COLOR = "#888780";

    /** The number of pentagon axes / vertices / reading dots. */
    public static final int AXIS_COUNT = 5;
}
