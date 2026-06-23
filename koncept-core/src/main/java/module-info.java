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
 * Rendering-agnostic Koncept core (ike-issues#623): the shared component-kind scheme
 * ({@link network.ike.docs.konceptcore.KonceptKind}) and the locked stamp-sigil pentagon geometry
 * ({@link network.ike.docs.konceptcore.StampSigilGeometry}). A pure, dependency-free library so the
 * same kind/geometry data drives every medium adapter (adoc SVG, Zulip/email PNG, JavaFX badge).
 *
 * <p>Named explicitly (not an automatic module) because the modular Komet app consumes the released
 * artifact with {@code requires network.ike.docs.konceptcore;} once it re-points downstream (#623);
 * the module name mirrors the sole package per IKE-NAMING.
 */
module network.ike.docs.konceptcore {
    exports network.ike.docs.konceptcore;
}
