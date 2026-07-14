package network.ike.docs.koncept;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Computed graph views over every known koncept's {@code broader} (is-a parent)
 * relationship — no curated data of its own, just walks of what
 * {@link KonceptDefinitionSource} already carries. Shared by
 * {@link KonceptGlossaryTreeprocessor} (children-of-a-group for the glossary) and
 * {@link KonceptTreeBlockProcessor} ({@code leaves:}/{@code children:} query
 * directives inside a {@code [koncept-tree]} block, IKE-Network/ike-issues#879).
 */
final class KonceptGraph {

    private KonceptGraph() {
    }

    /**
     * Inverts every known koncept's {@code broader} parents into a children map.
     *
     * @param allIds    every known koncept identifier
     * @param defSource the definition source to read {@code broader} from
     * @return a map from identifier to its direct children, in first-seen order
     */
    static Map<String, List<String>> invertBroader(Collection<String> allIds, KonceptDefinitionSource defSource) {
        Map<String, List<String>> childrenById = new TreeMap<>();
        for (String id : allIds) {
            for (String parent : defSource.lookup(id).map(KonceptDefinition::broader).orElse(List.of())) {
                childrenById.computeIfAbsent(parent, key -> new ArrayList<>()).add(id);
            }
        }
        return childrenById;
    }

    /**
     * Every leaf descendant of {@code rootId} — nodes reachable by following
     * {@code childrenById} that have no children of their own. A node with a cyclic
     * {@code broader} chain is visited at most once, so a data error degrades to an
     * incomplete list rather than an infinite walk.
     *
     * @param rootId      the koncept identifier to walk descendants from
     * @param childrenById the children map, as built by {@link #invertBroader}
     * @return every leaf descendant, in a depth-first, first-seen order
     */
    static List<String> leaves(String rootId, Map<String, List<String>> childrenById) {
        List<String> out = new ArrayList<>();
        collectLeaves(rootId, childrenById, out, new HashSet<>());
        return out;
    }

    private static void collectLeaves(String id, Map<String, List<String>> childrenById,
                                       List<String> out, Set<String> visited) {
        if (!visited.add(id)) {
            return;
        }
        List<String> children = childrenById.get(id);
        if (children == null || children.isEmpty()) {
            out.add(id);
            return;
        }
        for (String child : children) {
            collectLeaves(child, childrenById, out, visited);
        }
    }

    /**
     * Every descendant of {@code rootId}, not including {@code rootId} itself — nodes
     * reachable by following {@code childrenById}, at any depth. A node with a cyclic
     * {@code broader} chain is visited at most once, so a data error degrades to an
     * incomplete list rather than an infinite walk.
     *
     * @param rootId       the koncept identifier to walk descendants from
     * @param childrenById the children map, as built by {@link #invertBroader}
     * @return every descendant, in a depth-first, first-seen order
     */
    static List<String> descendants(String rootId, Map<String, List<String>> childrenById) {
        List<String> out = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(rootId);
        collectDescendants(rootId, childrenById, out, visited);
        return out;
    }

    private static void collectDescendants(String id, Map<String, List<String>> childrenById,
                                            List<String> out, Set<String> visited) {
        for (String child : childrenById.getOrDefault(id, List.of())) {
            if (visited.add(child)) {
                out.add(child);
                collectDescendants(child, childrenById, out, visited);
            }
        }
    }

    /**
     * {@code rootId} itself plus every one of its descendants — Tinkar's own "kind of"
     * relation.
     *
     * @param rootId       the koncept identifier
     * @param childrenById the children map, as built by {@link #invertBroader}
     * @return {@code rootId} followed by every descendant, in a depth-first, first-seen order
     */
    static List<String> kindOf(String rootId, Map<String, List<String>> childrenById) {
        List<String> out = new ArrayList<>();
        out.add(rootId);
        out.addAll(descendants(rootId, childrenById));
        return out;
    }
}
