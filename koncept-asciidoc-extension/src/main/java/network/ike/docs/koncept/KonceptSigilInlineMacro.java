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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
 * never a hand-copied asset that could drift. The bare Koncept kind deliberately has no
 * sigil ("Koncept bare, everything else marked", ike-issues#638) and renders nothing, with
 * a warning. On non-HTML backends the sigil degrades to its letter glyph, or the kind
 * name for the pentagon.
 * <p>
 * With a bracket label the macro renders a <em>specimen badge</em> instead of the bare
 * sigil — the full badge form of that kind carrying the given text, unlinked, because a
 * specimen shows what a badge of the kind looks like without claiming a glossary
 * reference. A {@code uuid} attribute names the specimen's real referent, and the badge
 * renders <em>in full</em> — sigil, identicon, label — honouring the anatomy rule that a
 * sigil never stands alone (ike-issues#742 amendment):
 * <pre>
 * koncept-sigil:description[label="Uninitialized Component (SOLOR)", uuid=6d3a2410-…]
 * koncept-sigil:semantic[label="Gretel", uuid=1a5b28fd-…]
 * koncept-sigil:stamp[label="Active · Inception · IKE Community", uuid=770cba9b-…]
 * </pre>
 * Without a {@code uuid} the chip degrades to sigil + label (legacy form). Letter kinds
 * render via {@link KonceptInlineMacro#renderSpecimenChip}; a stamp renders the
 * pentagon-and-provenance chip via {@link KonceptInlineMacro#renderStampSpecimenChip}
 * (or {@link KonceptSvgRenderer#renderStampSpecimen(String)} without a uuid). Keep
 * specimen text and uuids real — drawn from actual knowledge-base content — so the guide
 * never illustrates with invented values.
 * <p>
 * A bare sigil accepts a {@code scale} attribute ({@code koncept-sigil:stamp[scale=1.5]})
 * for legend and teaching contexts where the mark should read larger than badge scale;
 * the geometry is untouched — only the rendered box grows. Specimen chips size with the
 * surrounding text and ignore {@code scale}.
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
            LOG.warn("koncept-sigil: the bare Koncept kind has no sigil (target '{}')", target);
            return createPhraseNode(parent, "quoted", "", Map.of("subs", ":none"));
        }

        Document doc = parent.getDocument();
        String backend = doc.getAttribute("backend", "html5").toString();
        // label: the named attribute wins; positional 1 is the legacy bracket-label form. A
        // positional value containing '=' would have been parsed as a named attribute, so the
        // named form is required once uuid= appears.
        Object named = attributes.get("label");
        Object bracket = named != null ? named : attributes.get("1");
        String label = bracket != null && !bracket.toString().isBlank()
                ? bracket.toString().strip() : null;
        String idString = specimenIdString(attributes, target);
        String identity = label != null && idString != null
                ? label + " · " + attributes.get("uuid").toString().strip() : label;

        if (backend.startsWith("html")) {
            String rendered;
            if (label == null) {
                rendered = KonceptSvgRenderer.renderSigil(kind, scale(attributes, target));
            } else if (idString != null) {
                rendered = kind.isStamp()
                        ? KonceptInlineMacro.renderStampSpecimenChip(label, idString, identity)
                        : KonceptInlineMacro.renderSpecimenChip(kind, label, idString, identity);
            } else if (kind.isStamp()) {
                rendered = KonceptSvgRenderer.renderStampSpecimen(label);
            } else {
                rendered = KonceptInlineMacro.renderSpecimenChip(kind, label);
            }
            return createPhraseNode(parent, "quoted", rendered, Map.of("subs", ":none"));
        }

        String glyph = kind.hasLetterGlyph() ? kind.glyph() : kind.accessibleName();
        // Prawn PDF with a referent: re-processable AsciiDoc — the glyph, a native inline
        // image of the identicon PNG, then the label (the KonceptInlineMacro #836 idiom,
        // unlinked). The stamp's pentagon degrades to its ⬠ text glyph here.
        if ("pdf".equals(backend) && label != null && idString != null) {
            String mark = kind.isStamp() ? "⬠" : glyph;
            String content = mark + " image:" + IdenticonRenderer.pngFile(idString) + "["
                    + KonceptInlineMacro.attrValue(label) + ",18,18] " + label;
            return createPhraseNode(parent, "quoted", content,
                    Map.of("subs", "specialcharacters,macros"));
        }
        String fallback = label == null ? glyph : glyph + " " + label;
        return createPhraseNode(parent, "quoted", fallback, Map.of("subs", ":none"));
    }

    /**
     * The specimen referent's identicon idString from the {@code uuid} attribute (a single
     * UUID or a comma-joined PublicId array), or {@code null} when absent or malformed —
     * a malformed uuid degrades to the no-identicon specimen with a warning rather than
     * failing the build.
     *
     * @param attributes the macro's attributes
     * @param target     the macro target, for the warning
     * @return the idString, or {@code null}
     */
    private static String specimenIdString(Map<String, Object> attributes, String target) {
        Object raw = attributes.get("uuid");
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        try {
            List<UUID> uuids = new ArrayList<>();
            for (String part : raw.toString().split(",")) {
                uuids.add(UUID.fromString(part.strip()));
            }
            return KonceptIdentity.idString(uuids);
        } catch (RuntimeException e) {
            LOG.warn("koncept-sigil: unparseable uuid '{}' on {} — rendering without identicon",
                    raw, target);
            return null;
        }
    }

    /**
     * The bare sigil's display scale from the {@code scale} attribute: {@code 1.0} when
     * absent, clamped to a sane range, with a warning on an unparseable value.
     *
     * @param attributes the macro's attributes
     * @param target     the macro target, for the warning
     * @return the display scale
     */
    private static double scale(Map<String, Object> attributes, String target) {
        Object raw = attributes.get("scale");
        if (raw == null) {
            return 1.0;
        }
        try {
            double parsed = Double.parseDouble(raw.toString().trim());
            return Math.clamp(parsed, 0.5, 4.0);
        } catch (NumberFormatException e) {
            LOG.warn("koncept-sigil: unparseable scale '{}' on {} — using 1.0", raw, target);
            return 1.0;
        }
    }
}
