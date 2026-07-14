package network.ike.docs.koncept;

import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AsciidoctorJ block processor that renders a {@code [koncept-tree]} delimited block as an
 * <em>indented list of Koncept chips</em> — the print/HTML/e-mail projection of the live tree
 * Komet draws in the assistant surface ({@code IKE-Network/ike-issues#827}, the static counterpart
 * of {@code #805}).
 *
 * <p>Usage in AsciiDoc:
 * <pre>
 * [koncept-tree]
 * ----
 * k:sctid=772222008[Medical devices]
 *   k:sctid=118956008[Microbiology device]
 *     k:MultiTargetRespiratoryTest[Multi-target respiratory NAA test]
 * ----
 * </pre>
 *
 * <p>Each line is an id-bearing {@code k:} token; leading-space indentation carries the
 * parent/child edges (nearest ancestor with a strictly smaller indent — any consistent unit
 * works). Two token forms are accepted, so the same block is portable across media:
 * <ul>
 *   <li><b>Typed id</b> — {@code k:sctid=…}, {@code k:uuid=…}, {@code k:id=…}, {@code k:nid=…} —
 *       the <em>exact</em> grammar the live renderer parses
 *       ({@code KonceptTreeBlockRenderer} in {@code komet-claude-plugin}), so a tree the assistant
 *       emits in Komet renders identically here. The identicon is resolved <em>store-free</em>:
 *       {@code sctid} via {@link SnomedUuids#fromSnomed}, {@code uuid}/{@code id} directly. A
 *       {@code nid} is a live-store native id with no static meaning, so it falls back to its
 *       authoring label.</li>
 *   <li><b>Name key</b> — {@code k:HeartFailure[…]} — a key into {@code koncepts.yml}, resolved
 *       exactly like the inline {@link KonceptInlineMacro}; this form additionally carries the
 *       glossary cross-reference and the component-kind sigil.</li>
 * </ul>
 *
 * <p>A line may also be a <em>computed query directive</em> instead of a literal
 * {@code k:} token — {@code leaves: Identifier} (every leaf descendant, walked
 * recursively), {@code children: Identifier} (direct children only),
     * {@code descendants: Identifier} (every descendant at any depth, not including the
     * identifier itself), or {@code kindof: Identifier} (the identifier itself plus every
     * descendant — Tinkar's own "kind of" relation) — expanded,
 * before token parsing, into the equivalent set of {@code k:Identifier[]} lines at
 * the directive's own indent (so they nest exactly where a literal list of the same
 * lines would have). Both are computed fresh from the {@code broader} relationship
 * already in {@code koncepts.yml} ({@link KonceptGraph}) — no separate query data,
 * just a graph walk over what's already there (IKE-Network/ike-issues#879):
 * <pre>
 * [koncept-tree]
 * ----
 * k:Axioms[]
 *   leaves: Axioms
 * ----
 * </pre>
 *
 * <p>The hierarchy is drawn with <em>indentation, not line art</em>: box-drawing connectors
 * ({@code ├─ └─ │}) are exactly what breaks on copy-and-paste (wrong font, collapsed whitespace, a
 * plain-text target), whereas a leading run of fixed-width non-breaking spaces survives. The
 * indent is therefore emitted as literal {@code U+00A0} characters in the text — not a CSS margin,
 * which would vanish the instant a reader copies the tree into a plain-text e-mail — and each chip
 * carries its concept identity in the identicon {@code alt}/link {@code title}, so a plain-text
 * copy degrades to an indented outline of names-with-identity rather than to rubble.
 *
 * <p>Stage 1 ({@code #827}) renders the {@code html5} family (which includes the CSS-PDF backends
 * Prince/AH/WeasyPrint, all of which report {@code backend=html5}); {@code docbook5} &rarr; FO and
 * Prawn {@code pdf} render the raw tokens as a literal block for now and gain their own indented
 * projection in stage 2.
 */
@Name("koncept-tree")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class KonceptTreeBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptTreeBlockProcessor.class);

    /**
     * A typed-id token: {@code k:<kind>=<value>[label]}. Group 1 = kind (sctid/uuid/nid/id),
     * group 2 = the id value (an SCTID, a UUID, or a comma-joined UUID array), group 3 = the
     * optional bracketed authoring label. Kept byte-for-byte in step with the live renderer's
     * {@code TOKEN} pattern so both media accept the same source.
     */
    private static final Pattern TYPED = Pattern.compile(
            "^k:\\s*(sctid|uuid|nid|id)\\s*=\\s*([^\\[\\]]+?)\\s*(?:\\[(.*)])?$");

    /**
     * A name-key token: {@code k:<Identifier>[label]}. Group 1 = the {@code koncepts.yml}
     * identifier (no {@code =}, so it never collides with a typed token), group 2 = the optional
     * bracketed label.
     */
    private static final Pattern NAMED = Pattern.compile(
            "^k:\\s*([^\\[\\]=]+?)\\s*(?:\\[(.*)])?$");

    /**
     * A computed-query directive line: {@code leaves: Identifier}, {@code children: Identifier},
     * {@code descendants: Identifier}, or {@code kindof: Identifier}. Group 1 = the directive
     * kind, group 2 = the koncept identifier to walk from.
     */
    private static final Pattern DIRECTIVE =
            Pattern.compile("^(leaves|children|descendants|kindof)\\s*:\\s*(\\S+)\\s*$");

    /** Non-breaking spaces of indent per nesting level — fixed width, copy-survivable. */
    private static final int NBSP_PER_LEVEL = 3;
    /** The indent character: {@code U+00A0}, which (unlike a plain space) never collapses in HTML. */
    private static final char NBSP = '\u00A0';

    /** Creates a new block processor instance. */
    public KonceptTreeBlockProcessor() {
    }

    /**
     * Parses the block body and renders it as an indented list of Koncept chips for the current
     * backend.
     *
     * @param parent     the parent AST node
     * @param reader     the block content reader (raw, unsubstituted lines)
     * @param attributes the block attributes (unused)
     * @return a passthrough block of chip rows on the html5 family; a literal block of the raw
     *         tokens on other backends or when the body is not a well-formed tree
     */
    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        Document doc = parent.getDocument();
        List<String> lines = expandDirectives(doc, reader.readLines());
        List<ParsedNode> nodes = parse(lines);
        if (nodes.isEmpty()) {
            LOG.debug("koncept-tree: no well-formed token line; rendering raw content as literal");
            return createBlock(parent, "literal", lines);
        }
        String backend = String.valueOf(doc.getAttribute("backend", "html5"));
        if ("html5".equals(backend)) {
            return createBlock(parent, "pass", renderHtml(doc, nodes));
        }
        if (backend.startsWith("docbook") || "pdf".equals(backend)) {
            // DocBook → FO and Prawn PDF: re-parse each row as AsciiDoc so the inline k: macro
            // emits each backend's own identicon — a DocBook inlinemediaobject or a Prawn native
            // image — via the exact per-backend paths the inline chip is already tested on.
            return renderViaAsciiDoc(parent, doc, nodes);
        }
        // Any other backend: the raw tokens as a literal block, so the content is visible, never dropped.
        LOG.debug("koncept-tree: backend {} has no tree projection; emitting literal tokens", backend);
        return createBlock(parent, "literal", lines);
    }

    /**
     * Expands every {@code leaves:}/{@code children:}/{@code descendants:}/{@code kindof:}
     * directive line into the equivalent set of {@code k:Identifier[]} lines at the
     * directive's own indent, computed fresh from the document's known koncepts'
     * {@code broader} relationship.
     * Any other line (blank, a literal {@code k:} token, garbage that will fail
     * {@link #parse}) passes through unchanged.
     *
     * @param doc   the document (for {@code koncepts.yml} resolution)
     * @param lines the raw block lines
     * @return the lines with every directive expanded
     */
    private static List<String> expandDirectives(Document doc, List<String> lines) {
        KonceptDefinitionSource defSource = KonceptDefinitions.forDocument(doc);
        Map<String, List<String>> childrenById = null; // built lazily; most blocks have no directive
        List<String> expanded = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line.isBlank()) {
                expanded.add(line);
                continue;
            }
            String indent = line.substring(0, line.length() - line.stripLeading().length());
            Matcher directive = DIRECTIVE.matcher(line.strip());
            if (!directive.matches()) {
                expanded.add(line);
                continue;
            }
            if (childrenById == null) {
                childrenById = KonceptGraph.invertBroader(defSource.identifiers(), defSource);
            }
            String kind = directive.group(1);
            String rootId = directive.group(2);
            List<String> ids = switch (kind) {
                case "leaves" -> KonceptGraph.leaves(rootId, childrenById);
                case "descendants" -> KonceptGraph.descendants(rootId, childrenById);
                case "kindof" -> KonceptGraph.kindOf(rootId, childrenById);
                default -> childrenById.getOrDefault(rootId, List.of()); // "children"
            };
            if (ids.isEmpty()) {
                LOG.debug("koncept-tree: {} directive for {} produced no results", kind, rootId);
            }
            for (String id : ids) {
                expanded.add(indent + "k:" + id + "[]");
            }
        }
        return expanded;
    }

    // ── Parsing (pure; no document access) ──────────────────────────────

    /**
     * One parsed node: its indentation (leading-space count), the typed kind
     * (sctid/uuid/nid/id) or {@code null} for a name-key node, the raw id value or the name key,
     * and the optional bracketed authoring label.
     *
     * @param indent leading-space count, carrying nesting depth
     * @param kind   the typed token kind, or {@code null} for a name-key token
     * @param value  the id value (typed) or the {@code koncepts.yml} identifier (name-key)
     * @param label  the bracketed authoring label, or {@code null}
     */
    record ParsedNode(int indent, String kind, String value, String label) {
    }

    /**
     * Parses the block lines into node records. Blank lines are skipped; a non-blank line that is
     * neither a typed nor a name-key {@code k:} token aborts the parse (returns empty), so a
     * garbled block renders as a literal rather than a partial tree.
     *
     * @param lines the raw block lines
     * @return the parsed nodes in source order, or an empty list if any line is malformed
     */
    static List<ParsedNode> parse(List<String> lines) {
        List<ParsedNode> nodes = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int indent = line.length() - line.stripLeading().length();
            String body = line.strip();
            Matcher typed = TYPED.matcher(body);
            if (typed.matches()) {
                nodes.add(new ParsedNode(indent, typed.group(1), typed.group(2).trim(), typed.group(3)));
                continue;
            }
            Matcher named = NAMED.matcher(body);
            if (named.matches()) {
                nodes.add(new ParsedNode(indent, null, named.group(1).trim(), named.group(2)));
                continue;
            }
            return List.of();
        }
        return nodes;
    }

    /**
     * Assigns each node its nesting depth from the indentation, using an indent stack: a node
     * attaches to the nearest earlier node with a strictly smaller indent.
     *
     * @param nodes the parsed nodes in source order
     * @return a parallel array of depths (0 for roots)
     */
    static int[] depths(List<ParsedNode> nodes) {
        int[] depth = new int[nodes.size()];
        Deque<Integer> indents = new ArrayDeque<>();
        for (int i = 0; i < nodes.size(); i++) {
            int indent = nodes.get(i).indent();
            while (!indents.isEmpty() && indents.peek() >= indent) {
                indents.pop();
            }
            depth[i] = indents.size();
            indents.push(indent);
        }
        return depth;
    }

    // ── HTML rendering (html5 family) ───────────────────────────────────

    /**
     * Renders the parsed nodes as a passthrough block of indented chip rows for the html5 family.
     *
     * @param doc   the document (for {@code koncepts.yml} resolution of name-key tokens)
     * @param nodes the parsed nodes
     * @return the self-contained, inline-styled HTML for the tree
     */
    private String renderHtml(Document doc, List<ParsedNode> nodes) {
        int[] depth = depths(nodes);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"koncept-tree\" style=\"margin:0.6em 0;line-height:1.7;\">");
        for (int i = 0; i < nodes.size(); i++) {
            sb.append(renderRow(doc, nodes.get(i), depth[i]));
        }
        return sb.append("</div>").toString();
    }

    /**
     * Renders one tree row: a leading run of non-breaking spaces (depth × {@link #NBSP_PER_LEVEL})
     * followed by the resolved chip.
     *
     * @param doc   the document
     * @param node  the parsed node
     * @param depth the node's nesting depth
     * @return the HTML for the row
     */
    private String renderRow(Document doc, ParsedNode node, int depth) {
        String indent = String.valueOf(NBSP).repeat(depth * NBSP_PER_LEVEL);
        return "<div class=\"koncept-tree-row\" style=\"white-space:nowrap;\">"
                + indent + renderChip(doc, node) + "</div>";
    }

    /**
     * Resolves and renders one node's chip: an identicon chip when an idString resolves, otherwise
     * a plain label. A name-key node additionally links to its glossary anchor and carries its
     * component-kind sigil.
     *
     * @param doc  the document
     * @param node the parsed node
     * @return the HTML for the chip
     */
    private String renderChip(Document doc, ParsedNode node) {
        KonceptResolver.Resolved r = resolve(doc, node);
        String labelHtml = "<span class=\"koncept-label\" "
                + "style=\"color:#2a5a8a;font-variant:small-caps;letter-spacing:0.02em;\">"
                + escapeXml(r.label()) + "</span>";

        // A stamp is provenance, never an identicon (ike-issues#638): the pentagon glyph + label.
        if (r.kind().isStamp()) {
            return wrapLink(r.anchor(), r.identity(),
                    "<span class=\"koncept-stamp\" style=\"color:#777;\">⬠ </span>" + labelHtml);
        }

        String sigil = r.kind().hasLetterGlyph()
                ? "<span class=\"koncept-sigil koncept-sigil-%s\" style=\"color:%s;font-weight:bold;margin-right:0.25em;\">%s</span>"
                        .formatted(r.kind().name().toLowerCase(Locale.ROOT), r.kind().colorHex(),
                                escapeXml(r.kind().glyph()))
                : "";

        if (r.idString().isEmpty()) {
            // Unresolved id (e.g. a nid at build time) — the authoring label carries the node, so a
            // single unknown id never breaks the tree.
            return wrapLink(r.anchor(), r.identity(),
                    "<span class=\"koncept-chip\" style=\"display:inline;background:#eee;"
                            + "border-radius:0.5em;padding:0.12em 0.45em;\">" + sigil + labelHtml + "</span>");
        }

        // Identity in the identicon alt (best-effort plain-text-copy carrier) and the link title.
        String img = "<img class=\"koncept-identicon\" src=\"%s\" alt=\"%s\" "
                .formatted(IdenticonRenderer.dataUri(r.idString().get()), escapeXml(r.identity()))
                + "style=\"height:0.9em;width:0.9em;vertical-align:-0.12em;border-radius:2px;"
                + "image-rendering:pixelated;margin-right:0.3em;\"/>";
        String chip = "<span class=\"koncept-chip\" style=\"display:inline;background:#e9eff6;"
                + "border-radius:0.5em;padding:0.12em 0.45em;"
                + "-webkit-box-decoration-break:clone;box-decoration-break:clone;\">"
                + sigil + img + labelHtml + "</span>";
        return wrapLink(r.anchor(), r.identity(), chip);
    }

    /**
     * Wraps chip markup in a glossary cross-reference link when an anchor is known, else in a
     * bare span carrying the identity as a hover title.
     *
     * @param anchor   the glossary anchor identifier, or {@code null} for an id-only node
     * @param identity the identity text (name · id) for the link/span title
     * @param inner    the chip markup
     * @return the wrapped HTML
     */
    private String wrapLink(String anchor, String identity, String inner) {
        if (anchor != null) {
            return "<a href=\"#koncept-%s\" class=\"koncept-ref koncept-identicon-ref\" title=\"%s\" "
                    .formatted(escapeXml(anchor), escapeXml(identity))
                    + "style=\"text-decoration:none;white-space:nowrap;\">" + inner + "</a>";
        }
        return "<span title=\"%s\" style=\"white-space:nowrap;\">".formatted(escapeXml(identity))
                + inner + "</span>";
    }

    // ── DocBook / Prawn-PDF rendering (re-parsed rows) ──────────────────

    /**
     * Renders the tree for the DocBook and Prawn-PDF backends by re-parsing its rows as AsciiDoc:
     * a curated node becomes a {@code k:} inline chip (so the inline macro emits the backend's own
     * identicon — a DocBook {@code inlinemediaobject} or a Prawn native image, both already tested),
     * an uncurated node with a PublicId an inline {@code image:} of its identicon PNG, and an
     * unresolved node its plain label. Rows are one paragraph — hard-break separated and indented
     * with non-breaking spaces (depth × {@link #NBSP_PER_LEVEL}) — inside an open block.
     *
     * @param parent the parent AST node
     * @param doc    the document (for {@code koncepts.yml} resolution)
     * @param nodes  the parsed nodes
     * @return an open block containing the rendered tree paragraph
     */
    private Object renderViaAsciiDoc(StructuralNode parent, Document doc, List<ParsedNode> nodes) {
        int[] depth = depths(nodes);
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            String indent = "{nbsp}".repeat(depth[i] * NBSP_PER_LEVEL);
            String row = indent + rowAsciiDoc(doc, nodes.get(i));
            // A trailing " +" hard-breaks to the next row, keeping the whole tree one tight paragraph.
            rows.add(i < nodes.size() - 1 ? row + " +" : row);
        }
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>());
        parseContent(container, rows);
        return container;
    }

    /**
     * One tree row as AsciiDoc: a {@code k:} chip for a curated node (the inline macro resolves the
     * canonical label unless the author bracketed an override), an inline identicon {@code image:}
     * for an uncurated node that still carries a PublicId, or the plain label when no identicon
     * resolves.
     *
     * @param doc  the document
     * @param node the parsed node
     * @return the row's AsciiDoc source
     */
    private String rowAsciiDoc(Document doc, ParsedNode node) {
        KonceptResolver.Resolved resolved = resolve(doc, node);
        if (resolved.anchor() != null) {
            // k: renders identicon + visible name + glossary cross-reference on every backend
            // (ike-issues#836); pass the (possibly overridden) label so the chip shows exactly it.
            return "k:" + resolved.anchor() + "[" + adocAttribute(resolved.label()) + "]";
        }
        if (resolved.idString().isPresent()) {
            // Uncurated node: an inline image macro (no k: chip), so the name is appended here.
            String png = IdenticonRenderer.pngFile(resolved.idString().get());
            return "image:" + png + "[" + adocAttribute(resolved.identity()) + ",18,18] "
                    + resolved.label();
        }
        return resolved.label();
    }

    /**
     * Renders a value safe to embed as a re-parsed {@code k:}/{@code image:} attribute: quoted so a
     * comma cannot split the attribute list, and with {@code ]} backslash-escaped so it cannot
     * prematurely close the macro — Asciidoctor's macro-boundary regex terminates the attrlist at
     * the first unescaped {@code ]} before quotes are parsed. (SNOMED CT carries bracketed
     * descriptions like {@code [D]Chest pain}.)
     */
    private static String adocAttribute(String value) {
        return "\"" + value.replace("\"", "\\\"").replace("]", "\\]") + "\"";
    }

    // ── Resolution (delegated to the shared resolver, #837) ─────────────

    /**
     * Resolves a parsed node through the shared {@link KonceptResolver}, so a tree node and an
     * inline {@code k:} reference resolve identically (typed ids via the reverse index, PublicId
     * preferred, koncepts.yml the authority).
     *
     * @param doc  the document
     * @param node the parsed node
     * @return the resolved rendering data
     */
    private KonceptResolver.Resolved resolve(Document doc, ParsedNode node) {
        return KonceptResolver.resolve(doc, node.kind(), node.value(), node.label());
    }

    /** Minimal XML/HTML attribute-and-text escaping. */
    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
