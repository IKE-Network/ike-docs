package network.ike.docs.koncept;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ArrayList;

/**
 * AsciidoctorJ postprocessor that generates a "Referenced Koncepts" glossary
 * section and a colophon at the end of the document.
 * <p>
 * Uses a {@link Postprocessor} rather than a Treeprocessor because inline
 * macros (including {@link KonceptInlineMacro}) are resolved during the
 * conversion phase, which runs after tree processing.
 * <p>
 * The glossary is built from the koncept registry populated by
 * {@link KonceptInlineMacro} during document conversion. Each referenced
 * koncept gets a single glossary entry containing:
 * <ul>
 *   <li>An anchor target matching the inline SVG badge links</li>
 *   <li>Natural language definition</li>
 *   <li>Description logic axiom in Unicode notation</li>
 *   <li>Optional SNOMED CT identifier</li>
 *   <li>Optional OWL IRI</li>
 * </ul>
 * <p>
 * The colophon describes the rendering pipeline used to produce the document,
 * including the AsciiDoc backend, build timestamp, and document version.
 * <p>
 * Definitions are resolved via {@link KonceptDefinitionSource}. The source
 * is configured by document attributes:
 * <ul>
 *   <li>{@code :koncept-definitions-file:} — filesystem path to YAML</li>
 *   <li>{@code :koncept-definitions-classpath:} — classpath resource path</li>
 *   <li>Default: {@code /koncepts.yml} on classpath</li>
 * </ul>
 */
public class KonceptGlossaryProcessor extends Postprocessor {

    /** Creates a new glossary processor instance. */
    public KonceptGlossaryProcessor() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(KonceptGlossaryProcessor.class);

    private static final String DEFAULT_CLASSPATH_RESOURCE = "/koncepts.yml";
    private static final String ATTR_DEFS_FILE = "koncept-definitions-file";
    private static final String ATTR_DEFS_CLASSPATH = "koncept-definitions-classpath";

    @Override
    public String process(Document document, String output) {
        // The pdf backend (asciidoctorj-pdf/Prawn) produces binary PDF output,
        // not HTML or DocBook text. The Postprocessor cannot modify it and
        // attempting to do so causes a JRuby type conversion error.
        String backend = document.getAttribute("backend", "html5").toString();
        if ("pdf".equals(backend)) {
            LOG.debug("Skipping glossary/colophon generation for pdf backend (Prawn)");
            return output;
        }

        // Build glossary content (may be empty if no koncept references)
        String glossaryContent = "";
        Map<String, KonceptEntry> rawRegistry = KonceptInlineMacro.removeRegistry(document);
        if (rawRegistry != null && !rawRegistry.isEmpty()) {
            Map<String, KonceptEntry> registry = new TreeMap<>(rawRegistry);
            LOG.debug("Generating glossary for {} referenced koncepts", registry.size());
            KonceptDefinitionSource defSource = resolveDefinitionSource(document);

            if ("docbook5".equals(backend) || "docbook".equals(backend)) {
                glossaryContent = buildGlossaryDocbook(registry, defSource);
            } else {
                glossaryContent = buildGlossaryHtml(registry, defSource);
            }
        }

        // Build colophon
        String colophonContent;
        if ("docbook5".equals(backend) || "docbook".equals(backend)) {
            colophonContent = buildColophonDocbook(document);
        } else {
            colophonContent = buildColophonHtml(document);
        }

        // Insert both before closing tag
        String combined = glossaryContent + colophonContent;
        if ("docbook5".equals(backend) || "docbook".equals(backend)) {
            int bookClose = output.lastIndexOf("</book>");
            if (bookClose >= 0) {
                return output.substring(0, bookClose) + combined + output.substring(bookClose);
            }
            return output + combined;
        } else {
            int bodyClose = output.lastIndexOf("</body>");
            if (bodyClose >= 0) {
                return output.substring(0, bodyClose) + combined + output.substring(bodyClose);
            }
            return output + combined;
        }
    }

    /**
     * Determine which definition source to use based on document attributes.
     * <p>
     * Always loads the pipeline's classpath {@code /koncepts.yml} as the base.
     * If a project-local file is specified via {@code :koncept-definitions-file:},
     * its definitions are layered on top — overriding any classpath entries
     * with the same identifier. This allows child projects to add
     * domain-specific concepts without modifying the pipeline.
     */
    private KonceptDefinitionSource resolveDefinitionSource(Document document) {
        // Always load the pipeline base from classpath
        KonceptDefinitionSource base;
        Object cpPath = document.getAttribute(ATTR_DEFS_CLASSPATH);
        if (cpPath != null && !cpPath.toString().isBlank()) {
            LOG.debug("Loading base koncept definitions from classpath: {}", cpPath);
            base = KonceptDefinitionSource.fromClasspath(cpPath.toString());
        } else {
            LOG.debug("Loading base koncept definitions from default classpath: {}",
                    DEFAULT_CLASSPATH_RESOURCE);
            base = KonceptDefinitionSource.fromClasspath(DEFAULT_CLASSPATH_RESOURCE);
        }

        // Check for project-local file overlay
        Object filePath = document.getAttribute(ATTR_DEFS_FILE);
        if (filePath != null && !filePath.toString().isBlank()) {
            LOG.debug("Loading project koncept definitions from file: {}", filePath);
            KonceptDefinitionSource overlay = KonceptDefinitionSource.fromFile(filePath.toString());
            // Overlay takes priority (listed first)
            return new CompositeKonceptDefinitionSource(List.of(overlay, base));
        }

        return base;
    }

