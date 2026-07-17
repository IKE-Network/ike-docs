package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptKind;
import network.ike.docs.konceptcore.StampSigilGeometry;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

/**
 * AsciidoctorJ inline macro that renders a component-kind sigil on its own — the mark a
 * Koncept Badge carries for its kind, without any badge around it — so prose that
 * <em>talks about</em> the sigil scheme (a "How to Read This Guide" front matter, a
 * tutorial) can show the real marks instead of describing them.
 * <p>
 * Usage in AsciiDoc:
 * <pre>
 * koncept-sigil:description[]   (amber D)
 * koncept-sigil:semantic[]      (green S)
 * koncept-sigil:pattern[]       (violet P)
 * koncept-sigil:stamp[]         (the grey pentagon)
 * koncept-sigil:unknown[]       (red ?)
 * </pre>
 * <p>
 * The target is a {@link KonceptKind} name, case-insensitive. The SVG comes from
 * {@link KonceptSvgRenderer#renderSigil(KonceptKind)}, which draws from the same locked
 * {@link KonceptKind} glyph/colour data and {@link StampSigilGeometry} pentagon the badge
 * renderers use — the sigil shown in prose is the sigil shown in badges, by construction,
 * never a hand-copied asset that could drift. The bare concept kind deliberately has no
 * sigil ("concept bare, everything else marked", ike-issues#638) and renders nothing, with
 * a warning. On non-HTML backends the sigil degrades to its letter glyph, or the kind
 * name for the pentagon.
 */
@Name("koncept-sigil")
public class KonceptSigilInlineMacro extends InlineMacroProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptSigilInlineMacro.class);

    /** Creates a new inline macro processor instance. */
    public KonceptSigilInlineMacro() {
    }

    @Override
    public PhraseNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        KonceptKind kind = KonceptKind.fromString(target);
        if (kind == KonceptKind.UNKNOWN
                && !"unknown".equals(target.trim().toLowerCase(Locale.ROOT))) {
            LOG.warn("koncept-sigil: unrecognised kind '{}' — rendering the unknown sigil", target);
        }
        if (kind.isBare()) {
            LOG.warn("koncept-sigil: the bare concept kind has no sigil (target '{}')", target);
            return createPhraseNode(parent, "quoted", "", Map.of("subs", ":none"));
        }

        Document doc = parent.getDocument();
        String backend = doc.getAttribute("backend", "html5").toString();
        if (backend.startsWith("html")) {
            return createPhraseNode(parent, "quoted",
                    KonceptSvgRenderer.renderSigil(kind), Map.of("subs", ":none"));
        }
        String fallback = kind.hasLetterGlyph() ? kind.glyph() : kind.accessibleName();
        return createPhraseNode(parent, "quoted", fallback, Map.of("subs", ":none"));
    }
}
