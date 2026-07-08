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

import network.ike.docs.konceptcore.KonceptKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
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
        List<String> lines = reader.readLines();
        List<ParsedNode> nodes = parse(lines);
        if (nodes.isEmpty()) {
            LOG.debug("koncept-tree: no well-formed token line; rendering raw content as literal");
            return createBlock(parent, "literal", lines);
        }
        String backend = String.valueOf(doc.getAttribute("backend", "html5"));
        if ("html5".equals(backend)) {
            return createBlock(parent, "pass", renderHtml(doc, nodes));
        }
        // Stage 2 targets: docbook5 → FO and Prawn pdf. Until their indented projection lands, emit
        // the source tokens as a literal block so the content is visible, never dropped.
        LOG.debug("koncept-tree: backend {} not yet rendered; emitting literal tokens", backend);
        return createBlock(parent, "literal", lines);
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
        Resolved r = resolve(doc, node);
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

    // ── Resolution ──────────────────────────────────────────────────────

    /**
     * A node resolved for rendering: display label, identicon idString (empty when none),
     * component kind, glossary anchor (null for id-only nodes), and identity text.
     *
     * @param label    the display label
     * @param idString the Tinkar identicon idString, or empty
     * @param kind     the component kind
     * @param anchor   the glossary anchor identifier, or {@code null}
     * @param identity the identity text (name · id) for alt/title
     */
    private record Resolved(String label, Optional<String> idString, KonceptKind kind,
                            String anchor, String identity) {
    }

    /**
     * Resolves a parsed node <em>through {@code koncepts.yml}</em> — the resolution authority until a
     * live datastore is wired in. A name-key token resolves by identifier; a typed token
     * ({@code sctid}/{@code uuid}/{@code id}) resolves by a reverse index over the curated
     * definitions. When a definition is found, the concept's <b>PublicId is preferred</b> — explicit
     * UUIDs, in datastore order, win over a SNOMED-derived UUID — and the node gains its glossary
     * cross-reference and component kind. A typed token whose id is not curated falls back to the
     * PublicId it carries (a {@code uuid}/{@code id} array) or, last, a SNOMED-derived UUID: best
     * effort, and not guaranteed byte-identical to the live identicon — which is the reason to curate
     * the concept into {@code koncepts.yml}.
     *
     * @param doc  the document
     * @param node the parsed node
     * @return the resolved rendering data
     */
    private Resolved resolve(Document doc, ParsedNode node) {
        String bracketLabel = (node.label() != null && !node.label().isBlank())
                ? node.label().strip() : null;
        KonceptDefinitionSource source = KonceptDefinitions.forDocument(doc);
        Optional<KonceptDefinition> def = node.kind() == null
                ? source.lookup(node.value())
                : reverseIndex(doc, source).find(node.kind(), node.value()).flatMap(source::lookup);

        String label;
        KonceptKind kind;
        String anchor;
        List<UUID> uuids;
        if (def.isPresent()) {
            KonceptDefinition d = def.get();
            anchor = d.identifier();
            kind = Optional.ofNullable(d.kind()).map(KonceptKind::fromString).orElse(KonceptKind.CONCEPT);
            label = bracketLabel != null ? bracketLabel
                    : (d.label() != null && !d.label().isBlank()
                        ? d.label() : KonceptInlineMacro.splitCamelCase(d.identifier()));
            uuids = publicId(d);
        } else {
            anchor = null;                      // not curated → no glossary cross-reference
            kind = KonceptKind.CONCEPT;
            label = bracketLabel != null ? bracketLabel
                    : (node.kind() == null ? KonceptInlineMacro.splitCamelCase(node.value()) : node.value());
            uuids = uncuratedPublicId(node);
        }

        Optional<String> idString = idsEnabled(doc) && !uuids.isEmpty()
                ? Optional.of(KonceptIdentity.idString(uuids)) : Optional.empty();
        return new Resolved(label, idString, kind, anchor, identity(label, uuids, node));
    }

    /** The concept's PublicId UUIDs (explicit UUIDs preferred over the SNOMED-derived one), or empty. */
    private static List<UUID> publicId(KonceptDefinition def) {
        try {
            return KonceptIdentity.resolveUuids(def);
        } catch (RuntimeException e) {
            LOG.debug("koncept-tree: malformed uuids on {}: {}", def.identifier(), e.toString());
            return List.of();
        }
    }

    /**
     * The PublicId for a typed token that is <em>not</em> curated in {@code koncepts.yml}: a
     * {@code uuid}/{@code id} token already carries its PublicId; an {@code sctid} token falls back
     * to a SNOMED-derived UUID; a {@code nid} is a live-store id with no static form.
     *
     * @param node the parsed node
     * @return the PublicId UUIDs, or empty when none can be formed
     */
    private static List<UUID> uncuratedPublicId(ParsedNode node) {
        if (node.kind() == null) {
            return List.of();                   // a bare name absent from koncepts.yml
        }
        try {
            return switch (node.kind()) {
                case "uuid", "id" -> parseUuids(node.value());
                case "sctid" -> List.of(SnomedUuids.fromSnomed(node.value()));
                default -> List.of();           // nid
            };
        } catch (RuntimeException e) {
            LOG.debug("koncept-tree: unresolvable {}={}: {}", node.kind(), node.value(), e.toString());
            return List.of();
        }
    }

    /** Parses a single UUID or a comma-joined UUID array into an ordered list (a multi-id PublicId). */
    private static List<UUID> parseUuids(String value) {
        String[] parts = value.split(",");
        List<UUID> uuids = new ArrayList<>(parts.length);
        for (String part : parts) {
            uuids.add(UUID.fromString(part.trim()));
        }
        return uuids;
    }

    /** Honours the {@code :koncept-identicon:} toggle (default on). */
    private static boolean idsEnabled(Document doc) {
        return !"false".equalsIgnoreCase(String.valueOf(doc.getAttribute("koncept-identicon", "true")));
    }

    /**
     * Builds the identity text carried in the identicon {@code alt} and link {@code title} so a
     * concept survives — as name-plus-identity — a copy into a plain-text context. The identity is
     * the <b>PublicId</b> (the concept's UUIDs, preferred over any SNOMED id); a {@code nid} node,
     * which has no static PublicId, carries its native id instead.
     *
     * @param label the display name
     * @param uuids the PublicId UUIDs, possibly empty
     * @param node  the parsed node (for the nid fallback)
     * @return e.g. {@code "Heart Failure · f05fae71-…"}, or just the label when no id is known
     */
    private static String identity(String label, List<UUID> uuids, ParsedNode node) {
        if (!uuids.isEmpty()) {
            StringBuilder sb = new StringBuilder(label).append(" · ");
            for (int i = 0; i < uuids.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(uuids.get(i));
            }
            return sb.toString();
        }
        if ("nid".equals(node.kind())) {
            return label + " · nid " + node.value();
        }
        return label;
    }

    // ── Reverse index (typed id → curated identifier) ───────────────────

    /** Per-document cache of the reverse index over the curated definitions. */
    private static final Map<Document, ReverseIndex> REVERSE_INDEX = new WeakHashMap<>();

    private static synchronized ReverseIndex reverseIndex(Document doc, KonceptDefinitionSource source) {
        return REVERSE_INDEX.computeIfAbsent(doc, d -> ReverseIndex.build(source));
    }

    /**
     * Maps a typed id (SCTID or UUID) back to its curated koncept identifier, so {@code k:sctid=…}
     * and {@code k:uuid=…} resolve through {@code koncepts.yml} exactly as a name key does. Built by
     * enumerating the source's definitions; a source that cannot enumerate yields an empty index and
     * typed tokens fall back to the id they carry.
     *
     * @param bySctid SNOMED CT id → koncept identifier
     * @param byUuid  lowercase UUID → koncept identifier
     */
    private record ReverseIndex(Map<String, String> bySctid, Map<String, String> byUuid) {

        static ReverseIndex build(KonceptDefinitionSource source) {
            Map<String, String> bySctid = new HashMap<>();
            Map<String, String> byUuid = new HashMap<>();
            Set<String> conceptSctids = new HashSet<>();
            for (String identifier : source.identifiers()) {
                source.lookup(identifier).ifPresent(def -> {
                    boolean concept = Optional.ofNullable(def.kind()).map(KonceptKind::fromString)
                            .orElse(KonceptKind.CONCEPT) == KonceptKind.CONCEPT;
                    if (def.sctid() != null && !def.sctid().isBlank()) {
                        String sctid = def.sctid().strip();
                        // A bare SCTID means the concept — not a description or stamp that shares it.
                        if (concept) {
                            bySctid.put(sctid, identifier);
                            conceptSctids.add(sctid);
                        } else if (!conceptSctids.contains(sctid)) {
                            bySctid.putIfAbsent(sctid, identifier);
                        }
                    }
                    if (def.uuids() != null) {
                        for (String uuid : def.uuids()) {
                            byUuid.putIfAbsent(uuid.strip().toLowerCase(Locale.ROOT), identifier);
                        }
                    }
                });
            }
            return new ReverseIndex(bySctid, byUuid);
        }

        Optional<String> find(String kind, String value) {
            return switch (kind) {
                case "sctid" -> Optional.ofNullable(bySctid.get(value.strip()));
                case "uuid", "id" -> firstUuidMatch(value);
                default -> Optional.empty();     // nid is not curated by UUID
            };
        }

        private Optional<String> firstUuidMatch(String value) {
            for (String part : value.split(",")) {
                String identifier = byUuid.get(part.strip().toLowerCase(Locale.ROOT));
                if (identifier != null) {
                    return Optional.of(identifier);
                }
            }
            return Optional.empty();
        }
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