    /**
     * Build the glossary section as complete HTML.
     */
    private String buildGlossaryHtml(
            Map<String, KonceptEntry> registry,
            KonceptDefinitionSource defSource) {

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"koncept-glossary-section\">\n");
        html.append("<h2 id=\"_referenced_koncepts\">Referenced Koncepts</h2>\n");
        html.append("<dl class=\"koncept-definitions\">\n");

        for (Map.Entry<String, KonceptEntry> entry : registry.entrySet()) {
            String id = entry.getKey();
            KonceptEntry konceptEntry = entry.getValue();
            Optional<KonceptDefinition> defOpt = defSource.lookup(id);

            String label = defOpt
                    .map(KonceptDefinition::label)
                    .orElse(KonceptInlineMacro.splitCamelCase(id));

            html.append("  <dt id=\"koncept-").append(escapeHtml(id)).append("\">");
            html.append("<strong>").append(escapeHtml(label)).append("</strong>");

            // Show reference count
            if (konceptEntry.getRefCount() > 1) {
                html.append(" <span class=\"koncept-ref-count\">(")
                    .append(konceptEntry.getRefCount())
                    .append(" references)</span>");
            }
            html.append("</dt>\n");

            html.append("  <dd>\n");

            if (defOpt.isPresent()) {
                KonceptDefinition def = defOpt.get();

                // Natural language definition
                if (def.definition() != null) {
                    html.append("    <p class=\"koncept-def-text\">")
                        .append(escapeHtml(def.definition()))
                        .append("</p>\n");
                }

                // Description logic axiom
                if (def.axiom() != null) {
                    html.append("    <p class=\"koncept-axiom\"><code>")
                        .append(escapeHtml(def.axiom()))
                        .append("</code></p>\n");
                }

                // Terminology identifiers
                if (def.sctid() != null || def.iri() != null) {
                    html.append("    <p class=\"koncept-ids\">");
                    if (def.sctid() != null) {
                        html.append("<span class=\"koncept-sctid\">SCTID: ")
                            .append(escapeHtml(def.sctid()))
                            .append("</span>");
                    }
                    if (def.iri() != null) {
                        if (def.sctid() != null) html.append(" | ");
                        html.append("<span class=\"koncept-iri\">IRI: ")
                            .append(escapeHtml(def.iri()))
                            .append("</span>");
                    }
                    html.append("</p>\n");
                }
            } else {
                html.append("    <p class=\"koncept-def-missing\">")
                    .append("<em>Definition not available.</em></p>\n");
            }

            html.append("  </dd>\n");
        }

        html.append("</dl>\n");
        html.append("</div>\n");

