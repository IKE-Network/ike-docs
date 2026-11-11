package network.ike.docs.plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moves a document's abstract onto the title page of the print HTML.
 *
 * <p>An author marks an abstract with AsciiDoc's own {@code abstract} style
 * on a paragraph or an open block that comes before the document's first
 * section. The {@code html5} backend writes that block into the preamble,
 * inside {@code <div id="content">}, after the header has closed. The print
 * stylesheet gives {@code <div id="header">} the title page and starts the
 * body, and page 1, at {@code #content}, so a stylesheet alone cannot print
 * the abstract beneath the title.
 *
 * <p>This class rewrites the print HTML before a CSS renderer reads it. The
 * abstract is taken out of the preamble and written into the header, after
 * the title, the author and the date and before the table of contents. It is
 * written there as an ordinary open block, so its paragraphs and lists are
 * set by the same rules as the body's text. The stylesheet
 * ({@code ike-print.css}) aligns it left and gives the pages it runs on to a
 * lowercase roman folio. A preamble that is left with nothing that prints is
 * taken out with the abstract, so the body begins at the first block after
 * the abstract.
 *
 * <p>The rewrite happens here, and not in the docinfo script, because the
 * build runs Prince with scripts off, and WeasyPrint runs none. The screen
 * HTML is a separate output and is not touched.
 *
 * <p>Nothing is moved when the document has no title, when it has no
 * abstract before its first section, or when nothing follows the abstract.
 * The HTML is then returned as the same {@link String}. A second pass over
 * HTML that was already rewritten finds nothing to move.
 *
 * <p>Design: {@code dev-document-front-matter} in {@code ike-lab-documents};
 * IKE-Network/ike-issues#1307.
 */
final class TitlePageAbstract {

    /** The class of the block that holds the abstract on the title page. */
    static final String TITLE_ABSTRACT_CLASS = "ike-title-abstract";

    private static final String HEADER_OPEN = "<div id=\"header\">";
    private static final String CONTENT_OPEN = "<div id=\"content\">";
    private static final String PREAMBLE_OPEN = "<div id=\"preamble\">";
    private static final String SECTIONBODY_OPEN = "<div class=\"sectionbody\">";
    private static final String DIV_CLOSE = "</div>";

    /** An opening or a closing {@code div} tag. */
    private static final Pattern DIV_TAG =
            Pattern.compile("<div\\b[^>]*>|</div\\s*>");

    /**
     * The opening tag of an abstract block, as the {@code html5} backend
     * writes it: an optional id, then the classes {@code quoteblock abstract}
     * and any roles.
     */
    private static final Pattern ABSTRACT_OPEN = Pattern.compile(
            "<div(?: id=\"([^\"]*)\")? class=\"quoteblock abstract(?: [^\"]*)?\">");

    /** The table of contents, when the backend writes it into the header. */
    private static final Pattern TOC_OPEN =
            Pattern.compile("<div id=\"toc\"[^>]*>");

    /** The parts of an abstract block: an optional title, then the quote. */
    private static final Pattern ABSTRACT_PARTS = Pattern.compile(
            "\\s*(<div class=\"title\">.*?</div>)?\\s*<blockquote>(.*)</blockquote>\\s*",
            Pattern.DOTALL);

    /**
     * How the content of a block that holds other blocks begins. An abstract
     * written on a paragraph has plain text here, and is wrapped as a
     * paragraph.
     */
    private static final Pattern BLOCK_START = Pattern.compile(
            "<(?:div|table|hr|details|h[1-6]|ul|ol|dl|p|pre|blockquote|figure|section)\\b");

    /** What a preamble may still hold and print nothing. */
    private static final Pattern NOT_PRINTED = Pattern.compile(
            "<style\\b.*?</style>|<script\\b.*?</script>|<!--.*?-->",
            Pattern.DOTALL);

    private TitlePageAbstract() {
    }

    /**
     * Move the abstract of an {@code html5} document into its header.
     *
     * @param html the document as the {@code html5} backend wrote it
     * @return the document with its abstract in the header, or {@code html}
     *         itself when there is nothing to move
     */
    static String relocate(String html) {
        int header = html.indexOf(HEADER_OPEN);
        if (header < 0) {
            return html;
        }
        int headerBody = header + HEADER_OPEN.length();
        int headerClose = closeOf(html, headerBody);
        if (headerClose < 0) {
            return html;
        }
        // A document with no title has an empty header and no title page.
        if (!html.substring(headerBody, headerClose).contains("<h1")) {
            return html;
        }
        int content = html.indexOf(CONTENT_OPEN, headerClose);
        if (content < 0) {
            return html;
        }

        // The blocks before the first section are in the preamble. A document
        // with no section has no preamble, and its blocks are in #content.
        int container = content + CONTENT_OPEN.length();
        int preamble = -1;
        int afterContent = skipWhitespace(html, container);
        if (html.startsWith(PREAMBLE_OPEN, afterContent)) {
            int afterPreamble = skipWhitespace(html, afterContent + PREAMBLE_OPEN.length());
            if (html.startsWith(SECTIONBODY_OPEN, afterPreamble)) {
                preamble = afterContent;
                container = afterPreamble + SECTIONBODY_OPEN.length();
            }
        }
        int containerClose = closeOf(html, container);
        if (containerClose < 0) {
            return html;
        }

        Matcher open = childMatching(html, container, containerClose, ABSTRACT_OPEN);
        if (open == null) {
            return html;
        }
        int abstractStart = open.start();
        int abstractClose = closeOf(html, open.end());
        if (abstractClose < 0) {
            return html;
        }
        int abstractEnd = abstractClose + DIV_CLOSE.length();

        // An abstract with nothing after it is not front matter: there is no
        // body for it to come before. Only a document with no section can be
        // in that state, because a preamble is followed by a section.
        if (preamble < 0 && html.substring(abstractEnd, containerClose).isBlank()) {
            return html;
        }

        Matcher parts = ABSTRACT_PARTS.matcher(html.substring(open.end(), abstractClose));
        if (!parts.matches()) {
            return html;
        }
        String block = titlePageBlock(open.group(1), parts.group(1), parts.group(2).strip());

        // Where the abstract goes: before the table of contents when the
        // header holds one, and otherwise at the end of the header.
        Matcher toc = childMatching(html, headerBody, headerClose, TOC_OPEN);
        int insertAt = toc != null ? toc.start() : headerClose;

        // What is taken out: the abstract, and the preamble with it when
        // nothing that prints is left in it. A book's preamble starts a page
        // of its own, so a preamble left empty would put a blank page 1 in
        // front of the first section. A style or script element that was in
        // the preamble is kept, in the preamble's place.
        int removeStart = abstractStart;
        int removeEnd = skipWhitespace(html, abstractEnd);
        String kept = "";
        if (preamble >= 0) {
            String left = html.substring(container, abstractStart)
                    + html.substring(abstractEnd, containerClose);
            int preambleClose = closeOf(html, preamble + PREAMBLE_OPEN.length());
            int afterSectionBody = containerClose + DIV_CLOSE.length();
            if (NOT_PRINTED.matcher(left).replaceAll("").isBlank()
                    && preambleClose >= 0
                    && html.substring(afterSectionBody, preambleClose).isBlank()) {
                removeStart = preamble;
                removeEnd = skipWhitespace(html, preambleClose + DIV_CLOSE.length());
                kept = left.isBlank() ? "" : left.strip() + "\n";
            }
        }

        return html.substring(0, insertAt)
                + block
                + html.substring(insertAt, removeStart)
                + kept
                + html.substring(removeEnd);
    }

    /**
     * Write the abstract as an ordinary open block, so the stylesheet rules
     * for the body's text are the rules that set it.
     *
     * @param id      the id of the abstract block, or {@code null}
     * @param title   the block's title element, or {@code null}
     * @param content the content of the block's quote
     * @return the block for the title page, ending in a line break
     */
    private static String titlePageBlock(String id, String title, String content) {
        StringBuilder block = new StringBuilder();
        block.append("<div");
        if (id != null) {
            block.append(" id=\"").append(id).append('"');
        }
        block.append(" class=\"openblock ").append(TITLE_ABSTRACT_CLASS).append("\">\n");
        if (title != null) {
            block.append(title).append('\n');
        }
        block.append("<div class=\"content\">\n");
        if (BLOCK_START.matcher(content).lookingAt()) {
            block.append(content).append('\n');
        } else {
            block.append("<div class=\"paragraph\">\n<p>").append(content).append("</p>\n</div>\n");
        }
        block.append("</div>\n</div>\n");
        return block.toString();
    }

    /**
     * Find the first child element of a {@code div} whose opening tag matches
     * a pattern. A {@code div} nested inside another child is not a child.
     *
     * @param html    the document
     * @param from    the offset after the parent's opening tag
     * @param to      the offset of the parent's closing tag
     * @param pattern the opening tag looked for
     * @return a matcher on the child's opening tag, or {@code null}
     */
    private static Matcher childMatching(String html, int from, int to, Pattern pattern) {
        Matcher tag = DIV_TAG.matcher(html);
        tag.region(from, to);
        int depth = 0;
        while (tag.find()) {
            if (tag.group().startsWith("</")) {
                depth--;
            } else {
                if (depth == 0) {
                    Matcher child = pattern.matcher(html);
                    child.region(tag.start(), tag.end());
                    if (child.matches()) {
                        return child;
                    }
                }
                depth++;
            }
        }
        return null;
    }

    /**
     * Find the closing tag of a {@code div}.
     *
     * @param html the document
     * @param from the offset after the opening tag
     * @return the offset of the matching closing tag, or {@code -1} when the
     *         {@code div} tags do not balance
     */
    private static int closeOf(String html, int from) {
        Matcher tag = DIV_TAG.matcher(html);
        tag.region(from, html.length());
        int depth = 0;
        while (tag.find()) {
            if (tag.group().startsWith("</")) {
                if (depth == 0) {
                    return tag.start();
                }
                depth--;
            } else {
                depth++;
            }
        }
        return -1;
    }

    private static int skipWhitespace(String html, int from) {
        int at = from;
        while (at < html.length() && Character.isWhitespace(html.charAt(at))) {
            at++;
        }
        return at;
    }
}
