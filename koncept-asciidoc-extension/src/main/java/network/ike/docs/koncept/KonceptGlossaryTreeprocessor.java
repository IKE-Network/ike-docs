package network.ike.docs.koncept;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.extension.Treeprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a real, TOC-visible AST section for the comprehensive ({@code koncept-glossary-all})
 * glossary mode (IKE-Network/ike-issues#877).
 * <p>
 * {@link KonceptGlossaryProcessor} — the pre-existing, referenced-only (default)
 * glossary — is a {@link org.asciidoctor.extension.Postprocessor}: it runs after
 * conversion, injecting the whole glossary as a raw HTML string appended to the
 * output. Asciidoctor's own table of contents is built from the parsed AST
 * <em>during</em> conversion, before a postprocessor ever runs, so that glossary
 * is structurally invisible to the TOC no matter how its content is grouped —
 * the root cause of an 88-page guide with a single TOC entry.
 * <p>
 * This treeprocessor runs during tree processing instead, so the {@link Section}
 * nodes it creates here are exactly what Asciidoctor's TOC-builder walks moments
 * later. Two shapes, both covering every known koncept (from
 * {@link KonceptDefinitionSource#identifiers()}):
 * <ul>
 *   <li><b>Default</b> — one flat "Koncept Glossary" section, alphabetical by
 *   label: a traditional glossary appendix. The intended companion to a
 *   hand-authored manuscript (separate chapters) that carries the narrative
 *   structure a reader actually wants — this treeprocessor's job is just
 *   comprehensive lookup, not organization.</li>
 *   <li><b>{@code koncept-glossary-grouped}</b> (set alongside
 *   {@code koncept-glossary-all}) — the original grouped shape: one child
 *   section per {@code section} field (computed against the live knowledge
 *   base by {@code KonceptExtractor} in tinkar-core, not derived here), titled
 *   with its group's own koncept label when the group is a genuine taxonomy
 *   subtree, opening with that koncept's own definition as the section's
 *   narrative. Kept as an opt-in for whoever wants taxonomy-shaped browsing
 *   instead of (or alongside, via a separate render) the flat default.</li>
 * </ul>
 * <p>
 * No-ops unless {@code koncept-glossary-all} is set, and skips the {@code pdf}
 * (Prawn) and DocBook backends — the HTML5 family first; {@link KonceptGlossaryProcessor}
 * keeps rendering the referenced-only (default) glossary for every backend, and
 * keeps owning {@code koncept-glossary-all} for DocBook too (it never fully
 * supported "all" there either — {@code buildGlossaryDocbook} always used the
 * referenced-only registry — so this treeprocessor isn't ceding anything new).
 * <p>
 * Registered via SPI ({@link KonceptExtensionRegistry}) like {@link FrontBackMatterTreeprocessor}:
 * a plain AST-only treeprocessor is safe for every backend, including Prawn — the
 * backend guard below is a scope choice, not a crash-avoidance requirement.
 */
public final class KonceptGlossaryTreeprocessor extends Treeprocessor {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptGlossaryTreeprocessor.class);
    private static final Pattern RESIDUAL_KEY = Pattern.compile("Residual(\\d+)");
    private static final String RAN_MARKER = "koncept-glossary-treeprocessor-ran";

    /** Creates the treeprocessor. */
    public KonceptGlossaryTreeprocessor() {
    }

    @Override
    public Document process(Document document) {
        String backend = document.getAttribute("backend", "html5").toString();
        if (!document.hasAttribute("koncept-glossary-all")
                || "pdf".equals(backend) || "docbook5".equals(backend) || "docbook".equals(backend)) {
            return document;
        }
        // Asciidoctor invokes a treeprocessor more than once for one document in some
        // pipelines (confirmed empirically) -- this guard makes building new AST content
        // idempotent, unlike FrontBackMatterTreeprocessor's role-toggle, which is naturally
        // idempotent and never needed one.
        if (!document.setAttribute(RAN_MARKER, true, false)) {
            return document;
        }

        KonceptDefinitionSource defSource = KonceptDefinitions.forDocument(document);
        Collection<String> allIds = defSource.identifiers();
        if (allIds.isEmpty()) {
            return document;
        }

        if (document.hasAttribute("koncept-glossary-grouped")) {
            buildGrouped(document, allIds, defSource);
        } else {
            buildFlat(document, allIds, defSource);
        }
        return document;
    }

    /**
     * The default shape: one flat "Koncept Glossary" section, every known koncept in
     * one {@code <dl>}, alphabetical by label — a traditional glossary appendix. No
     * grouping, no per-entry narrative; a hand-authored manuscript is expected to carry
     * the narrative structure separately.
     */
    private void buildFlat(Document document, Collection<String> allIds, KonceptDefinitionSource defSource) {
        Map<String, List<String>> childrenById = KonceptGraph.invertBroader(allIds, defSource);
        List<String> sortedIds = new ArrayList<>(allIds);
        sortedIds.sort(Comparator.comparing(
                id -> defSource.lookup(id).map(KonceptDefinition::label).orElse(id),
                String.CASE_INSENSITIVE_ORDER));

        Section glossary = createSection(document, 1, true, Map.of());
        glossary.setTitle("Koncept Glossary");
        glossary.setId(slugify(glossary.getTitle()));
        document.append(glossary);

        StringBuilder html = new StringBuilder();
        html.append("<dl class=\"koncept-definitions\">\n");
        for (String id : sortedIds) {
            html.append(KonceptGlossaryEntryRenderer.entryHtml(
                    id, defSource.lookup(id), null, childrenById, defSource));
        }
        html.append("</dl>\n");

        Block block = createBlock(glossary, "pass", html.toString(), Map.of());
        glossary.append(block);

        LOG.debug("Built flat, alphabetical glossary covering {} koncepts", sortedIds.size());
    }

    /**
     * The opt-in shape ({@code koncept-glossary-grouped}): one child section per
     * {@code section} field, titled and narrated by its root koncept when the group is
     * a genuine taxonomy subtree.
     */
    private void buildGrouped(Document document, Collection<String> allIds, KonceptDefinitionSource defSource) {
        Map<String, List<String>> childrenById = KonceptGraph.invertBroader(allIds, defSource);
        Map<String, List<String>> idsBySection = new TreeMap<>();
        for (String id : allIds) {
            String section = defSource.lookup(id).map(KonceptDefinition::section).orElse(null);
            idsBySection.computeIfAbsent(section != null ? section : "Unclassified", key -> new ArrayList<>()).add(id);
        }

        List<String> groupKeys = new ArrayList<>(idsBySection.keySet());
        groupKeys.sort(Comparator.comparing(key -> groupTitle(key, defSource)));

        Section glossary = createSection(document, 1, true, Map.of());
        glossary.setTitle("Koncept Glossary");
        glossary.setId(slugify(glossary.getTitle()));
        document.append(glossary);

        for (String groupKey : groupKeys) {
            List<String> ids = idsBySection.get(groupKey);
            Section group = createSection(glossary, 2, true, Map.of());
            group.setTitle(groupTitle(groupKey, defSource));
            group.setId(slugify(group.getTitle()));

            StringBuilder html = new StringBuilder();
            Optional<KonceptDefinition> rootDef = defSource.lookup(groupKey);
            if (rootDef.isPresent() && rootDef.get().definition() != null) {
                html.append("<p class=\"koncept-section-narrative\">")
                        .append(KonceptGlossaryEntryRenderer.escapeHtml(rootDef.get().definition()))
                        .append("</p>\n");
            }
            html.append("<dl class=\"koncept-definitions\">\n");
            for (String id : ids) {
                html.append(KonceptGlossaryEntryRenderer.entryHtml(
                        id, defSource.lookup(id), null, childrenById, defSource));
            }
            html.append("</dl>\n");

            Block block = createBlock(group, "pass", html.toString(), Map.of());
            group.append(block);
            glossary.append(group);
        }

        LOG.debug("Built {} TOC-visible glossary group(s) covering {} koncepts", groupKeys.size(), allIds.size());
    }

    /**
     * A group's display title: the root koncept's own label when {@code groupKey}
     * resolves to one (a genuine taxonomy-subtree section), or a readable name for
     * the positional {@code ResidualN}/{@code Unclassified} catch-all buckets
     * {@code KonceptExtractor} falls back to when a member isn't reachable from any
     * single taxonomy root.
     */
    private static String groupTitle(String groupKey, KonceptDefinitionSource defSource) {
        Optional<String> label = defSource.lookup(groupKey).map(KonceptDefinition::label);
        if (label.isPresent()) {
            return label.get();
        }
        Matcher residual = RESIDUAL_KEY.matcher(groupKey);
        if (residual.matches()) {
            return "Additional Koncepts (" + residual.group(1) + ")";
        }
        return "Unclassified".equals(groupKey) ? "Unclassified Koncepts" : groupKey;
    }

    /**
     * Slugifies a title into an anchor id matching this pipeline's {@code idprefix=""}/
     * {@code idseparator="-"} convention ({@code AsciidocMojo}) — the same shape Asciidoctor's
     * native parser auto-assigns to author-written {@code == Title} sections. A section built
     * via the extension API (like this treeprocessor's) gets no such auto-id, so without this,
     * the TOC link falls back to a bare {@code href="#"} that a PDF renderer's
     * {@code target-counter} cannot resolve, corrupting every page number after it.
     *
     * @param title the section title
     * @return the slugified id
     */
    private static String slugify(String title) {
        String slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("^-+|-+$", "");
    }
}
