/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.typst.asciidoc;

import network.ike.docs.typst.TypstPageSetup;
import network.ike.docs.typst.TypstText;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.Column;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.DescriptionList;
import org.asciidoctor.ast.DescriptionListEntry;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.asciidoctor.converter.ConverterFor;
import org.asciidoctor.converter.StringConverter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * AsciidoctorJ converter for the {@code typst} backend: walks the Asciidoctor
 * AST and emits Typst source.
 *
 * <p>Coverage (IKE-Network/ike-issues#1307 spike): document title and
 * attributes, sections to any depth, paragraphs, emphasis / strong /
 * monospace, ordered, unordered and description lists, listings and literal
 * blocks, admonitions, tables with {@code cols} proportions and a repeating
 * header row, footnotes (native {@code footnote}), in-document cross
 * references ({@code link} to a label), index terms (as {@code metadata}
 * labelled {@code <ike-index>}), bibliography anchors and citations (plain),
 * images ({@code image()} in a {@code figure}) and {@code stem} blocks
 * (LaTeX through the mitex package). Anything else renders as a visibly
 * marked literal fallback, never silently dropped.
 *
 * <p>Koncept inline macros arrive as {@code quoted} phrase nodes carrying the
 * role {@code koncept} (see {@code KonceptInlineMacro}'s typst branch) and
 * render through {@code ike-koncept} from the page setup.
 */
@ConverterFor(value = "typst", suffix = ".typ")
public class TypstConverter extends StringConverter {

    private static final String ROLE_KONCEPT = "koncept";

    /**
     * Creates the converter for one document.
     *
     * @param backend the backend name ({@code typst})
     * @param opts    converter options from AsciidoctorJ
     */
    public TypstConverter(String backend, Map<String, Object> opts) {
        super(backend, opts);
    }

    @Override
    public String convert(ContentNode node, String transform, Map<Object, Object> opts) {
        String t = transform != null ? transform : node.getNodeName();
        if (node instanceof PhraseNode phrase) {
            return convertInline(phrase, t);
        }
        return switch (t) {
            case "document" -> convertDocument((Document) node, true);
            case "embedded" -> convertDocument((Document) node, false);
            case "preamble", "open" -> convertOpen((StructuralNode) node);
            case "section" -> convertSection((Section) node);
            case "floating_title" -> convertFloatingTitle((StructuralNode) node);
            case "paragraph" -> convertParagraph((StructuralNode) node);
            case "ulist" -> convertList((org.asciidoctor.ast.List) node, "- ");
            case "olist" -> convertList((org.asciidoctor.ast.List) node, "+ ");
            case "colist" -> convertList((org.asciidoctor.ast.List) node, "+ ");
            case "dlist" -> convertDescriptionList((DescriptionList) node);
            case "listing", "literal" -> convertVerbatim((StructuralNode) node);
            case "admonition" -> convertAdmonition((StructuralNode) node);
            case "example" -> convertFramed((StructuralNode) node, "ike-example");
            case "sidebar" -> convertFramed((StructuralNode) node, "ike-sidebar");
            case "quote" -> convertQuote((StructuralNode) node);
            case "verse" -> convertVerse((StructuralNode) node);
            case "table" -> convertTable((Table) node);
            case "image" -> convertImage((StructuralNode) node);
            case "stem" -> convertStem((StructuralNode) node);
            case "thematic_break" -> "#line(length: 100%, stroke: 0.5pt + luma(160))\n\n";
            case "page_break" -> "#pagebreak()\n\n";
            case "toc" -> "#outline()\n\n";
            default -> unconverted((StructuralNode) node, t);
        };
    }

    // ── Document ─────────────────────────────────────────────────────

    private String convertDocument(Document doc, boolean standalone) {
        StringBuilder body = new StringBuilder();
        String title = doc.getDoctitle();
        if (standalone && title != null && !title.isEmpty()) {
            body.append("#ike-title[").append(TypstText.markup(title)).append(']');
            if (doc.getId() != null) {
                body.append(' ').append(TypstText.label(doc.getId()));
            }
            body.append("\n\n");
            body.append(attributeMetadata(doc));
        }
        Object content = doc.getContent();
        if (content != null) {
            body.append(content);
        }
        if (!standalone) {
            return body.toString();
        }
        boolean needsMath = body.indexOf("#mitex(") >= 0 || body.indexOf("#mi(") >= 0;
        return TypstPageSetup.preamble(TypstText.plain(title), needsMath) + body;
    }

    /** The topic header attributes and revision data, kept on the page as queryable metadata. */
    private String attributeMetadata(Document doc) {
        Map<String, String> kept = new TreeMap<>();
        for (Map.Entry<String, Object> e : doc.getAttributes().entrySet()) {
            String k = e.getKey();
            if (k.startsWith("topic-") || k.equals("author") || k.equals("revnumber")
                    || k.equals("revdate") || k.equals("docname")) {
                kept.put(k, String.valueOf(e.getValue()));
            }
        }
        if (kept.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("#metadata((\n");
        kept.forEach((k, v) -> sb.append("  ").append(TypstText.string(k)).append(": ")
                .append(TypstText.string(v)).append(",\n"));
        sb.append(")) <ike-attributes>\n\n");
        return sb.toString();
    }

    // ── Sections and paragraphs ──────────────────────────────────────

    private String convertSection(Section node) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(Math.max(1, node.getLevel()))).append(' ')
                .append(TypstText.markup(node.getTitle()));
        if (node.getId() != null) {
            sb.append(' ').append(TypstText.label(node.getId()));
        }
        sb.append("\n\n");
        appendContent(sb, node);
        return sb.toString();
    }

    private String convertFloatingTitle(StructuralNode node) {
        StringBuilder sb = new StringBuilder("#heading(level: ").append(Math.max(1, node.getLevel()))
                .append(", outlined: false)[").append(TypstText.markup(node.getTitle())).append(']');
        if (node.getId() != null) {
            sb.append(' ').append(TypstText.label(node.getId()));
        }
        return sb.append("\n\n").toString();
    }

    private String convertOpen(StructuralNode node) {
        StringBuilder sb = new StringBuilder();
        if ("abstract".equals(node.getStyle())) {
            sb.append("#block(inset: (x: 1.5em), text(style: \"italic\")[");
            appendContent(sb, node);
            return sb.append("])\n\n").toString();
        }
        blockTitle(sb, node);
        appendContent(sb, node);
        return sb.toString();
    }

    private String convertParagraph(StructuralNode node) {
        StringBuilder sb = new StringBuilder();
        blockTitle(sb, node);
        String text = TypstText.markup(String.valueOf(node.getContent()));
        if (node.getId() != null) {
            sb.append("#par[").append(text).append("] ").append(TypstText.label(node.getId()));
        } else {
            sb.append(text);
        }
        return sb.append("\n\n").toString();
    }

    private void blockTitle(StringBuilder sb, StructuralNode node) {
        String title = node.getTitle();
        if (title != null && !title.isEmpty()) {
            sb.append("#ike-block-title[").append(TypstText.markup(title)).append("]\n");
        }
    }

    /** Appends a compound node's already-converted children. */
    private void appendContent(StringBuilder sb, StructuralNode node) {
        Object content = node.getContent();
        if (content != null) {
            sb.append(content);
        }
    }

    // ── Lists ────────────────────────────────────────────────────────

    private String convertList(org.asciidoctor.ast.List node, String marker) {
        StringBuilder sb = new StringBuilder();
        blockTitle(sb, node);
        Object start = node.getAttribute("start");
        if (start != null && "+ ".equals(marker)) {
            sb.append("#set enum(start: ").append(start).append(")\n");
        }
        for (StructuralNode item : node.getItems()) {
            ListItem li = (ListItem) item;
            StringBuilder body = new StringBuilder();
            if (li.hasText()) {
                body.append(TypstText.markup(li.getText()));
            }
            if (!li.getBlocks().isEmpty()) {
                body.append("\n\n");
                appendContent(body, li);
            }
            sb.append(marker).append(indentContinuation(body.toString().strip(), marker.length())).append('\n');
        }
        return sb.append('\n').toString();
    }

    private String convertDescriptionList(DescriptionList node) {
        StringBuilder sb = new StringBuilder();
        blockTitle(sb, node);
        for (DescriptionListEntry entry : node.getItems()) {
            List<String> terms = new ArrayList<>();
            for (ListItem term : entry.getTerms()) {
                terms.add(TypstText.markup(term.getText()));
            }
            StringBuilder body = new StringBuilder();
            ListItem desc = entry.getDescription();
            if (desc != null) {
                if (desc.hasText()) {
                    body.append(TypstText.markup(desc.getText()));
                }
                if (!desc.getBlocks().isEmpty()) {
                    body.append("\n\n");
                    appendContent(body, desc);
                }
            }
            sb.append("/ ").append(String.join(", ", terms)).append(": ")
                    .append(indentContinuation(body.toString().strip(), 2)).append('\n');
        }
        return sb.append('\n').toString();
    }

    /** Indents every line but the first so that it stays inside the list item. */
    private static String indentContinuation(String text, int width) {
        if (text.indexOf('\n') < 0) return text;
        String pad = " ".repeat(width);
        return text.replace("\n", "\n" + pad).replaceAll("(?m)^" + pad + "$", "");
    }

    // ── Verbatim ─────────────────────────────────────────────────────

    private String convertVerbatim(StructuralNode node) {
        String content = TypstText.plain(String.valueOf(node.getContent()));
        String lang = null;
        Object language = node.getAttribute("language");
        if ("listing".equals(node.getContext()) && language != null) {
            lang = language.toString();
        }
        String fence = TypstText.fence(content);
        StringBuilder raw = new StringBuilder();
        raw.append(fence);
        if (lang != null) raw.append(lang);
        raw.append('\n').append(content).append('\n').append(fence);
        StringBuilder sb = new StringBuilder();
        String title = node.getTitle();
        if (title != null && !title.isEmpty()) {
            sb.append("#figure(caption: [").append(TypstText.markup(title))
                    .append("], kind: raw, supplement: [Listing])[\n").append(raw).append("\n]");
        } else {
            sb.append(raw);
        }
        if (node.getId() != null) {
            sb.append(' ').append(TypstText.label(node.getId()));
        }
        return sb.append("\n\n").toString();
    }

    // ── Framed blocks ────────────────────────────────────────────────

    private String convertAdmonition(StructuralNode node) {
        String kind = String.valueOf(node.getAttribute("name", "note")).toUpperCase();
        StringBuilder sb = new StringBuilder("#ike-admonition(").append(TypstText.string(kind)).append(")[\n");
        if (node.getTitle() != null) {
            sb.append("#strong[").append(TypstText.markup(node.getTitle())).append("] \\\n");
        }
        sb.append(innerContent(node));
        return sb.append("\n]\n\n").toString();
    }

    private String convertFramed(StructuralNode node, String function) {
        StringBuilder sb = new StringBuilder("#").append(function).append('(');
        sb.append(node.getTitle() == null ? "none" : "[" + TypstText.markup(node.getTitle()) + "]");
        sb.append(")[\n").append(innerContent(node)).append("\n]");
        if (node.getId() != null) {
            sb.append(' ').append(TypstText.label(node.getId()));
        }
        return sb.append("\n\n").toString();
    }

    private String convertQuote(StructuralNode node) {
        StringBuilder sb = new StringBuilder("#quote(block: true");
        Object attribution = node.getAttribute("attribution");
        Object citetitle = node.getAttribute("citetitle");
        if (attribution != null || citetitle != null) {
            sb.append(", attribution: [");
            if (attribution != null) sb.append(TypstText.markup(attribution.toString()));
            if (attribution != null && citetitle != null) sb.append(", ");
            if (citetitle != null) sb.append("#emph[").append(TypstText.markup(citetitle.toString())).append(']');
            sb.append(']');
        }
        sb.append(")[\n").append(innerContent(node)).append("\n]\n\n");
        return sb.toString();
    }

    private String convertVerse(StructuralNode node) {
        String text = TypstText.markup(String.valueOf(node.getContent()));
        return "#ike-verse[\n" + text.replace("\n", " \\\n") + "\n]\n\n";
    }

    /** A block's content as Typst: text for a simple content model, children for a compound one. */
    private String innerContent(StructuralNode node) {
        Object content = node.getContent();
        if (content == null) return "";
        if ("compound".equals(node.getContentModel())) {
            return content.toString().strip();
        }
        return TypstText.markup(content.toString());
    }

    // ── Tables ───────────────────────────────────────────────────────

    private String convertTable(Table table) {
        List<Column> columns = table.getColumns();
        boolean autowidth = table.getAttribute("autowidth-option") != null;
        StringBuilder sb = new StringBuilder();
        boolean titled = table.getTitle() != null && !table.getTitle().isEmpty();
        if (titled) {
            sb.append("#figure(\n  ");
        } else {
            sb.append('#');
        }
        sb.append("table(\n    columns: (");
        for (Column c : columns) {
            if (autowidth) {
                sb.append("auto, ");
            } else {
                Object pct = c.getAttribute("colpcwidth");
                String width = pct != null ? pct.toString() : String.valueOf(c.getWidth());
                if (width.endsWith(".0")) width = width.substring(0, width.length() - 2);
                sb.append(width).append("fr, ");
            }
        }
        sb.append("),\n    align: (");
        for (Column c : columns) {
            sb.append(halign(c.getHorizontalAlignment())).append(" + top, ");
        }
        sb.append("),\n");
        if (!table.getHeader().isEmpty()) {
            sb.append("    table.header(repeat: true,\n");
            for (Row row : table.getHeader()) {
                sb.append("      ");
                for (Cell cell : row.getCells()) {
                    sb.append(cellContent(cell, true)).append(", ");
                }
                sb.append('\n');
            }
            sb.append("    ),\n");
        }
        for (Row row : table.getBody()) {
            sb.append("    ");
            for (Cell cell : row.getCells()) {
                sb.append(cellContent(cell, false)).append(", ");
            }
            sb.append('\n');
        }
        if (!table.getFooter().isEmpty()) {
            sb.append("    table.footer(\n");
            for (Row row : table.getFooter()) {
                sb.append("      ");
                for (Cell cell : row.getCells()) {
                    sb.append(cellContent(cell, false)).append(", ");
                }
                sb.append('\n');
            }
            sb.append("    ),\n");
        }
        sb.append("  )");
        if (titled) {
            sb.append(",\n  caption: [").append(TypstText.markup(table.getTitle())).append("],\n  kind: table,\n)");
        }
        if (table.getId() != null) {
            sb.append(' ').append(TypstText.label(table.getId()));
        }
        return sb.append("\n\n").toString();
    }

    private String cellContent(Cell cell, boolean header) {
        String body;
        String style = cell.getStyle();
        if ("asciidoc".equals(style)) {
            Object content = cell.getContent();
            body = content == null ? "" : content.toString().strip().replaceAll("\\n{3,}", "\n\n");
        } else if ("literal".equals(style) || "verbatim".equals(style)) {
            String text = TypstText.plain(cell.getText());
            String fence = TypstText.fence(text);
            body = "\n" + fence + "\n" + text + "\n" + fence + "\n";
        } else {
            body = TypstText.markup(cell.getText());
            if ("strong".equals(style)) body = "#strong[" + body + "]";
            else if ("emphasis".equals(style)) body = "#emph[" + body + "]";
            else if ("monospaced".equals(style)) body = "#raw(" + TypstText.string(TypstText.plain(cell.getText())) + ")";
        }
        if (header || "header".equals(style)) {
            body = "#strong[" + body + "]";
        }
        StringBuilder sb = new StringBuilder();
        if (cell.getColspan() > 1 || cell.getRowspan() > 1) {
            sb.append("table.cell(");
            if (cell.getColspan() > 1) sb.append("colspan: ").append(cell.getColspan()).append(", ");
            if (cell.getRowspan() > 1) sb.append("rowspan: ").append(cell.getRowspan()).append(", ");
            sb.append(")[").append(body).append(']');
        } else {
            sb.append('[').append(body).append(']');
        }
        return sb.toString();
    }

    private static String halign(Table.HorizontalAlignment alignment) {
        if (alignment == null) return "left";
        return switch (alignment) {
            case CENTER -> "center";
            case RIGHT -> "right";
            default -> "left";
        };
    }

    // ── Images and math ──────────────────────────────────────────────

    private String convertImage(StructuralNode node) {
        String target = imagePath(node, String.valueOf(node.getAttribute("target")));
        StringBuilder sb = new StringBuilder();
        StringBuilder image = new StringBuilder("image(").append(TypstText.string(target));
        Object width = node.getAttribute("pdfwidth", node.getAttribute("scaledwidth"));
        if (width != null && width.toString().endsWith("%")) {
            image.append(", width: ").append(width);
        } else {
            image.append(", width: 100%");
        }
        image.append(')');
        boolean titled = node.getTitle() != null && !node.getTitle().isEmpty();
        if (titled) {
            sb.append("#figure(\n  ").append(image).append(",\n  caption: [")
                    .append(TypstText.markup(node.getTitle())).append("],\n)");
        } else {
            sb.append("#figure(").append(image).append(')');
        }
        if (node.getId() != null) {
            sb.append(' ').append(TypstText.label(node.getId()));
        }
        return sb.append("\n\n").toString();
    }

    /**
     * An image path as Typst resolves it: root-relative ({@code /…}) when the
     * resolved file lies under the {@code typst-root} document attribute, so
     * that {@code typst compile --root} finds it; otherwise as given.
     */
    private String imagePath(ContentNode node, String target) {
        Document doc = node.getDocument();
        Object imagesdir = doc.getAttribute("imagesdir");
        String resolved = target;
        if (!target.startsWith("/") && !target.contains("://") && imagesdir != null && !imagesdir.toString().isEmpty()) {
            resolved = imagesdir + "/" + target;
        }
        Object root = doc.getAttribute("typst-root");
        if (root != null && resolved.startsWith("/")) {
            Path rootPath = Path.of(root.toString()).toAbsolutePath().normalize();
            Path image = Path.of(resolved).toAbsolutePath().normalize();
            if (image.startsWith(rootPath)) {
                return "/" + rootPath.relativize(image).toString().replace('\\', '/');
            }
        }
        return resolved;
    }

    private String convertStem(StructuralNode node) {
        String style = String.valueOf(node.getStyle());
        String source = TypstText.plain(String.valueOf(node.getContent())).strip();
        StringBuilder sb = new StringBuilder();
        blockTitle(sb, node);
        if ("asciimath".equals(style)) {
            return sb.append(unconverted(node, "stem (asciimath)")).toString();
        }
        sb.append("#mitex(").append(TypstText.string(source)).append(')');
        if (node.getId() != null) {
            sb.append(' ').append(TypstText.label(node.getId()));
        }
        return sb.append("\n\n").toString();
    }

    // ── Fallback ─────────────────────────────────────────────────────

    private String unconverted(StructuralNode node, String context) {
        String source = null;
        if (node instanceof Block block && block.getSource() != null && !block.getSource().isEmpty()) {
            source = block.getSource();
        } else if (node.getContent() != null) {
            source = TypstText.plain(node.getContent().toString());
        }
        if (source == null) source = "";
        String fence = TypstText.fence(source);
        return "#ike-unconverted(" + TypstText.string(context) + ")[\n"
                + fence + "\n" + source + "\n" + fence + "\n]\n\n";
    }

    // ── Inline ───────────────────────────────────────────────────────

    /**
     * Inline nodes return protected Typst: an atom ({@link TypstText#protect})
     * when the content is a string argument, or an open/close pair around the
     * substituted text ({@link TypstText#wrap}) when the content is markup.
     */
    private String convertInline(PhraseNode node, String transform) {
        return switch (transform) {
            case "inline_quoted" -> convertQuoted(node);
            case "inline_anchor" -> convertAnchor(node);
            case "inline_footnote" -> convertFootnote(node);
            case "inline_indexterm" -> convertIndexTerm(node);
            case "inline_image" -> TypstText.protect("#box(image(" + TypstText.string(imagePath(node, node.getTarget()))
                    + ", height: 1em))");
            case "inline_break" -> (node.getText() == null ? "" : node.getText()) + TypstText.protect(" \\\n");
            case "inline_button" -> TypstText.wrap("#box(stroke: 0.5pt + luma(120), inset: (x: 3pt, y: 1pt), radius: 2pt)[",
                    node.getText(), "]");
            case "inline_kbd" -> TypstText.protect(kbd(node));
            case "inline_menu" -> TypstText.protect(menu(node));
            case "inline_callout" -> TypstText.protect("(" + TypstText.plain(node.getText()) + ")");
            default -> TypstText.wrap("#ike-unconverted-inline(" + TypstText.string(transform) + ")[",
                    node.getText(), "]");
        };
    }

    private String convertQuoted(PhraseNode node) {
        String type = node.getType() == null ? "unquoted" : node.getType();
        String text = node.getText();
        return switch (type) {
            case "strong" -> TypstText.wrap("#strong[", text, "]");
            case "emphasis" -> TypstText.wrap("#emph[", text, "]");
            case "monospaced" -> TypstText.protect("#raw(" + TypstText.string(TypstText.plain(text)) + ")");
            case "superscript" -> TypstText.wrap("#super[", text, "]");
            case "subscript" -> TypstText.wrap("#sub[", text, "]");
            case "mark" -> TypstText.wrap("#highlight[", text, "]");
            case "double" -> TypstText.wrap("“", text, "”");
            case "single" -> TypstText.wrap("‘", text, "’");
            case "latexmath" -> TypstText.protect("#mi(" + TypstText.string(TypstText.plain(text)) + ")");
            case "asciimath" -> TypstText.wrap("#ike-unconverted-inline(\"stem (asciimath)\")[", text, "]");
            default -> {
                if (node.hasRole(ROLE_KONCEPT)) {
                    yield TypstText.protect(koncept(node));
                }
                yield text == null ? "" : text;
            }
        };
    }

    private String koncept(PhraseNode node) {
        Object kind = node.getAttribute("koncept-kind", "KONCEPT");
        boolean inactive = Boolean.parseBoolean(String.valueOf(node.getAttribute("koncept-inactive", "false")));
        return "#ike-koncept(" + TypstText.string(TypstText.plain(node.getText()))
                + ", kind: " + TypstText.string(String.valueOf(kind))
                + (inactive ? ", inactive: true" : "") + ")";
    }

    private String convertAnchor(PhraseNode node) {
        String type = node.getType() == null ? "" : node.getType();
        switch (type) {
            case "xref" -> {
                String refid = String.valueOf(node.getAttribute("refid", stripHash(node.getTarget())));
                Document doc = node.getDocument();
                Map<String, Object> refs = doc.getCatalog().getRefs();
                String text = node.getText();
                if (refs.containsKey(refid)) {
                    String body = text != null ? text : refText(refs.get(refid), refid);
                    return TypstText.wrap("#link(" + TypstText.label(refid) + ")[", body, "]");
                }
                String body = text != null ? text : refid;
                return TypstText.wrap("#ike-xref-unresolved(" + TypstText.string(refid) + ")[", body, "]");
            }
            case "link" -> {
                String text = node.getText() != null ? node.getText() : node.getTarget();
                return TypstText.wrap("#link(" + TypstText.string(node.getTarget()) + ")[", text, "]");
            }
            case "ref" -> {
                return TypstText.protect("#metadata(\"anchor\") " + TypstText.label(node.getId()));
            }
            case "bibref" -> {
                String shown = node.getReftext() != null ? node.getReftext() : "[" + node.getId() + "]";
                return TypstText.protect("#metadata(\"bibref\") " + TypstText.label(node.getId())
                        + TypstText.escapeMarkup(TypstText.plain(shown)));
            }
            default -> {
                return TypstText.wrap("#ike-unconverted-inline(" + TypstText.string("anchor " + type) + ")[",
                        node.getText(), "]");
            }
        }
    }

    private static String stripHash(String target) {
        if (target == null) return "";
        return target.startsWith("#") ? target.substring(1) : target;
    }

    /** The text a reference shows when the source gave none: the target's reftext or title. */
    private static String refText(Object ref, String refid) {
        if (ref instanceof StructuralNode sn) {
            if (sn.getReftext() != null) return sn.getReftext();
            if (sn.getTitle() != null) return sn.getTitle();
        } else if (ref instanceof PhraseNode pn) {
            if (pn.getReftext() != null) return pn.getReftext();
            if (pn.getText() != null) return pn.getText();
        } else if (ref instanceof ContentNode cn && cn.getReftext() != null) {
            return cn.getReftext();
        }
        return "[" + refid + "]";
    }

    private String convertFootnote(PhraseNode node) {
        if ("xref".equals(node.getType())) {
            return TypstText.protect("#footnote(" + TypstText.label("fn-" + node.getTarget()) + ")");
        }
        String close = node.getId() != null ? "]" + TypstText.label("fn-" + node.getId()) : "]";
        return TypstText.wrap("#footnote[", node.getText(), close);
    }

    private String convertIndexTerm(PhraseNode node) {
        List<String> terms = new ArrayList<>();
        Object attr = node.getAttribute("terms");
        if (attr instanceof java.util.List<?> list) {
            for (Object term : list) {
                terms.add(TypstText.string(TypstText.plain(String.valueOf(term))));
            }
        } else if (node.getText() != null) {
            terms.add(TypstText.string(TypstText.plain(node.getText())));
        }
        String call = TypstText.protect("#ike-index(" + String.join(", ", terms) + ")");
        if ("visible".equals(node.getType()) && node.getText() != null) {
            return call + node.getText();
        }
        return call;
    }

    private String kbd(PhraseNode node) {
        Object keys = node.getAttribute("keys");
        if (keys instanceof java.util.List<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object k : list) {
                parts.add("#raw(" + TypstText.string(String.valueOf(k)) + ")");
            }
            return String.join("+", parts);
        }
        return "#raw(" + TypstText.string(TypstText.plain(node.getText())) + ")";
    }

    private String menu(PhraseNode node) {
        List<String> parts = new ArrayList<>();
        parts.add(TypstText.escapeMarkup(String.valueOf(node.getAttribute("menu"))));
        Object submenus = node.getAttribute("submenus");
        if (submenus instanceof java.util.List<?> list) {
            for (Object s : list) parts.add(TypstText.escapeMarkup(String.valueOf(s)));
        }
        Object item = node.getAttribute("menuitem");
        if (item != null) parts.add(TypstText.escapeMarkup(item.toString()));
        return "#strong[" + String.join(" › ", parts) + "]";
    }
}
