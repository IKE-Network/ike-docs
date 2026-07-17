package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptKind;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AsciidoctorJ block macro that renders the anatomy of a Koncept Badge as a real,
 * captioned figure: the referenced koncept's badge — kind sigil, identicon, name — drawn
 * large, with a muted part label beneath each part.
 * <p>
 * Usage in AsciiDoc, per the image standards (block title and alt text always present):
 * <pre>
 * .Anatomy of a Koncept Badge
 * koncept-badge-anatomy::DescriptionPattern[]
 * </pre>
 * <p>
 * The figure is composed at conversion time by {@link KonceptFigureRenderer} from the
 * same primitives the badges render with — the {@link KonceptKind} sigil glyph/colour
 * data and the {@link IdenticonRenderer} LifeHash image — so the illustration can never
 * drift from a real badge. The target resolves through {@link KonceptResolver} exactly
 * as a {@code k:} reference does; a target with no computable identity renders a visible
 * placeholder. The html family embeds a {@code data:} URI; file-referencing backends get
 * a content-addressed PNG file. Drawn at 2&times; and displayed at half width for
 * crispness.
 */
@Name("koncept-badge-anatomy")
public class KonceptBadgeAnatomyBlockMacro extends BlockMacroProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptBadgeAnatomyBlockMacro.class);

    /** Creates a new block macro processor instance. */
    public KonceptBadgeAnatomyBlockMacro() {
    }

    @Override
    public StructuralNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        Document doc = parent.getDocument();
        KonceptResolver.Resolved resolved = KonceptResolver.resolveTarget(doc, target, null);

        if (resolved.idString().isEmpty()) {
            LOG.warn("koncept-badge-anatomy: no computable identity for {}", target);
            StructuralNode container = createBlock(parent, "open", List.of());
            parseContent(container, List.of("_No computable identity for `" + target + "`._"));
            return container;
        }

        KonceptKind kind = resolved.kind();
        String idString = resolved.idString().get();
        String label = resolved.label();

        String backend = doc.getAttribute("backend", "html5").toString();
        boolean fileBackend = "pdf".equals(backend)
                || "docbook5".equals(backend) || "docbook".equals(backend);
        String imageTarget = fileBackend
                ? KonceptFigureRenderer.anatomyPngFile(kind, idString, label)
                : KonceptFigureRenderer.anatomyDataUri(kind, idString, label);

        Map<String, Object> imageAttrs = new HashMap<>();
        imageAttrs.put("target", imageTarget);
        imageAttrs.put("alt", "Anatomy of the " + label + " badge: kind sigil, identicon, name");
        imageAttrs.put("width", Integer.toString(displayWidth(kind, idString, label)));
        Block image = createBlock(parent, "image", "", imageAttrs);
        Object title = attributes.get("title");
        if (title != null && !title.toString().isBlank()) {
            image.setTitle(title.toString());
        }
        KonceptIdenticonFigureBlockMacro.assignFigureCaption(doc, image);
        return image;
    }

    /**
     * The figure's display width: the drawn PNG's pixel width at half scale (the
     * renderer draws at 2&times; for crispness).
     *
     * @param kind     the component kind
     * @param idString the identicon idString
     * @param label    the badge name
     * @return the display width in pixels
     */
    private static int displayWidth(KonceptKind kind, String idString, String label) {
        try {
            return ImageIO.read(new ByteArrayInputStream(
                    KonceptFigureRenderer.anatomyPng(kind, idString, label))).getWidth() / 2;
        } catch (IOException e) {
            LOG.warn("koncept-badge-anatomy: could not size the figure for {}", label, e);
            return 320;
        }
    }
}
