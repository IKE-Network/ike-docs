package network.ike.docs.koncept;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AsciidoctorJ block macro processor that renders a {@code kind: pattern} koncept's
 * shape as a real, captioned AsciiDoc table — one Model Feature (the referenced
 * component, then each declared field in order) per three-row group
 * (IKE-Network/ike-issues#883).
 * <p>
 * Usage in AsciiDoc:
 * <pre>
 * koncept-pattern-table::DescriptionPattern[]
 * </pre>
 * <p>
 * The target is a {@code koncepts.yml} identifier (the same key a {@code k:} reference
 * uses) of a {@code kind: pattern} koncept. The macro emits genuine AsciiDoc table
 * source ({@code .Caption} + {@code |===}) and hands it to
 * {@link #parseContent(StructuralNode, List)} — the same splice-and-re-parse mechanism
 * {@link KonceptNarrativeBlockMacro} uses — so Asciidoctor numbers the table
 * ("Table 2.1"), prose can cross-reference it via
 * {@code <<koncept-pattern-table-DescriptionPattern>>}, and the {@code k:} badges in
 * its cells resolve through the normal inline-macro pipeline. The glossary's
 * {@link KonceptGlossaryEntryRenderer#appendPatternShapeTable pattern-shape table}
 * renders the same design from the same {@link KonceptDefinition#modelFeatures()}
 * source; only this macro's tables are numbered and cross-referenceable, which is why
 * the two exist side by side.
 * <p>
 * Each Model Feature renders as three rows, with no separate row-label column:
 * <ol>
 *   <li>an italic label row spanning all three columns — {@code Referenced component}
 *       (with a generic explanatory phrase: the referenced component may be any
 *       component, not just a concept — {@code SemanticRecord.referencedComponentNid}
 *       has no type restriction) or {@code Field N};</li>
 *   <li>a content row of Meaning | Purpose | Data type badges (the referenced
 *       component has no data type — that cell renders an em dash);</li>
 *   <li>an italic {@code e.g.} row spanning all three columns with the feature's real
 *       example value — a live value from an actual semantic of this pattern, badged
 *       when it resolves to a curated koncept, plain text otherwise — omitted when the
 *       extraction found no example semantic.</li>
 * </ol>
 * Fields display 1-indexed ({@code Field 1} is {@code fieldDefinitions().get(0)}),
 * matching {@code SemanticVersionRecord.toString()}'s existing display convention over
 * 0-indexed storage; the referenced component is never numbered because it is not part
 * of the fields array at either the schema or the instance level. The
 * {@code koncept-pattern-table}, {@code koncept-model-feature}, and
 * {@code koncept-feature-example} roles are stylesheet hooks — the shared
 * {@code ike-doc-resources} docinfo mutes and indents them; without that CSS the label
 * and example rows still read correctly as italic.
 * <p>
 * A target with no pattern shape (a plain concept, or an unknown identifier) renders a
 * visible placeholder rather than silently vanishing, matching the
 * {@code koncept-narrative} convention.
 */
@Name("koncept-pattern-table")
public class KonceptPatternTableBlockMacro extends BlockMacroProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptPatternTableBlockMacro.class);

    /** Creates a new block macro processor instance. */
    public KonceptPatternTableBlockMacro() {
    }

    @Override
    public StructuralNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        Document doc = parent.getDocument();
        KonceptDefinitionSource defSource = KonceptDefinitions.forDocument(doc);
        Optional<KonceptDefinition> def = defSource.lookup(target)
                .filter(d -> !d.modelFeatures().isEmpty());

        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        if (def.isEmpty()) {
            LOG.warn("koncept-pattern-table: no pattern shape for {}", target);
            parseContent(container, List.of("_No pattern shape for `" + target + "`._"));
            return container;
        }
        parseContent(container, tableLines(target, def.get(), defSource));
        return container;
    }

    /**
     * Builds the AsciiDoc table source for one pattern: anchor, caption, header, and a
     * three-row group per Model Feature.
     *
     * @param target    the pattern's koncept identifier, used for the table's anchor
     * @param def       the pattern's definition (already confirmed to carry a shape)
     * @param defSource the document's definition source, used to decide whether an
     *                  example value resolves to a curated koncept
     * @return the table's AsciiDoc source lines
     */
    static List<String> tableLines(String target, KonceptDefinition def,
                                   KonceptDefinitionSource defSource) {
        List<String> lines = new ArrayList<>();
        lines.add("[[koncept-pattern-table-" + KonceptResolver.anchorSlug(target) + "]]");
        lines.add("." + def.label() + " shape");
        lines.add("[.koncept-pattern-table%header,cols=\"1,1,1\"]");
        lines.add("|===");
        lines.add("| Meaning | Purpose | Data type");
        lines.add("");

        for (KonceptDefinition.ModelFeature feature : def.modelFeatures()) {
            lines.add("3+e| [.koncept-model-feature]##" + feature.label() + "##");
            lines.add("| " + badgeOrDash(feature.meaning()) + " | " + badgeOrDash(feature.purpose())
                    + " | " + badgeOrDash(feature.dataType()));
            if (feature.example() != null && !feature.example().isBlank()) {
                lines.add("3+e| [.koncept-feature-example]##e.g. "
                        + exampleCell(feature.example(), defSource) + "##");
            }
            lines.add("");
        }

        lines.add("|===");
        return lines;
    }

    /** Renders a koncept identifier as a badge, or an em dash for {@code null}. */
    private static String badgeOrDash(String identifier) {
        return identifier != null ? "k:" + identifier + "[]" : "—";
    }

    /**
     * Renders an example value: a badge when it resolves to a curated koncept,
     * otherwise its plain display text with table-cell separators escaped.
     */
    private static String exampleCell(String example, KonceptDefinitionSource defSource) {
        if (defSource.lookup(example).isPresent()) {
            return "k:" + example + "[]";
        }
        return example.replace("|", "\\|");
    }
}
