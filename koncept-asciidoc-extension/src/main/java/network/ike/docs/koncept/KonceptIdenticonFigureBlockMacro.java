package network.ike.docs.koncept;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AsciidoctorJ block macro that renders a koncept's identicon as a real, captioned
 * figure — for prose that <em>talks about</em> identicons (a "How to Read This Guide"
 * front matter) and needs to show one at figure size, not badge size.
 * <p>
 * Usage in AsciiDoc, per the image standards (block title and alt text always present):
 * <pre>
 * .The identicon of English Language (SOLOR)
 * koncept-identicon-figure::EnglishLanguage[width=96]
 * </pre>
 * <p>
 * The image is the same LifeHash PNG every badge embeds ({@link IdenticonRenderer},
 * byte-identical to Komet's) — never a hand-exported asset that could drift. The target
 * resolves through {@link KonceptResolver} exactly as a {@code k:} reference does; a
 * target with no computable identity renders a visible placeholder instead of a broken
 * figure. On the html family the figure embeds a {@code data:} URI (deployable output,
 * no loose files); file-referencing backends (DocBook&rarr;FO, Prawn) get the
 * content-addressed PNG file the inline badges already use. The {@code width} attribute
 * (default {@code 96}) sizes the display; the PNG itself stays at Komet's native
 * 128&times;128.
 */
@Name("koncept-identicon-figure")
public class KonceptIdenticonFigureBlockMacro extends BlockMacroProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptIdenticonFigureBlockMacro.class);

    /** Creates a new block macro processor instance. */
    public KonceptIdenticonFigureBlockMacro() {
    }

    @Override
    public StructuralNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        Document doc = parent.getDocument();
        KonceptResolver.Resolved resolved = KonceptResolver.resolveTarget(doc, target, null);

        if (resolved.idString().isEmpty()) {
            LOG.warn("koncept-identicon-figure: no computable identity for {}", target);
            StructuralNode container = createBlock(parent, "open", List.of());
            parseContent(container, List.of("_No computable identity for `" + target + "`._"));
            return container;
        }

        String backend = doc.getAttribute("backend", "html5").toString();
        boolean fileBackend = "pdf".equals(backend)
                || "docbook5".equals(backend) || "docbook".equals(backend);
        String imageTarget = fileBackend
                ? IdenticonRenderer.pngFile(resolved.idString().get())
                : IdenticonRenderer.dataUri(resolved.idString().get());

        Map<String, Object> imageAttrs = new HashMap<>();
        imageAttrs.put("target", imageTarget);
        imageAttrs.put("alt", resolved.label() + " identicon");
        imageAttrs.put("width", attributes.getOrDefault("width", "96").toString());
        Block image = createBlock(parent, "image", "", imageAttrs);
        Object title = attributes.get("title");
        if (title != null && !title.toString().isBlank()) {
            image.setTitle(title.toString());
        }
        assignFigureCaption(doc, image);
        return image;
    }

    /**
     * Numbers the figure like a parsed one: extension-created image blocks bypass the
     * parser's caption assignment, so mirror it — the block title then renders as
     * {@code Figure N. <title>}. The number is consumed regardless of title, so always
     * give these figures a block title (the image standards require one anyway).
     *
     * @param doc   the document whose figure counter advances
     * @param image the image block to caption
     */
    static void assignFigureCaption(Document doc, Block image) {
        image.setCaption(doc.getAttribute("figure-caption", "Figure").toString()
                + " " + doc.getAndIncrementCounter("figure-number") + ". ");
    }
}
