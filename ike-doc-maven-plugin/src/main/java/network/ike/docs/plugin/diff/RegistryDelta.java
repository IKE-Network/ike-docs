package network.ike.docs.plugin.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Entry-keyed delta of the IKE topic registry between the two sides of
 * a doc-diff comparison, rendered as an AsciiDoc partial
 * (ike-issues#650). Both sides are generated from the files on that side
 * ({@link RegistryIndex#load}); no hand-kept registry is read.
 *
 * <p>Always keyed by entry id, never by line. Output is condensed for
 * review — {@code summary}/{@code notes} changes are flagged as rewritten
 * rather than reproduced, list fields report added/removed items, scalars
 * report old → new. Covers per-domain topic entries, the topic counts,
 * and assembly membership.
 */
public final class RegistryDelta {

    private final String fromRef;
    private final String toRef;

    /**
     * Create a delta generator over one comparison.
     *
     * @param fromRef the from-side ref
     * @param toRef   the to-side ref (may be {@link GitSource#WORKTREE})
     */
    public RegistryDelta(String fromRef, String toRef) {
        this.fromRef = fromRef;
        this.toRef = toRef;
    }

    /**
     * Render the delta as an AsciiDoc partial with a level-1 title.
     *
     * @param oldIndex the registry generated for the from side
     * @param newIndex the registry generated for the to side
     * @return the partial's text, or an empty string when nothing in
     *         the registry changed
     */
    public String render(RegistryIndex oldIndex, RegistryIndex newIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("[[registry-delta]]\n= Registry Delta\n\n")
          .append("Entry-level changes to the topic registry, ")
          .append(fromRef).append(" vs ").append(toRef.equals(GitSource.WORKTREE)
                  ? "working tree" : toRef).append(".\n")
          .append("Summary and notes fields are flagged as rewritten, not reproduced.\n\n");
        boolean any = false;

        Set<String> domains = new TreeSet<>(oldIndex.topicsByDomain().keySet());
        domains.addAll(newIndex.topicsByDomain().keySet());
        for (String domain : domains) {
            any |= domainDelta(sb, domain,
                    oldIndex.topicsByDomain().getOrDefault(domain, Map.of()),
                    newIndex.topicsByDomain().getOrDefault(domain, Map.of()));
        }
        any |= countDelta(sb, oldIndex, newIndex);
        any |= assembliesDelta(sb, oldIndex.assemblies(), newIndex.assemblies());
        return any ? sb.toString() : "";
    }

    private boolean domainDelta(StringBuilder sb, String domain,
                                Map<String, Map<String, Object>> oldT,
                                Map<String, Map<String, Object>> newT) {
        List<String> added = newT.keySet().stream().filter(k -> !oldT.containsKey(k)).toList();
        List<String> removed = oldT.keySet().stream().filter(k -> !newT.containsKey(k)).toList();
        List<String> changed = newT.keySet().stream()
                .filter(oldT::containsKey)
                .filter(k -> !Objects.equals(oldT.get(k), newT.get(k)))
                .toList();
        if (added.isEmpty() && removed.isEmpty() && changed.isEmpty()) {
            return false;
        }
        sb.append("== ").append(domain).append("\n\n");
        if (!added.isEmpty()) {
            sb.append(".New entries (").append(added.size()).append(")\n");
            for (String id : added) {
                Map<String, Object> t = newT.get(id);
                sb.append("* `").append(id).append("` — ").append(t.get("title"))
                  .append(" (").append(t.get("type")).append('/')
                  .append(t.get("status")).append(")\n");
            }
            sb.append('\n');
        }
        if (!removed.isEmpty()) {
            sb.append(".Removed entries (").append(removed.size()).append(")\n");
            for (String id : removed) {
                sb.append("* `").append(id).append("`\n");
            }
            sb.append('\n');
        }
        if (!changed.isEmpty()) {
            sb.append(".Changed entries (").append(changed.size()).append(")\n");
            for (String id : changed) {
                sb.append("* `").append(id).append("` — ")
                  .append(fieldDelta(oldT.get(id), newT.get(id))).append('\n');
            }
            sb.append('\n');
        }
        return true;
    }

    /**
     * Describe the field-level differences between two entries in
     * condensed form.
     *
     * @param oldEntry the from-side entry
     * @param newEntry the to-side entry
     * @return a semicolon-joined description of changed fields
     */
    static String fieldDelta(Map<String, Object> oldEntry, Map<String, Object> newEntry) {
        Set<String> keys = new LinkedHashSet<>(oldEntry.keySet());
        keys.addAll(newEntry.keySet());
        List<String> parts = new ArrayList<>();
        for (String k : keys) {
            Object o = oldEntry.get(k);
            Object n = newEntry.get(k);
            if (Objects.equals(o, n)) {
                continue;
            }
            switch (k) {
                case "summary", "notes" -> parts.add(k + " rewritten");
                case "related", "dependencies", "keywords", "issues" -> {
                    List<?> ol = o instanceof List<?> l ? l : List.of();
                    List<?> nl = n instanceof List<?> l ? l : List.of();
                    List<String> plus = nl.stream().filter(x -> !ol.contains(x))
                            .map(String::valueOf).toList();
                    List<String> minus = ol.stream().filter(x -> !nl.contains(x))
                            .map(String::valueOf).toList();
                    StringBuilder f = new StringBuilder(k);
                    if (!plus.isEmpty()) {
                        f.append(" +[").append(String.join(", ", plus)).append(']');
                    }
                    if (!minus.isEmpty()) {
                        f.append(" -[").append(String.join(", ", minus)).append(']');
                    }
                    parts.add(f.toString());
                }
                default -> parts.add(k + ": " + clip(o) + " → " + clip(n));
            }
        }
        return String.join("; ", parts);
    }

    private boolean countDelta(StringBuilder sb, RegistryIndex oldIndex, RegistryIndex newIndex) {
        List<String> lines = new ArrayList<>();
        int oldTotal = total(oldIndex);
        int newTotal = total(newIndex);
        if (oldTotal != newTotal) {
            lines.add("* total topic-count: " + oldTotal + " → " + newTotal);
        }
        Set<String> domains = new TreeSet<>(oldIndex.topicsByDomain().keySet());
        domains.addAll(newIndex.topicsByDomain().keySet());
        for (String d : domains) {
            Map<String, Map<String, Object>> o = oldIndex.topicsByDomain().get(d);
            Map<String, Map<String, Object>> n = newIndex.topicsByDomain().get(d);
            int oc = o == null ? 0 : o.size();
            int nc = n == null ? 0 : n.size();
            if (oc != nc) {
                lines.add("* " + d + ": " + (o == null ? "new domain" : oc) + " → " + nc);
            }
        }
        if (lines.isEmpty()) {
            return false;
        }
        sb.append("== Topic counts\n\n");
        lines.forEach(l -> sb.append(l).append('\n'));
        sb.append('\n');
        return true;
    }

    private static int total(RegistryIndex index) {
        return index.topicsByDomain().values().stream().mapToInt(Map::size).sum();
    }

    private boolean assembliesDelta(StringBuilder sb, Map<String, List<String>> oldRefs,
                                    Map<String, List<String>> newRefs) {
        Set<String> ids = new LinkedHashSet<>(oldRefs.keySet());
        ids.addAll(newRefs.keySet());
        List<String> lines = new ArrayList<>();
        for (String id : ids) {
            List<String> ol = oldRefs.getOrDefault(id, List.of());
            List<String> nl = newRefs.getOrDefault(id, List.of());
            List<String> plus = nl.stream().filter(x -> !ol.contains(x)).toList();
            List<String> minus = ol.stream().filter(x -> !nl.contains(x)).toList();
            if (plus.isEmpty() && minus.isEmpty()) {
                continue;
            }
            StringBuilder f = new StringBuilder("* `" + id + "` —");
            if (!plus.isEmpty()) {
                f.append(" +[").append(String.join(", ", plus)).append(']');
            }
            if (!minus.isEmpty()) {
                f.append(" -[").append(String.join(", ", minus)).append(']');
            }
            lines.add(f.toString());
        }
        if (lines.isEmpty()) {
            return false;
        }
        sb.append("== Assemblies (included topics)\n\n");
        lines.forEach(l -> sb.append(l).append('\n'));
        sb.append('\n');
        return true;
    }

    private static String clip(Object o) {
        String s = String.valueOf(o);
        return s.length() <= 40 ? s : s.substring(0, 40);
    }
}
