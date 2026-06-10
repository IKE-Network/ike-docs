package network.ike.docs.plugin.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marks the differences between two versions of an AsciiDoc fragment as
 * inline {@code [.diff-ins]#…#} / {@code [.diff-del]#…#} roles, producing
 * a renderable "marked copy" for a doc-diff review packet
 * (ike-issues#648, #649).
 *
 * <p>The marker is deliberately conservative. Constructs that inline role
 * markup would corrupt are <em>protected</em> — never marked in place —
 * and their changes are reported in a per-topic change-summary
 * {@code [NOTE]} block instead:
 *
 * <ul>
 *   <li>the header region before the {@code [[anchor]]} line
 *       (comment header and attribute block);</li>
 *   <li>attribute-value continuation lines (trailing backslash);</li>
 *   <li>verbatim, listing, passthrough, and table interiors;</li>
 *   <li>block-attribute, include, indexterm, anchor, and table-cell
 *       lines, and block delimiters;</li>
 *   <li>lines carrying inline macros that word-splicing could break
 *       ({@code xref:}, {@code k:}, {@code ((…))}, {@code footnote:},
 *       {@code image:}, …);</li>
 *   <li>lines containing the role delimiter character {@code #}.</li>
 * </ul>
 *
 * <p>Markable lines get a line-level diff with word-level refinement
 * inside changed line pairs. Because IKE fragments use semantic line
 * breaks, line-level deltas are already clause-grade. Structural
 * prefixes (heading and list markers) are kept outside the role so an
 * inserted heading or list item stays syntactically valid.
 *
 * <p>All methods are pure functions of their inputs — no I/O — so the
 * marking rules are unit-testable in isolation.
 */
public final class AdocDiffMarker {

    /** Lines never marked inline; changes to them are reported as notes. */
    private static final Pattern PROTECTED = Pattern.compile(
            "^(:[A-Za-z0-9_-]+:|//|\\[\\[|\\[|include::|indexterm:|\\||<<<|\\+$|''')");

    /** Block delimiter lines (also protected). */
    private static final Pattern DELIMITERS = Pattern.compile(
            "^(----|\\.\\.\\.\\.|====|\\*\\*\\*\\*|\\+\\+\\+\\+|____|\\|===|--)\\s*$");

    /** Inline constructs that make word-splicing risky → whole-line fallback. */
    private static final Pattern RISKY_INLINE = Pattern.compile(
            "(xref:|image:|pass:|k:[A-Za-z]|\\(\\(|footnote:|btn:|kbd:)");

    /** Structural prefix kept outside the role (headings, list markers). */
    private static final Pattern STRUCTURAL_PREFIX = Pattern.compile(
            "^((?:=+\\s)|(?:\\*+\\s)|(?:\\.+\\s)|(?:-\\s)|(?:\\d+\\.\\s))");

    /** Delimiters that open a verbatim region (interior never marked). */
    private static final Pattern VERBATIM_OPEN = Pattern.compile(
            "^(----|\\.\\.\\.\\.|\\+\\+\\+\\+|\\|===)\\s*$");

    private AdocDiffMarker() {
    }

    /**
     * The result of marking one fragment: the marked lines, word-level
     * insertion/deletion counts, and the notes describing changes to
     * protected constructs.
     *
     * @param lines    the marked fragment, line by line, including the
     *                 injected change-summary note block
     * @param insWords approximate count of inserted words
     * @param delWords approximate count of deleted words
     * @param notes    human-readable notes for changes the marker could
     *                 not show inline
     */
    public record MarkResult(List<String> lines, int insWords, int delWords, List<String> notes) {
    }

    /**
     * Source of STAMP endnote refs for marked regions (ike-issues#656).
     * When provided, each change boundary's last marked line gains a
     * reusable footnote ref — collapsed to one per paragraph when every
     * boundary in the paragraph shares the same stamp.
     */
    public interface StampSource {

        /**
         * The footnote macro for an Active region (insertion or
         * replacement) whose last new-side line has the given index.
         *
         * @param newLineIndex the region's last to-side line index
         * @return the {@code footnote:id[text]} macro, or {@code null}
         *         to skip stamping this region
         */
        String activeRef(int newLineIndex);

        /**
         * The footnote macro for an Inactive region (unpaired deletion).
         *
         * @return the {@code footnote:id[text]} macro, or {@code null}
         */
        String inactiveRef();
    }

    /**
     * Produce a marked copy of a changed fragment, without stamps.
     *
     * @param oldLines the fragment's lines on the from side
     * @param newLines the fragment's lines on the to side
     * @return the marked fragment with a change-summary note injected
     *         after the level-1 title
     */
    public static MarkResult mark(List<String> oldLines, List<String> newLines) {
        return mark(oldLines, newLines, null);
    }

    /**
     * Produce a marked copy of a changed fragment, with optional STAMP
     * endnote refs per change boundary.
     *
     * @param oldLines the fragment's lines on the from side
     * @param newLines the fragment's lines on the to side
     * @param stamps   stamp-ref source, or {@code null} for no stamps
     * @return the marked fragment with a change-summary note injected
     *         after the level-1 title
     */
    public static MarkResult mark(List<String> oldLines, List<String> newLines,
                                  StampSource stamps) {
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);
        List<String> out = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        State state = new State();
        int[] words = new int[2]; // ins, del
        List<int[]> pendingRefs = new ArrayList<>();   // [outIdx]
        List<String> pendingRefText = new ArrayList<>();

        int newPos = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            // Copy the equal region preceding this delta from the new side.
            while (newPos < delta.getTarget().getPosition()) {
                String l = newLines.get(newPos);
                if (l.isBlank()) {
                    flushParagraphRefs(out, pendingRefs, pendingRefText);
                }
                emit(out, state, l);
                newPos++;
            }
            List<String> oldRun = delta.getSource().getLines();
            List<String> newRun = delta.getTarget().getLines();
            int outStart = out.size();
            boolean markedAny = applyDelta(oldRun, newRun, out, notes, state, words);
            if (stamps != null && markedAny && out.size() > outStart) {
                // Stamp the last substantive line: walk back past trailing
                // blanks (and the EOF phantom) on both the to side — for
                // blame lookup — and the output — for ref attachment.
                int lastNew = delta.getTarget().getPosition() + newRun.size() - 1;
                while (lastNew > delta.getTarget().getPosition()
                        && newLines.get(lastNew).isBlank()) {
                    lastNew--;
                }
                String ref = newRun.isEmpty()
                        ? stamps.inactiveRef()
                        : stamps.activeRef(lastNew);
                if (ref != null) {
                    int attach = out.size() - 1;
                    while (attach > outStart && out.get(attach).isBlank()) {
                        attach--;
                    }
                    if (!out.get(attach).isBlank()) {
                        pendingRefs.add(new int[] {attach});
                        pendingRefText.add(ref);
                    }
                }
            }
            newPos += newRun.size();
        }
        while (newPos < newLines.size()) {
            String l = newLines.get(newPos);
            if (l.isBlank()) {
                flushParagraphRefs(out, pendingRefs, pendingRefText);
            }
            emit(out, state, l);
            newPos++;
        }
        flushParagraphRefs(out, pendingRefs, pendingRefText);

        List<String> withNote = injectSummary(out, words[0], words[1], notes);
        return new MarkResult(withNote, words[0], words[1], notes);
    }

    /**
     * Attach the paragraph's pending stamp refs: one ref at the last
     * boundary when all boundaries share a stamp, otherwise each
     * boundary keeps its own.
     *
     * @param out            the marked stream
     * @param pendingRefs    boundary end positions in the stream
     * @param pendingRefText the corresponding footnote macros
     */
    private static void flushParagraphRefs(List<String> out, List<int[]> pendingRefs,
                                           List<String> pendingRefText) {
        if (pendingRefs.isEmpty()) {
            return;
        }
        boolean uniform = pendingRefText.stream().distinct().count() == 1;
        for (int i = 0; i < pendingRefs.size(); i++) {
            if (uniform && i < pendingRefs.size() - 1) {
                continue;
            }
            int idx = pendingRefs.get(i)[0];
            out.set(idx, out.get(idx) + pendingRefText.get(i));
        }
        pendingRefs.clear();
        pendingRefText.clear();
    }

    /** Block-attribute line opening a diagram block. */
    private static final Pattern DIAGRAM_ATTR = Pattern.compile(
            "^\\[(plantuml|graphviz|mermaid)\\b.*]\\s*$");

    /**
     * One diagram block found in a fragment.
     *
     * @param kind      the diagram kind (plantuml, graphviz, mermaid)
     * @param body      the lines between the delimiters
     * @param closeLine the index of the closing delimiter line
     */
    private record DiagramBlock(String kind, List<String> body, int closeLine) {
    }

    /**
     * Append, after every diagram block whose body meaningfully changed
     * between the two sides, the compact source delta followed by the
     * rendered previous version — so the reader sees the new drawing,
     * what changed in the spec, and the old drawing, in that order.
     * Blocks are paired per diagram kind in occurrence order; bodies
     * are compared whitespace-insensitively, so formatting-only edits
     * never produce a pair, and diagrams only present on one side are
     * left alone (the change-summary notes already report removed
     * lines).
     *
     * @param oldLines    the fragment's from-side lines
     * @param markedLines the marked fragment (its verbatim regions carry
     *                    the to-side diagram blocks unmodified)
     * @return the marked fragment with delta-plus-previous blocks
     *         inserted
     */
    public static List<String> withDiagramHistory(List<String> oldLines,
                                                  List<String> markedLines) {
        List<DiagramBlock> oldBlocks = diagramBlocks(oldLines);
        List<DiagramBlock> newBlocks = diagramBlocks(markedLines);
        List<String> out = new ArrayList<>(markedLines);
        // Bottom-up so earlier insertions don't shift later indices.
        for (int i = newBlocks.size() - 1; i >= 0; i--) {
            DiagramBlock now = newBlocks.get(i);
            DiagramBlock before = nthOfKind(oldBlocks, now.kind(), kindIndex(newBlocks, i));
            if (before == null
                    || normalized(before.body()).equals(normalized(now.body()))) {
                continue;
            }
            List<String> insert = new ArrayList<>();
            insert.add("");
            insert.add("[.diff-meta]##This diagram changed — source delta, then the previous rendering:##");
            insert.add("");
            insert.add("[source,diff]");
            insert.add("-----");
            insert.addAll(bodyDelta(before.body(), now.body()));
            insert.add("-----");
            insert.add("");
            insert.add("[" + now.kind() + "]");
            insert.add("----");
            insert.addAll(before.body());
            insert.add("----");
            out.addAll(now.closeLine() + 1, insert);
        }
        return out;
    }

    private static List<String> normalized(List<String> body) {
        return body.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private static List<String> bodyDelta(List<String> oldBody, List<String> newBody) {
        List<String> out = new ArrayList<>();
        for (AbstractDelta<String> d : DiffUtils.diff(oldBody, newBody).getDeltas()) {
            d.getSource().getLines().forEach(l -> out.add("- " + l));
            d.getTarget().getLines().forEach(l -> out.add("+ " + l));
        }
        return out;
    }

    private static List<DiagramBlock> diagramBlocks(List<String> lines) {
        List<DiagramBlock> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = DIAGRAM_ATTR.matcher(lines.get(i).strip());
            if (!m.matches()) {
                continue;
            }
            int open = i + 1;
            while (open < lines.size() && lines.get(open).isBlank()) {
                open++;
            }
            if (open >= lines.size() || !lines.get(open).strip().equals("----")) {
                continue;
            }
            int close = open + 1;
            while (close < lines.size() && !lines.get(close).strip().equals("----")) {
                close++;
            }
            if (close >= lines.size()) {
                continue;
            }
            out.add(new DiagramBlock(m.group(1),
                    List.copyOf(lines.subList(open + 1, close)), close));
            i = close;
        }
        return out;
    }

    private static int kindIndex(List<DiagramBlock> blocks, int position) {
        String kind = blocks.get(position).kind();
        int n = 0;
        for (int i = 0; i < position; i++) {
            if (blocks.get(i).kind().equals(kind)) {
                n++;
            }
        }
        return n;
    }

    private static DiagramBlock nthOfKind(List<DiagramBlock> blocks, String kind, int n) {
        int seen = 0;
        for (DiagramBlock b : blocks) {
            if (b.kind().equals(kind)) {
                if (seen == n) {
                    return b;
                }
                seen++;
            }
        }
        return null;
    }

    /**
     * Produce a full-text copy of an added fragment with a banner note
     * instead of inline marks.
     *
     * @param newLines the fragment's lines on the to side
     * @param fromRef  the from-side ref name, used in the banner text
     * @return the banner-annotated fragment
     */
    public static MarkResult markAdded(List<String> newLines, String fromRef) {
        return markAdded(newLines, fromRef, null);
    }

    /**
     * Produce a full-text copy of an added fragment with a banner note,
     * optionally stamped — an added topic is one Active stamp, carried
     * on the banner rather than in the flow.
     *
     * @param newLines the fragment's lines on the to side
     * @param fromRef  the from-side ref name, used in the banner text
     * @param stampRef the stamp footnote macro, or {@code null}
     * @return the banner-annotated fragment
     */
    public static MarkResult markAdded(List<String> newLines, String fromRef, String stampRef) {
        List<String> out = new ArrayList<>();
        boolean placed = false;
        for (String l : newLines) {
            out.add(l);
            if (!placed && l.startsWith("= ")) {
                out.add("");
                out.add("[NOTE]");
                out.add("====");
                out.add("[.diff-meta]#New topic — did not exist at " + fromRef
                        + "; full text follows.#" + (stampRef == null ? "" : stampRef));
                out.add("====");
                placed = true;
            }
        }
        int ins = newLines.stream().mapToInt(l -> l.split("\\s+").length).sum();
        return new MarkResult(out, ins, 0, List.of());
    }

    private static boolean applyDelta(List<String> oldRun, List<String> newRun,
                                      List<String> out, List<String> notes,
                                      State state, int[] words) {
        boolean markedAny = false;
        int max = Math.max(oldRun.size(), newRun.size());
        for (int k = 0; k < max; k++) {
            String o = k < oldRun.size() ? oldRun.get(k) : null;
            String n = k < newRun.size() ? newRun.get(k) : null;
            if (o != null && n != null) {
                boolean ok = inlineOk(o, state);
                boolean nk = inlineOk(n, state);
                String merged = (ok && nk) ? wordMerge(o, n) : null;
                if (merged != null) {
                    emit(out, state, merged);
                    markedAny = true;
                } else if (ok && nk) {
                    out.add(wrapPrefixed("diff-del", o));
                    emit(out, state, wrapPrefixed("diff-ins", n));
                    markedAny = true;
                } else {
                    emit(out, state, n);
                    notes.add("changed (shown as new): `" + clip(o, 60)
                            + "` -> `" + clip(n, 60) + "`");
                }
                words[0] += wordCount(n);
                words[1] += wordCount(o);
            } else if (n != null) {
                if (inlineOk(n, state)) {
                    emit(out, state, wrapPrefixed("diff-ins", n));
                    markedAny = true;
                } else {
                    emit(out, state, n);
                    if (!n.isBlank()) {
                        notes.add("added (shown unmarked): `" + clip(n, 90) + "`");
                    }
                }
                words[0] += wordCount(n);
            } else if (o != null) {
                if (inlineOk(o, state)) {
                    out.add(wrapPrefixed("diff-del", o));
                    markedAny = true;
                } else if (!o.isBlank()) {
                    notes.add("removed: `" + clip(o, 90) + "`");
                }
                words[1] += wordCount(o);
            }
        }
        return markedAny;
    }

    /**
     * Minimum share of a line pair's characters that must be unchanged
     * for word-level merging; below this the pair reads better as a
     * whole struck line followed by a whole inserted line (rewrites
     * produce unreadable interleave when word-merged).
     */
    private static final double MERGE_SIMILARITY_FLOOR = 0.4;

    /**
     * Merge one old/new line pair with word-level ins/del roles, or
     * return {@code null} when the pair's structural prefixes differ or
     * the pair is too dissimilar (a rewrite), in which case the caller
     * falls back to whole-line marking.
     *
     * @param oldLine the from-side line
     * @param newLine the to-side line
     * @return the merged line, or {@code null} when no readable merge
     *         exists
     */
    static String wordMerge(String oldLine, String newLine) {
        String[] po = splitPrefix(oldLine);
        String[] pn = splitPrefix(newLine);
        if (!po[0].equals(pn[0])) {
            return null;
        }
        List<String> ot = tokenize(po[1]);
        List<String> nt = tokenize(pn[1]);
        Patch<String> patch = DiffUtils.diff(ot, nt);
        int changed = 0;
        for (AbstractDelta<String> d : patch.getDeltas()) {
            changed += d.getSource().getLines().stream().mapToInt(String::length).sum()
                    + d.getTarget().getLines().stream().mapToInt(String::length).sum();
        }
        int total = po[1].length() + pn[1].length();
        if (total > 0 && (double) (total - changed) / total < MERGE_SIMILARITY_FLOOR) {
            return null;
        }
        StringBuilder sb = new StringBuilder(pn[0]);
        int pos = 0;
        for (AbstractDelta<String> d : patch.getDeltas()) {
            while (pos < d.getTarget().getPosition()) {
                sb.append(nt.get(pos));
                pos++;
            }
            String o = String.join("", d.getSource().getLines());
            String n = String.join("", d.getTarget().getLines());
            if (!o.isBlank()) {
                sb.append(wrap("diff-del", o.strip())).append(' ');
            } else {
                sb.append(o);
            }
            if (!n.isBlank()) {
                sb.append(wrap("diff-ins", n.strip()));
                if (n.endsWith(" ")) {
                    sb.append(' ');
                }
            } else if (!n.isEmpty() && o.isEmpty()) {
                sb.append(n);
            }
            pos += d.getTarget().getLines().size();
        }
        while (pos < nt.size()) {
            sb.append(nt.get(pos));
            pos++;
        }
        return sb.toString().stripTrailing();
    }

    /**
     * Decide whether a line may carry inline role markup given the
     * current stream state.
     *
     * @param line  the candidate line
     * @param state the marker's stream state (header, continuation,
     *              verbatim tracking)
     * @return whether inline marking is safe for this line
     */
    static boolean inlineOk(String line, State state) {
        if (state.inHeader || state.continuation || state.inVerbatim()) {
            return false;
        }
        String s = line.stripTrailing();
        if (s.isBlank() || PROTECTED.matcher(s).find() || DELIMITERS.matcher(s).matches()) {
            return false;
        }
        if (s.endsWith("\\") || s.indexOf('#') >= 0) {
            return false;
        }
        return !RISKY_INLINE.matcher(s).find();
    }

    private static void emit(List<String> out, State state, String line) {
        state.feed(line);
        out.add(line);
    }

    private static List<String> injectSummary(List<String> lines, int ins, int del,
                                              List<String> notes) {
        List<String> out = new ArrayList<>(lines.size() + 8);
        boolean placed = false;
        for (String l : lines) {
            out.add(l);
            if (!placed && l.startsWith("= ")) {
                out.add("");
                out.add("[NOTE]");
                out.add("====");
                out.add("[.diff-meta]#Change summary: ~" + ins + " words added, ~"
                        + del + " words removed.#");
                if (!notes.isEmpty()) {
                    out.add("");
                    Set<String> seen = new LinkedHashSet<>(notes);
                    for (String n : seen) {
                        out.add("* [.diff-meta]#" + n + "#");
                    }
                }
                out.add("====");
                placed = true;
            }
        }
        return out;
    }

    private static String wrap(String role, String text) {
        // Unconstrained form (##…##): a constrained role fails to parse at
        // non-word boundaries (after ';', ']', …) and leaks literally into
        // the render. Content with a literal '#' never reaches here — such
        // lines are protected by inlineOk().
        return text.isBlank() ? text : "[." + role + "]##" + text + "##";
    }

    private static String wrapPrefixed(String role, String line) {
        String[] p = splitPrefix(line);
        return p[0] + wrap(role, p[1]);
    }

    private static String[] splitPrefix(String s) {
        Matcher m = STRUCTURAL_PREFIX.matcher(s);
        if (m.find()) {
            return new String[] {m.group(0), s.substring(m.end())};
        }
        return new String[] {"", s};
    }

    private static List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int start = i;
            boolean ws = Character.isWhitespace(s.charAt(i));
            while (i < s.length() && Character.isWhitespace(s.charAt(i)) == ws) {
                i++;
            }
            tokens.add(s.substring(start, i));
        }
        return tokens;
    }

    private static int wordCount(String s) {
        String t = s.strip();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    private static String clip(String s, int max) {
        String t = s.strip();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /**
     * Mutable stream state threaded through one marking pass: whether we
     * are still in the pre-anchor header region, whether the previous
     * emitted line ended in an attribute continuation, and the verbatim
     * delimiter currently open (if any).
     */
    static final class State {
        private boolean inHeader = true;
        private boolean continuation;
        private String verbatim;

        /**
         * Update the state from one emitted line.
         *
         * @param line the line just emitted to the marked stream
         */
        void feed(String line) {
            if (inHeader && line.startsWith("[[")) {
                inHeader = false;
            }
            continuation = line.stripTrailing().endsWith("\\");
            String s = line.strip();
            if (verbatim == null) {
                if (VERBATIM_OPEN.matcher(s).matches()) {
                    verbatim = s;
                }
            } else if (s.equals(verbatim)) {
                verbatim = null;
            }
        }

        /**
         * Whether the stream is currently inside a verbatim region.
         *
         * @return whether a verbatim delimiter is open
         */
        boolean inVerbatim() {
            return verbatim != null;
        }
    }
}
