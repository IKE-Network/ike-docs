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
 * How a Koncept badge renders its label text ({@link KonceptAppearance#labelCase()}).
 */
public enum LabelCase {

    /**
     * True small caps — capitals full height, the rest small capitals, the name in its natural
     * case. CSS media use {@code font-variant: small-caps}; raster media load the
     * {@linkplain KonceptAppearance#smallCapsFont() bundled dedicated family}, whose glyphs
     * already are small caps.
     */
    SMALL_CAPS,

    /** The label as-is, in the surrounding font — the fallback when small caps cannot render. */
    VERBATIM
}
