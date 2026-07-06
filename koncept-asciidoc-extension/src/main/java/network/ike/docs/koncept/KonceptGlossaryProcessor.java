package network.ike.docs.koncept;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
 *   <li>An anchor target matching the inline identicon links</li>
 *   <li>The concept's Komet identicon (when a PublicId resolves)</li>
 *   <li>Natural language definition</li>
 *   <li>Description logic axiom in Unicode notation</li>
 *   <li>Optional SNOMED CT identifier</li>
 *   <li>Optional OWL IRI</li>
 * </ul>
 * <p>
 * The colophon describes the rendering pipeline used to produce the document,
 * including the AsciiDoc backend, build timestamp, and document version.
 * <p>
 * Definitions are resolved via {@link KonceptDefinitions#forDocument}, the
 * same source the inline macro uses, so the YAML is parsed once per document.
 */
public class KonceptGlossaryProcessor extends Postprocessor {

    /** Creates a new glossary processor instance. */
    public KonceptGlossaryProcessor() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(KonceptGlossaryProcessor.class);

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
        Map<String, KonceptEntry> registry =
                rawRegistry != null ? new TreeMap<>(rawRegistry) : new TreeMap<>();
        KonceptDefinitionSource defSource = resolveDefinitionSource(document);

        // A comprehensive glossary (koncept-glossary-all) lists every known koncept,
        // not only those referenced in the document — the standard reference for a
        // knowledge set. Otherwise only referenced koncepts appear (the default).
        boolean glossaryAll = document.hasAttribute("koncept-glossary-all");
        java.util.Set<String> ids = new java.util.TreeSet<>(registry.keySet());
        if (glossaryAll) {
            ids.addAll(defSource.identifiers());
        }

        // Invert broader into children across every known definition.
        Map<String, java.util.List<String>> childrenById = new java.util.TreeMap<>();
        for (String id : defSource.identifiers()) {
            for (String parent : defSource.lookup(id).map(KonceptDefinition::broader)
                    .orElse(java.util.List.of())) {
                childrenById.computeIfAbsent(parent, k -> new java.util.ArrayList<>()).add(id);
            }
        }

        if (!ids.isEmpty()) {
            LOG.debug("Generating glossary for {} koncepts (all={})", ids.size(), glossaryAll);
            if ("docbook5".equals(backend) || "docbook".equals(backend)) {
                glossaryContent = buildGlossaryDocbook(registry, defSource);
            } else {
                glossaryContent = buildGlossaryHtml(ids, registry, childrenById, defSource);
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
     * Determine which definition source to use. Delegates to
     * {@link KonceptDefinitions#forDocument}, which loads the classpath
     * {@code /koncepts.yml} base plus any {@code :koncept-definitions-file:}
     * overlay and caches the result per document — shared with the inline macro.
     */
    private KonceptDefinitionSource resolveDefinitionSource(Document document) {
        return KonceptDefinitions.forDocument(document);
    }

    /**
     * Build an HTML {@code <img>} for the koncept's identicon, or an empty
     * string when no PublicId resolves.
     */
    private String htmlIdenticonImg(Optional<KonceptDefinition> defOpt) {
        // Self-contained inline styles (no koncept.css dependency); a larger
        // identicon than the inline chip, sized in em so it tracks the term.
        return KonceptIdentity.idString(defOpt.orElse(null))
                .map(idString -> "<img class=\"koncept-identicon koncept-glossary-icon\" src=\""
                        + IdenticonRenderer.dataUri(idString)
                        + "\" alt=\"\" style=\"height:1.3em;width:1.3em;vertical-align:-0.3em;"
                        + "border-radius:3px;image-rendering:pixelated;margin-right:0.4em;\"/> ")
                .orElse("");
    }

    /**
     * Build a DocBook {@code <inlinemediaobject>} for the koncept's identicon,
     * or an empty string when no PublicId resolves.
     */
    private String docbookIdenticonImg(Optional<KonceptDefinition> defOpt) {
        return KonceptIdentity.idString(defOpt.orElse(null))
                .map(idString -> {
                    String uri = new File(IdenticonRenderer.pngFile(idString)).toURI().toString();
                    return "<inlinemediaobject><imageobject><imagedata fileref=\""
                            + escapeXml(uri) + "\" format=\"PNG\""
                            + " contentwidth=\"16pt\" contentdepth=\"16pt\"/></imageobject>"
                            + "</inlinemediaobject> ";
                })
                .orElse("");
    }

    /**
     * Build the glossary section as complete HTML: one entry per koncept in
     * {@code ids} (referenced, or all known when {@code koncept-glossary-all} is set),
     * each with its definition, logical axiom, parents and children (as identicon-linked
     * chips), and all identifiers (UUID, koncept identifier, SCTID, IRI).
     */
    private String buildGlossaryHtml(
            java.util.Set<String> ids,
            Map<String, KonceptEntry> registry,
            Map<String, java.util.List<String>> childrenById,
            KonceptDefinitionSource defSource) {

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"koncept-glossary-section\">\n");
        html.append("<h2 id=\"_referenced_koncepts\">Referenced Koncepts</h2>\n");
        html.append("<dl class=\"koncept-definitions\">\n");

        for (String id : ids) {
            Optional<KonceptDefinition> defOpt = defSource.lookup(id);
            KonceptEntry konceptEntry = registry.get(id);

            String label = defOpt
                    .map(KonceptDefinition::label)
                    .orElse(KonceptInlineMacro.splitCamelCase(id));

            html.append("  <dt id=\"koncept-").append(escapeHtml(id)).append("\">");
            html.append(htmlIdenticonImg(defOpt));
            html.append("<strong>").append(escapeHtml(label)).append("</strong>");

            // Show reference count
            if (konceptEntry != null && konceptEntry.getRefCount() > 1) {
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

                // Taxonomy: parents and children as identicon chips linked to their entries.
                appendTaxonomyHtml(html, "Parents", def.broader(), defSource);
                appendTaxonomyHtml(html, "Children", childrenById.get(id), defSource);

                // Identifiers: koncept id, UUID(s), SCTID, IRI.
                html.append("    <p class=\"koncept-ids\">");
                java.util.List<String> parts = new java.util.ArrayList<>();
                parts.add("<span class=\"koncept-identifier\">id: <code>"
                        + escapeHtml(def.identifier()) + "</code></span>");
                if (def.uuids() != null && !def.uuids().isEmpty()) {
                    parts.add("<span class=\"koncept-uuid\">UUID: <code>"
                            + escapeHtml(def.uuids().get(0)) + "</code></span>");
                }
                if (def.sctid() != null) {
                    parts.add("<span class=\"koncept-sctid\">SCTID: " + escapeHtml(def.sctid()) + "</span>");
                }
                if (def.iri() != null) {
                    parts.add("<span class=\"koncept-iri\">IRI: " + escapeHtml(def.iri()) + "</span>");
                }
                html.append(String.join(" | ", parts)).append("</p>\n");
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
     * Append a taxonomy line (Parents or Children) as identicon chips linking to each
     * related koncept's glossary entry. No-op when the list is empty.
     */
    private void appendTaxonomyHtml(StringBuilder html, String heading,
                                    java.util.List<String> relatedIds,
                                    KonceptDefinitionSource defSource) {
        if (relatedIds == null || relatedIds.isEmpty()) {
            return;
        }
        html.append("    <p class=\"koncept-taxonomy koncept-taxonomy-")
            .append(heading.toLowerCase(java.util.Locale.ROOT)).append("\">")
            .append("<span class=\"koncept-taxonomy-label\">").append(heading).append(":</span> ");
        java.util.List<String> chips = new java.util.ArrayList<>();
        for (String relId : relatedIds) {
            Optional<KonceptDefinition> relDef = defSource.lookup(relId);
            String relLabel = relDef.map(KonceptDefinition::label)
                    .orElse(KonceptInlineMacro.splitCamelCase(relId));
            String img = htmlIdenticonImg(relDef);
            chips.add("<a href=\"#koncept-" + escapeHtml(relId)
                    + "\" class=\"koncept-ref koncept-taxonomy-ref\" style=\"text-decoration:none;"
                    + "white-space:nowrap;\">" + img + "<span class=\"koncept-taxonomy-name\" "
                    + "style=\"font-variant:small-caps;color:#2a5a8a;\">" + escapeHtml(relLabel)
                    + "</span></a>");
        }
        html.append(String.join(" ", chips)).append("</p>\n");
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
            db.append("    <glossterm>").append(docbookIdenticonImg(defOpt))
              .append(escapeXml(label)).append("</glossterm>\n");
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
            case "prawn"      -> "AsciiDoc → PDF (asciidoctorj-pdf / Prawn)";
            case "prince"     -> "AsciiDoc → HTML5 → PDF (Prince XML)";
            case "ah"         -> "AsciiDoc → HTML5 → PDF (Antenna House)";
            case "weasyprint" -> "AsciiDoc → HTML5 → PDF (WeasyPrint)";
            case "xep"        -> "AsciiDoc → DocBook 5 → XSL-FO (Saxon-HE + DocBook XSL) → PDF (RenderX XEP)";
            case "fop"        -> "AsciiDoc → DocBook 5 → XSL-FO (Saxon-HE + DocBook XSL) → PDF (Apache FOP)";
            default           -> "AsciiDoc → " + backend;
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
