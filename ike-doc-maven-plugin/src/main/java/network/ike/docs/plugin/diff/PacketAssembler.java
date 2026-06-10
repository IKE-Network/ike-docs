package network.ike.docs.plugin.diff;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.DiffUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Composes the review packet's generated AsciiDoc (ike-issues#648):
 * the master document with cover sheet, change summary, Record of
 * Changes, per-topic includes, Change Glossary, and Change Index, plus
 * the unified-diff listings for assembly scaffolding files.
 *
 * <p>All methods are pure functions over already-loaded content — the
 * mojo owns every file read and write.
 */
public final class PacketAssembler {

    private PacketAssembler() {
    }

    /**
     * One marked topic destined for the packet.
     *
     * @param change  the underlying file change
     * @param outName the flattened file name of the marked copy under
     *                {@code _diff/}
     * @param result  the marker's output for this topic
     */
    public record TopicEntry(GitSource.Change change, String outName,
                             AdocDiffMarker.MarkResult result) {
    }

    /**
     * One scaffolding (non-fragment) AsciiDoc change shown as a
     * unified-diff listing.
     *
     * @param path        the repository-relative path
     * @param unifiedDiff the unified diff text
     */
    public record ScaffoldEntry(String path, String unifiedDiff) {
    }

    /**
     * Produce a unified diff for a scaffolding file.
     *
     * @param path     the repository-relative path (used in headers)
     * @param oldLines the from-side lines (empty when added)
     * @param newLines the to-side lines (empty when deleted)
     * @return the unified diff text, without trailing newline
     */
    public static String unifiedDiff(String path, List<String> oldLines, List<String> newLines) {
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);
        List<String> diff = UnifiedDiffUtils.generateUnifiedDiff(
                "a/" + path, "b/" + path, oldLines, patch, 3);
        return String.join("\n", diff);
    }

    /**
     * Inject one silent change-index term per owning change after a
     * fragment's level-1 title, so the ordinary {@code [index]} section
     * collects a change → pages mapping. Titles are quoted because a
     * comma in an indexterm splits levels.
     *
     * @param lines        the marked fragment
     * @param changeTitles the owning changes' titles
     * @return the fragment with terms injected (a new list)
     */
    public static List<String> injectChangeTerms(List<String> lines, List<String> changeTitles) {
        if (changeTitles.isEmpty()) {
            return lines;
        }
        List<String> out = new ArrayList<>(lines.size() + changeTitles.size());
        boolean placed = false;
        for (String l : lines) {
            out.add(l);
            if (!placed && l.startsWith("= ")) {
                for (String t : changeTitles) {
                    out.add("indexterm:[change, \"" + t.replace("\"", "'") + "\"]");
                }
                placed = true;
            }
        }
        return out;
    }

    /**
     * Find a fragment's anchor id ({@code [[id]]}).
     *
     * @param lines the fragment
     * @return the anchor id, or {@code null} when none is present
     */
    public static String anchorOf(List<String> lines) {
        for (String l : lines) {
            if (l.startsWith("[[") && l.endsWith("]]")) {
                return l.substring(2, l.length() - 2);
            }
        }
        return null;
    }

    /**
     * Compose the packet master document.
     *
     * @param title         the document title
     * @param fromLabel     human label for the from side
     * @param toLabel       human label for the to side
     * @param topics        marked topics, in presentation order
     * @param deleted       repository-relative paths deleted in range
     * @param manifest      the change manifest
     * @param anchorsByChange per-change anchor ids for context links,
     *                      parallel to {@code manifest.changes()}
     * @param hasRegistryDelta whether a registry-delta partial exists
     * @param scaffolds     scaffolding diffs, in presentation order
     * @param singleStampLine the packet's one stamp when in-flow refs
     *                      are suppressed, or {@code null}
     * @param stampsUsed    the stamps used in-flow, for the Stamp
     *                      Register; empty when suppressed
     * @return the master document text
     */
    public static String masterDoc(String title, String fromLabel, String toLabel,
                                   List<TopicEntry> topics, List<String> deleted,
                                   ChangeManifest manifest,
                                   List<List<String>> anchorsByChange,
                                   boolean hasRegistryDelta,
                                   List<ScaffoldEntry> scaffolds,
                                   String singleStampLine,
                                   List<StampRegistry.Stamp> stampsUsed) {
        StringBuilder m = new StringBuilder();
        m.append("= ").append(title).append('\n')
         .append(":doctype: book\n:toc: left\n:toclevels: 1\n:sectnums!:\n")
         .append(":icons: font\n:source-highlighter: rouge\n\n")
         .append("ifdef::backend-html5[]\n++++\n<style>\n")
         .append("span.diff-ins{color:#1a7f37;background:#e6ffec;}\n")
         .append("span.diff-del{color:#cf222e;background:#ffebe9;text-decoration:line-through;}\n")
         .append("span.diff-meta{color:#6e40c9;}\n")
         .append("</style>\n++++\nendif::[]\n\n");

        m.append("== How to Read This Packet\n\n")
         .append("Compared: `").append(fromLabel).append("` → `").append(toLabel).append("`.\n")
         .append("Each changed topic follows as its own chapter with inline markup:\n")
         .append("[.diff-ins]#inserted text renders like this#, and\n")
         .append("[.diff-del]#removed text renders like this#.\n")
         .append("[.diff-meta]#Metadata notes render like this# and carry changes the\n")
         .append("renderer cannot show inline (attributes, keywords, index terms,\n")
         .append("verbatim blocks).\n");
        if (singleStampLine != null) {
            m.append("All changes in this packet carry one stamp — ")
             .append(singleStampLine).append(" — so per-change stamp endnotes are omitted.\n");
        } else if (!stampsUsed.isEmpty()) {
            m.append("Superscript endnotes stamp each change boundary with its\n")
             .append("provenance coordinate; the Stamp Register at the back lists them.\n");
        }
        m.append('\n');

        m.append("== Change Summary\n\n.Changed topics\n")
         .append("[cols=\"4,1,1,1,1\", options=\"header\"]\n|===\n")
         .append("| Topic file | status | +words | -words | notes\n");
        for (TopicEntry t : topics) {
            m.append("| ").append(t.change().displayPath())
             .append(" | ").append(t.change().status().name().toLowerCase())
             .append(" | ").append(t.result().insWords())
             .append(" | ").append(t.result().delWords())
             .append(" | ").append(t.result().notes().size()).append('\n');
        }
        for (String d : deleted) {
            m.append("| ").append(d).append(" | deleted | 0 | — | —\n");
        }
        m.append("|===\n\n");

        m.append("== Record of Changes\n\n")
         .append("Each named change, with context links into the marked topics;\n")
         .append("the Change Index at the back resolves each change to its pages.\n\n")
         .append("[cols=\"2,4,3\", options=\"header\"]\n|===\n")
         .append("| Change | Description | Context\n");
        List<ChangeManifest.ChangeEntity> changes = manifest.changes();
        for (int i = 0; i < changes.size(); i++) {
            ChangeManifest.ChangeEntity c = changes.get(i);
            List<String> anchors = anchorsByChange.get(i);
            String links = anchors.isEmpty()
                    ? "(registry/assembly only)"
                    : String.join(", ", anchors.stream().map(a -> "xref:" + a + "[]").toList());
            m.append("| *").append(c.title()).append("*\n`").append(c.id()).append('`')
             .append(refsSuffix(c))
             .append(" | ").append(c.description())
             .append(" | ").append(links).append('\n');
        }
        m.append("|===\n\n");

        for (TopicEntry t : topics) {
            m.append("<<<\ninclude::_diff/").append(t.outName())
             .append("[leveloffset=+1]\n\n");
        }
        if (hasRegistryDelta) {
            m.append("<<<\ninclude::_diff/registry-delta.adoc[leveloffset=+1]\n\n");
        }
        if (!scaffolds.isEmpty()) {
            m.append("<<<\n== Assembly Scaffolding Changes\n\n")
             .append("Source diffs for assembly files (structural includes, not prose).\n\n");
            for (ScaffoldEntry s : scaffolds) {
                m.append("=== ").append(s.path()).append("\n\n[source,diff]\n-----\n")
                 .append(s.unifiedDiff()).append("\n-----\n\n");
            }
        }

        if (!stampsUsed.isEmpty()) {
            m.append("<<<\n== Stamp Register\n\n")
             .append("Every stamp used in this packet. S is stated redundantly —\n")
             .append("the marks already render it — for STAMP consistency.\n\n")
             .append("[cols=\"1,2,2,1,1,1,2\", options=\"header\"]\n|===\n")
             .append("| S | Time | Author | Module | Path | Commit | Refs\n");
            for (StampRegistry.Stamp s : stampsUsed) {
                m.append("| ").append(s.status().label())
                 .append(" | ").append(s.time())
                 .append(" | ").append(s.author())
                 .append(" | ").append(s.module())
                 .append(" | ").append(s.branch())
                 .append(" | `").append(s.commitId()).append('`')
                 .append(" | ").append(s.refs().isEmpty() ? "—" : s.refs())
                 .append('\n');
            }
            m.append("|===\n\n");
        }

        m.append("<<<\n[glossary]\n== Change Glossary\n\n");
        for (int i = 0; i < changes.size(); i++) {
            ChangeManifest.ChangeEntity c = changes.get(i);
            m.append(c.title()).append("::\n  ").append(c.description()).append('\n');
            List<String> anchors = anchorsByChange.get(i);
            if (!anchors.isEmpty()) {
                m.append("  Context: ")
                 .append(String.join(", ",
                         anchors.stream().map(a -> "xref:" + a + "[]").toList()))
                 .append(".\n");
            }
            m.append('\n');
        }

        m.append("<<<\n[index]\n== Change Index\n");
        return m.toString();
    }

    private static String refsSuffix(ChangeManifest.ChangeEntity c) {
        if (c.refs().isEmpty()) {
            return "";
        }
        return "\n_" + String.join(", ", c.refs()) + "_";
    }
}
