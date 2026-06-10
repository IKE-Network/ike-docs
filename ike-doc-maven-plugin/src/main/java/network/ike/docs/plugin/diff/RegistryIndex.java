package network.ike.docs.plugin.diff;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One side's view of the IKE topic registry: topic id → source file and
 * assembly id → flattened topic-refs. This is what lets an assembly
 * module's {@code idoc:diff} run <em>project</em> the corpus diff onto
 * its own membership (ike-issues#649 scoping decision) — the registry,
 * not include-line parsing, is the source of truth for which topics an
 * assembly contains.
 *
 * <p>Also hosts the registry YAML shape helpers shared with
 * {@link RegistryDelta}.
 */
public final class RegistryIndex {

    private final Map<String, String> topicFilesById;
    private final Map<String, List<String>> assemblyRefsById;

    private RegistryIndex(Map<String, String> topicFilesById,
                          Map<String, List<String>> assemblyRefsById) {
        this.topicFilesById = topicFilesById;
        this.assemblyRefsById = assemblyRefsById;
    }

    /**
     * Load the registry as it stands on one side of a comparison.
     *
     * @param git          repository access
     * @param ref          the side to read ({@link GitSource#WORKTREE}
     *                     or a committish) — projections use the to
     *                     side, so membership reflects the state under
     *                     review
     * @param registryRoot repository-relative source root that contains
     *                     {@code topic-registry.yaml} and
     *                     {@code topic-registry/}
     * @return the index (empty maps when no registry exists on that side)
     * @throws IOException on repository access failure
     */
    public static RegistryIndex load(GitSource git, String ref, String registryRoot)
            throws IOException {
        Map<String, String> topics = new LinkedHashMap<>();
        for (String path : git.listYaml(ref, registryRoot + "/topic-registry")) {
            if (path.endsWith("/assemblies.yaml")) {
                continue;
            }
            topicsOf(parse(git.read(ref, path))).forEach((id, entry) ->
                    topics.put(id, String.valueOf(entry.get("file"))));
        }
        Map<String, List<String>> assemblies =
                assemblyRefsOf(parse(git.read(ref, registryRoot + "/topic-registry/assemblies.yaml")));
        return new RegistryIndex(topics, assemblies);
    }

    /**
     * Resolve a topic id to its source file, relative to the registry
     * root (e.g. {@code topics/arch/asg-substrate.adoc}).
     *
     * @param topicId the topic id
     * @return the registry-root-relative file, or {@code null} when the
     *         id is unknown
     */
    public String topicFile(String topicId) {
        return topicFilesById.get(topicId);
    }

    /**
     * An assembly's flattened topic-refs, in document order.
     *
     * @param assemblyId the assembly id (by IKE naming convention equal
     *                   to the module's artifactId)
     * @return the topic ids, or {@code null} when the assembly is not
     *         registered
     */
    public List<String> assemblyRefs(String assemblyId) {
        return assemblyRefsById.get(assemblyId);
    }

    /**
     * Render one assembly's membership delta (topic-refs added and
     * removed between two sides) as a small registry-delta partial for
     * an assembly-projection packet.
     *
     * @param git          repository access
     * @param fromRef      the from side
     * @param toRef        the to side (may be {@link GitSource#WORKTREE})
     * @param registryRoot the registry source root
     * @param assemblyId   the assembly to report on
     * @return the partial's text with a level-1 title, or an empty
     *         string when this assembly's membership is unchanged
     * @throws IOException on repository access failure
     */
    public static String membershipDelta(GitSource git, String fromRef, String toRef,
                                         String registryRoot, String assemblyId)
            throws IOException {
        String path = registryRoot + "/topic-registry/assemblies.yaml";
        List<String> oldRefs = assemblyRefsOf(parse(git.read(fromRef, path)))
                .getOrDefault(assemblyId, List.of());
        List<String> newRefs = assemblyRefsOf(parse(git.read(toRef, path)))
                .getOrDefault(assemblyId, List.of());
        List<String> plus = newRefs.stream().filter(r -> !oldRefs.contains(r)).toList();
        List<String> minus = oldRefs.stream().filter(r -> !newRefs.contains(r)).toList();
        if (plus.isEmpty() && minus.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[[registry-delta]]\n= Membership Delta — ")
                .append(assemblyId).append("\n\nTopic-refs changed for this assembly between ")
                .append(fromRef).append(" and ")
                .append(GitSource.WORKTREE.equals(toRef) ? "the working tree" : toRef)
                .append(".\n\n");
        plus.forEach(r -> sb.append("* added `").append(r).append("`\n"));
        minus.forEach(r -> sb.append("* removed `").append(r).append("`\n"));
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Parse one YAML document with safe construction (registry files
     * contain dates).
     *
     * @param text the YAML text, possibly {@code null}
     * @return the parsed root, or {@code null} for absent/blank input
     */
    static Object parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return new Yaml(new SafeConstructor(new LoaderOptions())).load(text);
    }

    /**
     * Extract a per-domain registry file's topics keyed by id. Accepts
     * both the indented-fragment shape (a top-level sequence of one
     * domain) and a root map with a {@code domains} list.
     *
     * @param root the parsed YAML root
     * @return topic entries keyed by id (insertion-ordered)
     */
    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Object>> topicsOf(Object root) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        List<?> domains = root instanceof List<?> l ? l
                : root instanceof Map<?, ?> m && m.get("domains") instanceof List<?> l2 ? l2
                : List.of();
        for (Object d : domains) {
            if (d instanceof Map<?, ?> dm && dm.get("topics") instanceof List<?> ts) {
                for (Object t : ts) {
                    if (t instanceof Map<?, ?> tm && tm.get("id") != null) {
                        out.put(String.valueOf(tm.get("id")), (Map<String, Object>) tm);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Extract every assembly's flattened topic-refs from a parsed
     * {@code assemblies.yaml}. Accepts both a bare sequence and a root
     * map with an {@code assemblies} list.
     *
     * @param root the parsed YAML root
     * @return flattened refs keyed by assembly id
     */
    static Map<String, List<String>> assemblyRefsOf(Object root) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        Object assemblies = root instanceof Map<?, ?> m && m.get("assemblies") != null
                ? m.get("assemblies") : root;
        if (assemblies instanceof List<?> list) {
            for (Object a : list) {
                if (a instanceof Map<?, ?> am && am.get("id") != null) {
                    List<String> refs = new ArrayList<>();
                    collectRefs(am.get("sections"), refs);
                    out.put(String.valueOf(am.get("id")), refs);
                }
            }
        }
        return out;
    }

    private static void collectRefs(Object sections, List<String> refs) {
        if (sections instanceof List<?> list) {
            for (Object s : list) {
                if (s instanceof Map<?, ?> sm) {
                    if (sm.get("topic-refs") instanceof List<?> tr) {
                        tr.forEach(r -> refs.add(String.valueOf(r)));
                    }
                    collectRefs(sm.get("sections"), refs);
                }
            }
        }
    }
}
