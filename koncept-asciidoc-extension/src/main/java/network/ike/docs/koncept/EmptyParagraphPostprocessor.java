package network.ike.docs.koncept;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AsciidoctorJ postprocessor that removes empty paragraph blocks from the
 * generated HTML.
 * <p>
 * IKE topics carry their index terms as standalone {@code indexterm:[...]}
 * lines after the section title, separated from the body by a blank line. In
 * the {@code html5} backend index terms produce no visible output, so that
 * blank-line-delimited block renders as an empty
 * {@code <div class="paragraph"><p></p></div>}. Sitting between a heading and
 * its first real paragraph, this zero-height block occupies the heading's
 * {@code page-break-after: avoid} "keep-with-next" slot in CSS Paged Media
 * (Prince, Antenna House, WeasyPrint): the heading binds to the empty
 * paragraph — which always fits — and the real content breaks to the next
 * page, stranding the heading alone at the page foot.
 * <p>
 * This processor strips those empty paragraphs so the heading binds to its
 * real first block. It is intentionally scoped to {@code html5}: the
 * DocBook&rarr;FO and Prawn PDF paths render index terms into a real index
 * (nothing to strip there) and use native keep-with-next on titles, so they
 * neither exhibit the orphan nor should lose their index entries.
 * <p>
 * Registered per-execution in the {@code asciidoctor-maven-plugin}
 * configuration (like {@link KonceptGlossaryProcessor}), never via SPI: an
 * SPI-registered Postprocessor crashes the Prawn backend in PostprocessorProxy
 * before any backend guard could run.
 */
public class EmptyParagraphPostprocessor extends Postprocessor {

    /** Creates a new empty-paragraph postprocessor instance. */
    public EmptyParagraphPostprocessor() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(EmptyParagraphPostprocessor.class);

    /**
     * Matches an asciidoctor paragraph block whose paragraph element is empty
     * or whitespace-only, plus any trailing whitespace so the surrounding
     * markup closes up cleanly. {@code \s*} cannot cross a {@code <}, so this
     * never spans into a non-empty paragraph.
     */
    private static final Pattern EMPTY_PARAGRAPH =
            Pattern.compile("<div class=\"paragraph\">\\s*<p>\\s*</p>\\s*</div>\\s*");

    /**
     * Strips empty paragraph blocks from the {@code html5} output; returns
     * other backends' output unchanged.
     *
     * @param document the converted document (used only to read the backend)
     * @param output   the rendered document output
     * @return the output with empty paragraph blocks removed (html5 only)
     */
    @Override
    public String process(Document document, String output) {
        String backend = document.getAttribute("backend", "html5").toString();
        if (!"html5".equals(backend)) {
            return output;
        }

        Matcher matcher = EMPTY_PARAGRAPH.matcher(output);
        StringBuilder sb = new StringBuilder(output.length());
        int removed = 0;
        while (matcher.find()) {
            matcher.appendReplacement(sb, "");
            removed++;
        }
        if (removed == 0) {
            return output;
        }
        matcher.appendTail(sb);
        LOG.debug("Removed {} empty paragraph(s) from html5 output", removed);
        return sb.toString();
    }
}
