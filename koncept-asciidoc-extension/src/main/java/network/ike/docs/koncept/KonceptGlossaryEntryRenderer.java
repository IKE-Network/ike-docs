package network.ike.docs.koncept;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Renders one koncept's glossary entry as HTML — identicon, definition, axiom,
 * parent/child taxonomy chips, identifiers, and (when present) since/comments/
 * retired-comments/see-also/a pattern-shape table (present only for a
 * {@code kind: pattern} koncept — its referenced component plus every declared
 * field, each row naming meaning/purpose/data-type koncepts). Shared by
 * {@link KonceptGlossaryProcessor} (the referenced-only, default Postprocessor) and
 * {@link KonceptGlossaryTreeprocessor} (the grouped {@code koncept-glossary-all}
 * mode), so the two modes render one koncept identically wherever they overlap.
 */
final class KonceptGlossaryEntryRenderer {

    private KonceptGlossaryEntryRenderer() {
    }

    /**
     * Renders one {@code <dt>}/{@code <dd>} glossary entry.
     *
     * @param id            the koncept identifier
     * @param defOpt        the koncept's definition, if resolved
     * @param konceptEntry  the inline-reference registry entry, or {@code null} when
     *                      this koncept was never referenced inline (always {@code null}
     *                      in grouped/{@code koncept-glossary-all} mode, since tree
     *                      processing runs before the inline macro populates it)
     * @param childrenById  every known koncept's children, inverted from {@code broader}
     * @param defSource     the definition source, for resolving related koncepts
     * @return the entry's HTML
     */
    static String entryHtml(String id, Optional<KonceptDefinition> defOpt, KonceptEntry konceptEntry,
                             Map<String, List<String>> childrenById, KonceptDefinitionSource defSource) {
        StringBuilder html = new StringBuilder();

        String label = defOpt
                .map(KonceptDefinition::label)
                .orElse(KonceptInlineMacro.splitCamelCase(id));

        html.append("  <dt id=\"koncept-").append(escapeHtml(id)).append("\">");
        html.append(htmlIdenticonImg(defOpt));
        html.append("<strong>").append(escapeHtml(label)).append("</strong>");

        if (konceptEntry != null && konceptEntry.getRefCount() > 1) {
            html.append(" <span class=\"koncept-ref-count\">(")
                .append(konceptEntry.getRefCount())
                .append(" references)</span>");
        }
        html.append("</dt>\n");

        html.append("  <dd>\n");

        if (defOpt.isPresent()) {
            KonceptDefinition def = defOpt.get();

            if (def.definition() != null) {
                html.append("    <p class=\"koncept-def-text\">")
                    .append(escapeHtml(def.definition()))
                    .append("</p>\n");
            }

            if (def.axiom() != null) {
                html.append("    <p class=\"koncept-axiom\"><code>")
                    .append(escapeHtml(def.axiom()))
                    .append("</code></p>\n");
            }

            appendTaxonomyHtml(html, "Parents", def.broader(), defSource);
            appendTaxonomyHtml(html, "Children", childrenById.get(id), defSource);
            appendPatternShapeTable(html, def, defSource);

            if (def.since() != null) {
                html.append("    <p class=\"koncept-since\"><span class=\"koncept-history-label\">Since:</span> ")
                    .append(escapeHtml(def.since())).append("</p>\n");
            }
            if (!def.comments().isEmpty()) {
                html.append("    <div class=\"koncept-comments\"><span class=\"koncept-history-label\">")
                    .append("Comments:</span><ul>\n");
                for (String comment : def.comments()) {
                    html.append("      <li>").append(escapeHtml(comment)).append("</li>\n");
                }
                html.append("    </ul></div>\n");
            }
            if (!def.retiredComments().isEmpty()) {
                html.append("    <div class=\"koncept-retired-comments\">")
                    .append("<span class=\"koncept-history-label\">Retired comments:</span><ul>\n");
                for (KonceptDefinition.RetiredComment retired : def.retiredComments()) {
                    html.append("      <li>").append(escapeHtml(retired.text()));
                    if (retired.retiredAt() != null) {
                        html.append(" <span class=\"koncept-retired-at\">(retired ")
                            .append(escapeHtml(retired.retiredAt())).append(")</span>");
                    }
                    html.append("</li>\n");
                }
                html.append("    </ul></div>\n");
            }
            appendTaxonomyHtml(html, "See also", def.seeAlso(), defSource);

            html.append("    <p class=\"koncept-ids\">");
            List<String> parts = new ArrayList<>();
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
        return html.toString();
    }

    /** Build an HTML {@code <img>} for the koncept's identicon, or an empty string. */
    static String htmlIdenticonImg(Optional<KonceptDefinition> defOpt) {
        return KonceptIdentity.idString(defOpt.orElse(null))
                .map(idString -> "<img class=\"koncept-identicon koncept-glossary-icon\" src=\""
                        + IdenticonRenderer.dataUri(idString)
                        + "\" alt=\"\" style=\"height:1.3em;width:1.3em;vertical-align:-0.3em;"
                        + "border-radius:3px;image-rendering:pixelated;margin-right:0.4em;\"/> ")
                .orElse("");
    }

    /**
     * Append a taxonomy line (Parents, Children, or See also) as identicon chips
     * linking to each related koncept's glossary entry. No-op when the list is empty.
     */
    static void appendTaxonomyHtml(StringBuilder html, String heading,
                                    List<String> relatedIds, KonceptDefinitionSource defSource) {
        if (relatedIds == null || relatedIds.isEmpty()) {
            return;
        }
        html.append("    <p class=\"koncept-taxonomy koncept-taxonomy-")
            .append(heading.toLowerCase(Locale.ROOT).replace(' ', '-')).append("\">")
            .append("<span class=\"koncept-taxonomy-label\">").append(heading).append(":</span> ");
        List<String> chips = new ArrayList<>();
        for (String relId : relatedIds) {
            chips.add(chipHtml(relId, defSource));
        }
        html.append(String.join(" ", chips)).append("</p>\n");
    }

    /**
     * Append a {@code kind: pattern} koncept's shape as one HTML table: a first row for
     * the referenced component (the meaning/purpose a semantic of this pattern's own
     * referenced component carries — Tinkar's wire schema calls this pair
     * {@code ReferencedComponentMeaning}/{@code Purpose}; the Java entity API calls the
     * identical field {@code semanticMeaning}/{@code Purpose} — both names describe the
     * same field), followed by one row per declared field, each naming its own meaning,
     * purpose, and data-type koncepts as chips, in declared order. No-op for a concept
     * (both the referenced component and the field list are {@code null}/empty).
     */
    static void appendPatternShapeTable(StringBuilder html, KonceptDefinition def,
                                         KonceptDefinitionSource defSource) {
        boolean hasReferencedComponent =
                def.referencedComponentMeaning() != null || def.referencedComponentPurpose() != null;
        List<KonceptDefinition.PatternField> fields = def.fields();
        boolean hasFields = fields != null && !fields.isEmpty();
        if (!hasReferencedComponent && !hasFields) {
            return;
        }

        html.append("    <table class=\"koncept-pattern-shape\">\n")
            .append("      <caption>Pattern shape</caption>\n")
            .append("      <thead><tr><th>Role</th><th>Meaning</th><th>Purpose</th>")
            .append("<th>Data type</th></tr></thead>\n      <tbody>\n");

        if (hasReferencedComponent) {
            html.append("        <tr class=\"koncept-pattern-referenced-component\">")
                .append("<th scope=\"row\">Referenced component</th><td>")
                .append(def.referencedComponentMeaning() != null
                        ? chipHtml(def.referencedComponentMeaning(), defSource) : "&mdash;")
                .append("</td><td>")
                .append(def.referencedComponentPurpose() != null
                        ? chipHtml(def.referencedComponentPurpose(), defSource) : "&mdash;")
                .append("</td><td>&mdash;</td></tr>\n");
        }
        if (hasFields) {
            int index = 1;
            for (KonceptDefinition.PatternField field : fields) {
                html.append("        <tr><th scope=\"row\">Field ").append(index++).append("</th><td>")
                    .append(chipHtml(field.meaning(), defSource)).append("</td><td>")
                    .append(chipHtml(field.purpose(), defSource)).append("</td><td>")
                    .append(chipHtml(field.dataType(), defSource)).append("</td></tr>\n");
            }
        }
        html.append("      </tbody>\n    </table>\n");
    }

    /** Builds one identicon-chip link to a related koncept's glossary entry. */
    private static String chipHtml(String relId, KonceptDefinitionSource defSource) {
        Optional<KonceptDefinition> relDef = defSource.lookup(relId);
        String relLabel = relDef.map(KonceptDefinition::label)
                .orElse(KonceptInlineMacro.splitCamelCase(relId));
        String img = htmlIdenticonImg(relDef);
        return "<a href=\"#koncept-" + escapeHtml(relId)
                + "\" class=\"koncept-ref koncept-taxonomy-ref\" style=\"text-decoration:none;"
                + "white-space:nowrap;\">" + img + "<span class=\"koncept-taxonomy-name\" "
                + "style=\"font-variant:small-caps;color:#2a5a8a;\">" + escapeHtml(relLabel)
                + "</span></a>";
    }

    static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
