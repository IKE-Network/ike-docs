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

/**
 * Rendering-agnostic Koncept core (ike-issues#623) — the single source of the cross-medium Koncept
 * rendering scheme:
 *
 * <ul>
 *   <li>{@link network.ike.docs.konceptcore.KonceptKind} — the honest component-kind sigil scheme
 *       ("concept bare, everything else marked"): a coloured letter for description/semantic/pattern/
 *       unknown, the pentagon for a stamp.</li>
 *   <li>{@link network.ike.docs.konceptcore.StampSigilGeometry} — the locked pentagon geometry of the
 *       STAMP sigil (ike-issues#638), pure constants with no rendering dependency.</li>
 * </ul>
 *
 * <p>The glyph, colour, and geometry are <em>data</em> (not styling), so every medium adapter — the
 * adoc SVG macro, the Zulip/email PNG compositor, the JavaFX badge — renders the same sigil. This
 * module is UPSTREAM of Komet in the release cascade; Komet's
 * {@code dev.ikm.komet.framework.controls} copies of these types re-point to this published artifact
 * once #623 lands.
 */
package network.ike.docs.konceptcore;
