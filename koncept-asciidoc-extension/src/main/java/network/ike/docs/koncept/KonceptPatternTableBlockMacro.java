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
 * AsciidoctorJ block macro processor that renders a "Table of Patterns": one row per
 * {@code kind: pattern} koncept in the definition source, alphabetical by label, naming
 * its referenced-component meaning/purpose and its declared field count, each linking to
 * that pattern's own entry in the Koncept Glossary.
 * <p>
 * Usage in AsciiDoc:
 * <pre>
 * koncept-pattern-table::[]
 * </pre>
 * <p>
 * Intended as a dedicated index — analogous to a table of contents, but for patterns
 * specifically — placed early in the document so a reader can jump straight to any
 * pattern's full shape (rendered by {@link KonceptGlossaryEntryRenderer#appendPatternShapeTable}
 * wherever the glossary itself renders that pattern's entry). The target and attributes
 * are unused; the macro always lists every pattern the document's definition source can
 * enumerate (IKE-Network/ike-issues#880).
 */
@Name("koncept-pattern-table")
public class KonceptPatternTableBlockMacro extends BlockMacroProcessor {

    /** Creates a new block macro processor instance. */
    public KonceptPatternTableBlockMacro() {
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
        html.append("<table class=\"koncept-pattern-table\">\n")
            .append("  <caption>Table of Patterns</caption>\n")
            .append("  <thead><tr><th>Pattern</th><th>Referenced component meaning</th>")
            .append("<th>Referenced component purpose</th><th>Fields</th></tr></thead>\n")
            .append("  <tbody>\n");

        for (String id : patternIds) {
            KonceptDefinition def = defSource.lookup(id).orElseThrow();
            html.append("    <tr><td>")
                .append(KonceptGlossaryEntryRenderer.chipHtml(id, defSource))
                .append("</td><td>")
                .append(def.referencedComponentMeaning() != null
                        ? KonceptGlossaryEntryRenderer.chipHtml(def.referencedComponentMeaning(), defSource)
                        : "&mdash;")
                .append("</td><td>")
                .append(def.referencedComponentPurpose() != null
                        ? KonceptGlossaryEntryRenderer.chipHtml(def.referencedComponentPurpose(), defSource)
                        : "&mdash;")
                .append("</td><td>")
                .append(def.fields().size())
                .append("</td></tr>\n");
        }
        html.append("  </tbody>\n</table>\n");

        return createBlock(parent, "pass", html.toString(), new HashMap<>());
    }
}
