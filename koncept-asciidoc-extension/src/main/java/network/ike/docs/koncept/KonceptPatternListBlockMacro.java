package network.ike.docs.koncept;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AsciidoctorJ block macro processor that renders a "List of Patterns": a flat,
 * numbered index of every {@code kind: pattern} koncept in the definition source,
 * alphabetical by label, each entry a plain link to that pattern's own entry in the
 * Koncept Glossary — no columns, no icons, styled the same as the document's own
 * table of contents ({@code ul.sectlevel1}) rather than as a data table.
 * <p>
 * Usage in AsciiDoc:
 * <pre>
 * koncept-pattern-list::[]
 * </pre>
 * <p>
 * The book convention this follows is a "List of Tables"/"List of Figures" page: an
 * index that only names and links each item, leaving the item's actual content (here,
 * the {@link KonceptGlossaryEntryRenderer#appendPatternShapeTable pattern-shape table})
 * to render where the item itself is defined. The target and attributes are unused;
 * the macro always lists every pattern the document's definition source can enumerate
 * (IKE-Network/ike-issues#880).
 */
@Name("koncept-pattern-list")
public class KonceptPatternListBlockMacro extends BlockMacroProcessor {

    /** Creates a new block macro processor instance. */
    public KonceptPatternListBlockMacro() {
    }

    @Override
    public StructuralNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        Document doc = parent.getDocument();
        KonceptDefinitionSource defSource = KonceptDefinitions.forDocument(doc);

        List<String> patternIds = new ArrayList<>();
        Collection<String> allIds = defSource.identifiers();
        for (String id : allIds) {
            Optional<KonceptDefinition> def = defSource.lookup(id);
            if (def.isPresent() && "pattern".equals(def.get().kind())) {
                patternIds.add(id);
            }
        }
        patternIds.sort(Comparator.comparing(
                id -> defSource.lookup(id).map(KonceptDefinition::label).orElse(id),
                String.CASE_INSENSITIVE_ORDER));

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"koncept-pattern-list\">\n")
            .append("  <div class=\"koncept-pattern-list-title\" ")
            .append("style=\"color:#7a2518;font-size:1.2em;margin-bottom:.8rem;\">")
            .append("List of Patterns</div>\n")
            .append("  <ul class=\"sectlevel1\">\n");

        int number = 1;
        for (String id : patternIds) {
            String label = defSource.lookup(id).map(KonceptDefinition::label)
                    .orElse(KonceptInlineMacro.splitCamelCase(id));
            html.append("    <li><a href=\"#koncept-").append(KonceptGlossaryEntryRenderer.escapeHtml(id))
                .append("\">").append(number++).append(". ")
                .append(KonceptGlossaryEntryRenderer.escapeHtml(label))
                .append("</a></li>\n");
        }
        html.append("  </ul>\n</div>\n");

        return createBlock(parent, "pass", html.toString(), new HashMap<>());
    }
}
