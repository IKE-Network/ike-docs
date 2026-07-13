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
 * AsciidoctorJ block macro processor that splices a koncept's curated, long-form
 * {@code narrative} field into the document as real AsciiDoc source.
 * <p>
 * Usage in AsciiDoc:
 * <pre>
 * koncept-narrative::Axioms[]
 * </pre>
 * <p>
 * The target is a {@code koncepts.yml} identifier (the same key a {@code k:}
 * reference uses); its {@code narrative} field — AsciiDoc source, including
 * embedded {@code k:} chip references — is split into lines and handed to
 * {@link #parseContent(StructuralNode, List)} so it is re-parsed as real
 * AsciiDoc (embedded chips resolve through the normal macro pipeline), not
 * frozen as pre-rendered text. This is the KB-native alternative to hand-typed
 * manuscript prose (IKE-Network/ike-issues#879): the same content lives once,
 * in the knowledge base, and is rendered here rather than copied.
 * <p>
 * A koncept with no {@code narrative} field renders a visible placeholder
 * rather than silently vanishing, matching the glossary's own
 * "Definition not available" convention for a missing field.
 */
@Name("koncept-narrative")
public class KonceptNarrativeBlockMacro extends BlockMacroProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptNarrativeBlockMacro.class);

    /** Creates a new block macro processor instance. */
    public KonceptNarrativeBlockMacro() {
    }

    @Override
    public StructuralNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        Document doc = parent.getDocument();
        Optional<String> narrative = KonceptDefinitions.forDocument(doc)
                .lookup(target)
                .map(KonceptDefinition::narrative);

        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        if (narrative.isEmpty() || narrative.get().isBlank()) {
            LOG.warn("koncept-narrative: no narrative content for {}", target);
            parseContent(container, List.of("_No curated narrative for `" + target + "`._"));
            return container;
        }
        parseContent(container, List.of(narrative.get().split("\n", -1)));
        return container;
    }
}
