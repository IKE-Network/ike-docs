/**
 * Koncept AsciiDoc Extension — inline markup for clinical knowledge concepts.
 *
 * <h2>Overview</h2>
 *
 * <p>This package provides an AsciidoctorJ extension that introduces the
 * {@code k:ConceptName[]} inline macro for referencing clinical and
 * knowledge-engineering concepts in AsciiDoc documents.
 * Each reference renders as a clickable SVG badge and is automatically
 * collected into a "Referenced Koncepts" glossary appended to the document.</p>
 *
 * <h2>Extension Architecture</h2>
 *
 * <p>The extension consists of three cooperating components:</p>
 *
 * <dl>
 *   <dt>{@link network.ike.docs.koncept.KonceptInlineMacro}</dt>
 *   <dd>Processes {@code k:Name[]} syntax during document conversion.
 *       Renders backend-appropriate markup (SVG for HTML, styled phrase
 *       for DocBook, bold text for Prawn PDF) and registers each reference
 *       in a document-scoped registry.</dd>
 *
 *   <dt>{@link network.ike.docs.koncept.KonceptGlossaryProcessor}</dt>
 *   <dd>Postprocessor that consumes the registry after conversion,
 *       resolves definitions from a {@link network.ike.docs.koncept.KonceptDefinitionSource},
 *       and appends a structured glossary and colophon to the output.</dd>
 *
 *   <dt>{@link network.ike.docs.koncept.KonceptExtensionRegistry}</dt>
 *   <dd>SPI entry point that auto-registers the inline macro with
 *       AsciidoctorJ. The glossary postprocessor is registered separately
 *       per-execution to avoid Prawn PDF backend incompatibility.</dd>
 * </dl>
 *
 * <h2>Koncept Definitions</h2>
 *
 * <p>Definitions are sourced via the {@link network.ike.docs.koncept.KonceptDefinitionSource}
 * strategy interface. The default implementation,
 * {@link network.ike.docs.koncept.YamlKonceptDefinitionSource}, loads from
 * YAML files. Each definition includes:</p>
 *
 * <ul>
 *   <li>Natural language definition text</li>
 *   <li>Description logic axiom using Unicode DL notation
 *       (≡, ⊓, ∃, ∀, ⊑, etc.)</li>
 *   <li>Optional SNOMED CT concept identifier (SCTID)</li>
 *   <li>Optional OWL IRI</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>In AsciiDoc source:</p>
 * <pre>{@code
 * The patient presented with k:HeartFailure[] and k:AorticStenosis[].
 * A label override: k:HeartFailure[Congestive Heart Failure].
 * }</pre>
 *
 * <p>In a YAML definitions file ({@code koncepts.yml}):</p>
 * <pre>{@code
 * HeartFailure:
 *   label: Heart Failure
 *   definition: >
 *     A clinical syndrome characterized by the heart's inability
 *     to pump sufficient blood to meet metabolic demands.
 *   axiom: "≡ ClinicalSyndrome ⊓ ∃hasPathology.(InsufficientCardiacOutput)"
 *   sctid: "84114007"
 * }</pre>
 *
 * <h2>Backend Support</h2>
 *
 * <table>
 *   <caption>Rendering behavior by AsciiDoc backend</caption>
 *   <tr><th>Backend</th><th>Inline Rendering</th><th>Glossary</th></tr>
 *   <tr><td>html5</td><td>SVG badge with anchor link</td><td>HTML definition list</td></tr>
 *   <tr><td>docbook5</td><td>DocBook link + phrase</td><td>DocBook glossary</td></tr>
 *   <tr><td>pdf (Prawn)</td><td>Bold + code styled text</td><td>Not supported</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @see network.ike.docs.koncept.KonceptInlineMacro
 * @see network.ike.docs.koncept.KonceptGlossaryProcessor
 * @see network.ike.docs.koncept.KonceptDefinitionSource
 */
package network.ike.docs.koncept;
