package network.ike.docs.plugin.diff;

import network.ike.docs.plugin.registry.TopicHeader;
import network.ike.docs.plugin.registry.TopicRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One side's topic registry, generated from the files as they stand on
 * that side of a doc-diff comparison: topics grouped by domain (each entry
 * in the shape {@code idoc:topic-registry} writes, built from the
 * {@code :topic-*:} header), and each assembly's topic membership read
 * from its {@code include::} lines. No hand-kept registry file is read.
 *
 * <p>This is what lets an assembly module's {@code idoc:diff} run
 * <em>project</em> the corpus diff onto its own membership
 * (ike-issues#649), and what {@link RegistryDelta} compares.
 */
public final class RegistryIndex {

    /** An {@code include::} directive's target, up to the attribute list. */
    private static final Pattern INCLUDE = Pattern.compile("^include::([^\\[]+)\\[", Pattern.MULTILINE);

    /** A leading attribute reference such as {@code {topicsdir}/}. */
    private static final Pattern LEADING_ATTRIBUTE = Pattern.compile("^\\{[^}]+\\}/");

    private static final String SOURCE_ROOT = "src/docs/asciidoc";

    private final Map<String, Map<String, Map<String, Object>>> topicsByDomain;
    private final Map<String, String> topicFilesById;
    private final Map<String, List<String>> assemblyRefsById;

    private RegistryIndex(Map<String, Map<String, Map<String, Object>>> topicsByDomain,
                          Map<String, String> topicFilesById,
                          Map<String, List<String>> assemblyRefsById) {
        this.topicsByDomain = topicsByDomain;
        this.topicFilesById = topicFilesById;
        this.assemblyRefsById = assemblyRefsById;
    }

    /**
     * Generate the registry as it stands on one side of a comparison.
     *
     * @param git          repository access
     * @param ref          the side to read ({@link GitSource#WORKTREE}
     *                     or a committish)
     * @param registryRoot repository-relative source root of the topic
     *                     library (the directory holding {@code topics/}),
     *                     or {@code null} when the repository has none
     * @return the index (empty when there is no topic library on that side)
     * @throws IOException on repository access failure
     */
    public static RegistryIndex load(GitSource git, String ref, String registryRoot)
            throws IOException {
        Map<String, Map<String, Map<String, Object>>> domains = new TreeMap<>();
        Map<String, String> files = new LinkedHashMap<>();
        Map<String, List<String>> assemblies = new LinkedHashMap<>();
        if (registryRoot == null) {
            return new RegistryIndex(domains, files, assemblies);
        }
        String prefix = registryRoot + "/";
        for (String path : git.listFiles(ref, registryRoot, ".adoc")) {
            String relative = path.substring(prefix.length());
            TopicHeader header = TopicHeader.parse(relative, git.read(ref, path));
            if (!header.topic()) {
                continue;
            }
            Map<String, Object> entry = TopicRegistry.topicEntry(header, relative);
            // Measured on every edit; the packet already shows content changes.
            entry.remove("char-count");
            domains.computeIfAbsent(TopicRegistry.domainOf(header.id()), k -> new LinkedHashMap<>())
                    .put(header.id(), entry);
            files.put(header.id(), relative);
        }
        for (String path : git.listFiles(ref, "", ".adoc")) {
            if (path.startsWith(prefix)) {
                continue;
            }
            List<String> refs = resolve(includeTargets(git.read(ref, path)), files);
            if (!refs.isEmpty()) {
                List<String> merged = assemblies.computeIfAbsent(assemblyIdOf(path), k -> new ArrayList<>());
                refs.stream().filter(r -> !merged.contains(r)).forEach(merged::add);
            }
        }
        return new RegistryIndex(domains, files, assemblies);
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
     * An assembly's topic membership, in include order.
     *
     * @param assemblyId the assembly id: by IKE naming convention the
     *                   module directory (and artifactId) holding the
     *                   assembly's {@code src/docs/asciidoc}
     * @return the topic ids, or {@code null} when no file of that
     *         assembly includes a topic
     */
    public List<String> assemblyRefs(String assemblyId) {
        return assemblyRefsById.get(assemblyId);
    }

    /**
     * Topic entries grouped by domain, domains sorted, topics in file order.
     *
     * @return the entries keyed by domain, then by topic id
     */
    Map<String, Map<String, Map<String, Object>>> topicsByDomain() {
        return topicsByDomain;
    }

    /**
     * Every assembly's topic membership.
     *
     * @return topic ids keyed by assembly id
     */
    Map<String, List<String>> assemblies() {
        return assemblyRefsById;
    }

    /**
     * Render one assembly's membership delta (topics added to and removed
     * from its includes between two sides) as a small registry-delta
     * partial for an assembly-projection packet.
     *
     * @param oldIndex   the from side
     * @param newIndex   the to side
     * @param fromRef    the from-side ref, for the text
     * @param toRef      the to-side ref (may be {@link GitSource#WORKTREE})
     * @param assemblyId the assembly to report on
     * @return the partial's text with a level-1 title, or an empty
     *         string when this assembly's membership is unchanged
     */
    public static String membershipDelta(RegistryIndex oldIndex, RegistryIndex newIndex,
                                         String fromRef, String toRef, String assemblyId) {
        List<String> oldRefs = oldIndex.assemblyRefsById.getOrDefault(assemblyId, List.of());
        List<String> newRefs = newIndex.assemblyRefsById.getOrDefault(assemblyId, List.of());
        List<String> plus = newRefs.stream().filter(r -> !oldRefs.contains(r)).toList();
        List<String> minus = oldRefs.stream().filter(r -> !newRefs.contains(r)).toList();
        if (plus.isEmpty() && minus.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[[registry-delta]]\n= Membership Delta — ")
                .append(assemblyId).append("\n\nTopics included by this assembly changed between ")
                .append(fromRef).append(" and ")
                .append(GitSource.WORKTREE.equals(toRef) ? "the working tree" : toRef)
                .append(".\n\n");
        plus.forEach(r -> sb.append("* added `").append(r).append("`\n"));
        minus.forEach(r -> sb.append("* removed `").append(r).append("`\n"));
        sb.append('\n');
        return sb.toString();
    }

    /**
     * The targets of a file's top-level {@code include::} directives, in
     * document order, with leading attribute references such as
     * {@code {topicsdir}/} and {@code ./} removed.
     *
     * @param content the file text, possibly {@code null}
     * @return the normalized include targets
     */
    static List<String> includeTargets(String content) {
        List<String> out = new ArrayList<>();
        if (content == null) {
            return out;
        }
        Matcher m = INCLUDE.matcher(content);
        while (m.find()) {
            String target = m.group(1).strip();
            String before;
            do {
                before = target;
                target = LEADING_ATTRIBUTE.matcher(target).replaceFirst("");
                if (target.startsWith("./")) {
                    target = target.substring(2);
                }
            } while (!target.equals(before));
            out.add(target);
        }
        return out;
    }

    /**
     * Map include targets to topic ids by path: a target names a topic
     * when it equals the topic's registry-root-relative file or ends with
     * it (the assembly reaches the unpacked topic library through an
     * attribute such as {@code {topicsdir}}).
     *
     * @param targets        normalized include targets
     * @param topicFilesById topic files keyed by id
     * @return the topic ids included, in order, without repeats
     */
    static List<String> resolve(List<String> targets, Map<String, String> topicFilesById) {
        List<String> out = new ArrayList<>();
        for (String target : targets) {
            for (Map.Entry<String, String> e : topicFilesById.entrySet()) {
                String file = e.getValue();
                if ((target.equals(file) || target.endsWith("/" + file)) && !out.contains(e.getKey())) {
                    out.add(e.getKey());
                    break;
                }
            }
        }
        return out;
    }

    /**
     * The assembly id of a file: the module directory that holds its
     * {@code src/docs/asciidoc}, or the file's base name for a module at
     * the repository root.
     *
     * @param path repository-relative file path
     * @return the assembly id
     */
    static String assemblyIdOf(String path) {
        int at = path.indexOf("/" + SOURCE_ROOT + "/");
        if (at > 0) {
            String module = path.substring(0, at);
            return module.substring(module.lastIndexOf('/') + 1);
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.endsWith(".adoc") ? name.substring(0, name.length() - ".adoc".length()) : name;
    }

    /**
     * Find the topic library's source root on a side: the
     * {@code …/src/docs/asciidoc} directory whose {@code topics/} holds
     * {@code .adoc} files.
     *
     * @param git repository access
     * @param ref the side to search
     * @return the repository-relative source root, or {@code null} when
     *         the repository has no topic library on that side
     * @throws IOException on repository access failure
     */
    public static String findRoot(GitSource git, String ref) throws IOException {
        String marker = SOURCE_ROOT + "/topics/";
        for (String path : git.listFiles(ref, "", ".adoc")) {
            if (path.startsWith(marker)) {
                return SOURCE_ROOT;
            }
            int at = path.indexOf("/" + marker);
            if (at > 0) {
                return path.substring(0, at + 1 + SOURCE_ROOT.length());
            }
        }
        return null;
    }
}
