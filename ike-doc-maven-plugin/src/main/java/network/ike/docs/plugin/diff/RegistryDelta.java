package network.ike.docs.plugin.diff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Entry-keyed delta of the IKE topic registry between the two sides of
 * a doc-diff comparison, rendered as an AsciiDoc partial
 * (ike-issues#650).
 *
 * <p>Always keyed by entry id, never by line: the registry is YAML and
 * ids are its stable keys. Output is condensed for review —
 * {@code summary}/{@code notes} changes are flagged as rewritten rather
 * than reproduced, list fields report added/removed items, scalars
 * report old → new. Covers the per-domain files, the root index counts,
 * and assembly topic-ref membership.
 */
public final class RegistryDelta {

    private final GitSource git;
    private final String fromRef;
    private final String toRef;

    /**
     * Create a delta generator over one comparison.
     *
     * @param git     the repository access
     * @param fromRef the from-side ref
     * @param toRef   the to-side ref (may be {@link GitSource#WORKTREE})
     */
    public RegistryDelta(GitSource git, String fromRef, String toRef) {
        this.git = git;
        this.fromRef = fromRef;
        this.toRef = toRef;
    }

    /**
     * Render the delta as an AsciiDoc partial with a level-1 title.
     *
     * @param rootRegistry repository-relative path of the thin root
     *                     index ({@code …/topic-registry.yaml})
     * @param registryDir  repository-relative directory of the
     *                     per-domain files ({@code …/topic-registry})
     * @return the partial's text, or an empty string when nothing in
     *         the registry changed
     * @throws IOException on repository access failure
     */
    public String render(String rootRegistry, String registryDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[[registry-delta]]\n= Registry Delta\n\n")
          .append("Entry-level changes to the topic registry, ")
          .append(fromRef).append(" vs ").append(toRef.equals(GitSource.WORKTREE)
                  ? "working tree" : toRef).append(".\n")
          .append("Summary and notes fields are flagged as rewritten, not reproduced.\n\n");
        boolean any = false;

        Set<String> files = new LinkedHashSet<>();
        files.addAll(git.listYaml(fromRef, registryDir));
        files.addAll(git.listYaml(toRef, registryDir));
        for (String path : files) {
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.equals("assemblies.yaml")) {
                continue;
            }
            any |= domainDelta(sb, name.replace(".yaml", ""), path);
        }
        any |= rootDelta(sb, rootRegistry);
        any |= assembliesDelta(sb, registryDir + "/assemblies.yaml");
        return any ? sb.toString() : "";
    }

    private boolean domainDelta(StringBuilder sb, String domain, String path) throws IOException {
        Map<String, Map<String, Object>> oldT = RegistryIndex.topicsOf(load(fromRef, path));
        Map<String, Map<String, Object>> newT = RegistryIndex.topicsOf(load(toRef, path));
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

    private boolean rootDelta(StringBuilder sb, String rootRegistry) throws IOException {
        Object oldRoot = load(fromRef, rootRegistry);
        Object newRoot = load(toRef, rootRegistry);
        if (!(oldRoot instanceof Map<?, ?> om) || !(newRoot instanceof Map<?, ?> nm)) {
            return false;
        }
        List<String> lines = new ArrayList<>();
        if (!Objects.equals(om.get("topic-count"), nm.get("topic-count"))) {
            lines.add("* total topic-count: " + om.get("topic-count")
                    + " → " + nm.get("topic-count"));
        }
        Map<String, Object> oldCounts = domainCounts(om.get("domains"));
        for (Map.Entry<String, Object> e : domainCounts(nm.get("domains")).entrySet()) {
            Object o = oldCounts.get(e.getKey());
            if (!Objects.equals(o, e.getValue())) {
                lines.add("* " + e.getKey() + ": " + (o == null ? "new domain" : o)
                        + " → " + e.getValue());
            }
        }
        if (lines.isEmpty()) {
            return false;
        }
        sb.append("== Root index\n\n");
        lines.forEach(l -> sb.append(l).append('\n'));
        sb.append('\n');
        return true;
    }

    private boolean assembliesDelta(StringBuilder sb, String path) throws IOException {
        Map<String, List<String>> oldRefs = RegistryIndex.assemblyRefsOf(load(fromRef, path));
        Map<String, List<String>> newRefs = RegistryIndex.assemblyRefsOf(load(toRef, path));
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
        sb.append("== Assemblies (topic-refs)\n\n");
        lines.forEach(l -> sb.append(l).append('\n'));
        sb.append('\n');
        return true;
    }

    private Object load(String ref, String path) throws IOException {
        return RegistryIndex.parse(git.read(ref, path));
    }

    private static Map<String, Object> domainCounts(Object domains) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (domains instanceof List<?> list) {
            for (Object d : list) {
                if (d instanceof Map<?, ?> dm && dm.get("id") != null) {
                    out.put(String.valueOf(dm.get("id")), dm.get("topic-count"));
                }
            }
        }
        return out;
    }

    private static String clip(Object o) {
        String s = String.valueOf(o);
        return s.length() <= 40 ? s : s.substring(0, 40);
    }
}