        return html.toString();
    }

    /**
     * Build the glossary section as DocBook markup.
     */
    private String buildGlossaryDocbook(
            Map<String, KonceptEntry> registry,
            KonceptDefinitionSource defSource) {

        StringBuilder db = new StringBuilder();
        db.append("<glossary xmlns=\"http://docbook.org/ns/docbook\" version=\"5.0\">\n");
        db.append("  <title>Referenced Koncepts</title>\n");

        for (Map.Entry<String, KonceptEntry> entry : registry.entrySet()) {
            String id = entry.getKey();
            Optional<KonceptDefinition> defOpt = defSource.lookup(id);

            String label = defOpt
                    .map(KonceptDefinition::label)
                    .orElse(KonceptInlineMacro.splitCamelCase(id));

            db.append("  <glossentry xml:id=\"koncept-").append(escapeXml(id)).append("\">\n");
            db.append("    <glossterm>").append(escapeXml(label)).append("</glossterm>\n");
            db.append("    <glossdef>\n");

            if (defOpt.isPresent()) {
                KonceptDefinition def = defOpt.get();
                if (def.definition() != null) {
                    db.append("      <para>").append(escapeXml(def.definition())).append("</para>\n");
                }
                if (def.axiom() != null) {
                    db.append("      <para><literal>").append(escapeXml(def.axiom())).append("</literal></para>\n");
                }
                if (def.sctid() != null) {
                    db.append("      <para>SCTID: ").append(escapeXml(def.sctid())).append("</para>\n");
                }
            } else {
                db.append("      <para>Definition not available.</para>\n");
            }

            db.append("    </glossdef>\n");
            db.append("  </glossentry>\n");
        }

        db.append("</glossary>\n");
        return db.toString();
    }

    // ── Colophon builders ─────────────────────────────────────────────

    /**
     * Describe the rendering pipeline using the ike-pdf-renderer attribute.
     * Falls back to a generic description based on the AsciiDoc backend.
     */
    private static String describePipeline(String backend, String renderer) {
        return switch (renderer) {
            case "prawn"      -> "AsciiDoc \u2192 PDF (asciidoctorj-pdf / Prawn)";
            case "prince"     -> "AsciiDoc \u2192 HTML5 \u2192 PDF (Prince XML)";
            case "ah"         -> "AsciiDoc \u2192 HTML5 \u2192 PDF (Antenna House)";
            case "weasyprint" -> "AsciiDoc \u2192 HTML5 \u2192 PDF (WeasyPrint)";
            case "xep"        -> "AsciiDoc \u2192 DocBook 5 \u2192 XSL-FO (Saxon-HE + DocBook XSL) \u2192 PDF (RenderX XEP)";
            case "fop"        -> "AsciiDoc \u2192 DocBook 5 \u2192 XSL-FO (Saxon-HE + DocBook XSL) \u2192 PDF (Apache FOP)";
            default           -> "AsciiDoc \u2192 " + backend;
        };
    }

    private String buildColophonHtml(Document document) {
        String backend = document.getAttribute("backend", "html5").toString();
        String renderer = attrStr(document, "ike-pdf-renderer", backend);
        String version = attrStr(document, "revnumber", null);
        String docDate = attrStr(document, "docdate", null);
        String buildTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"colophon\" style=\"margin-top:3em; padding-top:1em; ")
            .append("border-top:1px solid #ddd; font-size:0.85em; color:#666;\">\n");
        html.append("<h2 id=\"_colophon\" style=\"font-size:1.1em; color:#333;\">Colophon</h2>\n");
        html.append("<p>This document was produced using the IKE Documentation Pipeline.</p>\n");
        html.append("<table style=\"border-collapse:collapse; font-size:0.95em;\">\n");

        addColophonRowHtml(html, "Rendering pipeline", describePipeline(backend, renderer));
        addColophonRowHtml(html, "AsciiDoc backend", backend);
        if (version != null) {
            addColophonRowHtml(html, "Document version", version);
        }
        if (docDate != null) {
            addColophonRowHtml(html, "Document date", docDate);
        }
        addColophonRowHtml(html, "Build timestamp", buildTime);

        html.append("</table>\n");
        html.append("</div>\n");
        return html.toString();
    }

    private void addColophonRowHtml(StringBuilder html, String label, String value) {
        html.append("<tr><td style=\"padding:2px 12px 2px 0; font-weight:bold;\">")
            .append(escapeHtml(label))
            .append("</td><td style=\"padding:2px 0;\">")
            .append(escapeHtml(value))
            .append("</td></tr>\n");
    }

    private String buildColophonDocbook(Document document) {
        String backend = document.getAttribute("backend", "html5").toString();
        String renderer = attrStr(document, "ike-pdf-renderer", backend);
        String version = attrStr(document, "revnumber", null);
        String docDate = attrStr(document, "docdate", null);
        String buildTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        StringBuilder db = new StringBuilder();
        db.append("<colophon xmlns=\"http://docbook.org/ns/docbook\" version=\"5.0\">\n");
        db.append("  <title>Colophon</title>\n");
        db.append("  <para>This document was produced using the IKE Documentation Pipeline.</para>\n");
        db.append("  <informaltable frame=\"none\">\n");
        db.append("    <tgroup cols=\"2\">\n");
        db.append("      <colspec colwidth=\"1*\"/>\n");
        db.append("      <colspec colwidth=\"2*\"/>\n");
        db.append("      <tbody>\n");

        addColophonRowDocbook(db, "Rendering pipeline", describePipeline(backend, renderer));
        addColophonRowDocbook(db, "AsciiDoc backend", backend);
        if (version != null) {
            addColophonRowDocbook(db, "Document version", version);
        }
        if (docDate != null) {
            addColophonRowDocbook(db, "Document date", docDate);
        }
        addColophonRowDocbook(db, "Build timestamp", buildTime);

        // For DocBook/FO pipelines, add a placeholder row that the XSLT
        // stage replaces with the actual FO processor identity.
        if ("xep".equals(renderer) || "fop".equals(renderer)) {
            addColophonRowDocbook(db, "FO processor", "@@IKE_FO_PROCESSOR@@");
        }

        db.append("      </tbody>\n");
        db.append("    </tgroup>\n");
        db.append("  </informaltable>\n");
        db.append("</colophon>\n");
        return db.toString();
    }

    private void addColophonRowDocbook(StringBuilder db, String label, String value) {
        db.append("        <row>\n");
        db.append("          <entry><emphasis role=\"bold\">").append(escapeXml(label)).append("</emphasis></entry>\n");
        db.append("          <entry>").append(escapeXml(value)).append("</entry>\n");
        db.append("        </row>\n");
    }

    private static String attrStr(Document doc, String name, String defaultValue) {
        Object val = doc.getAttribute(name);
        if (val != null && !val.toString().isBlank()) {
            return val.toString();
        }
        return defaultValue;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
