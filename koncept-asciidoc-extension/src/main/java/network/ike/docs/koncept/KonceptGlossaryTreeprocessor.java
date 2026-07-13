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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a real, TOC-visible AST section per taxonomy group for the comprehensive
 * ({@code koncept-glossary-all}) glossary mode (IKE-Network/ike-issues#877).
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
 * later. Every known koncept (from {@link KonceptDefinitionSource#identifiers()})
 * is grouped by its {@code section} field — computed against the live knowledge
 * base by {@code KonceptExtractor} (tinkar-core), not derived here — into one
 * child section per group, nested under one "Koncept Glossary" parent, each
 * titled with its group's own koncept label when the group is a genuine taxonomy
 * subtree, opening with that koncept's own definition as the section's narrative.
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

        Map<String, List<String>> childrenById = invertBroader(allIds, defSource);
        Map<String, List<String>> idsBySection = new TreeMap<>();
        for (String id : allIds) {
            String section = defSource.lookup(id).map(KonceptDefinition::section).orElse(null);
            idsBySection.computeIfAbsent(section != null ? section : "Unclassified", key -> new ArrayList<>()).add(id);
        }

        List<String> groupKeys = new ArrayList<>(idsBySection.keySet());
        groupKeys.sort(Comparator.comparing(key -> groupTitle(key, defSource)));

        Section glossary = createSection(document, 1, true, Map.of());
        glossary.setTitle("Koncept Glossary");
        document.append(glossary);

        for (String groupKey : groupKeys) {
            List<String> ids = idsBySection.get(groupKey);
            Section group = createSection(glossary, 2, true, Map.of());
            group.setTitle(groupTitle(groupKey, defSource));

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
        return document;
    }

    /** Inverts every known koncept's {@code broader} parents into a children map. */
    private static Map<String, List<String>> invertBroader(Collection<String> allIds, KonceptDefinitionSource defSource) {
        Map<String, List<String>> childrenById = new TreeMap<>();
        for (String id : allIds) {
            for (String parent : defSource.lookup(id).map(KonceptDefinition::broader).orElse(List.of())) {
                childrenById.computeIfAbsent(parent, key -> new ArrayList<>()).add(id);
            }
        }
        return childrenById;
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
}
