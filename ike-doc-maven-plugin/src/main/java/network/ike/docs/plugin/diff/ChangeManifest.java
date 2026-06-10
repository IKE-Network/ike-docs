package network.ike.docs.plugin.diff;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The change-entity manifest behind a review packet (ike-issues#648).
 *
 * <p>A change is a named, first-class entity: id, title, one-line
 * description, issue refs, and the files it touches. For working-tree
 * review the manifest is authored as {@code changes.yaml}; for
 * commit-to-commit comparisons it can be derived by grouping the
 * range's commits on their {@code Refs:}/{@code Fixes:} trailers
 * (ike-issues#652) — an authored file, when present, always wins.
 *
 * <p>Expected YAML shape:
 *
 * <pre>{@code
 * changes:
 *   - id: chg-asg-sweep
 *     title: "ASG, not AST"
 *     description: >
 *       One reviewable sentence or two.
 *     refs: [IKE-Network/ike-issues#648]
 *     files:
 *       - topics/src/docs/asciidoc/topics/arch/asg-substrate.adoc
 * }</pre>
 */
public final class ChangeManifest {

    private static final Pattern TRAILER = Pattern.compile(
            "^(?:Refs|Fixes):\\s*(\\S+#\\d+)\\s*$", Pattern.MULTILINE);

    private final List<ChangeEntity> changes;

    private ChangeManifest(List<ChangeEntity> changes) {
        this.changes = List.copyOf(changes);
    }

    /**
     * One named change.
     *
     * @param id          stable kebab-case identifier
     * @param title       short human title (used as glossary term and
     *                    change-index entry)
     * @param description one-or-two-sentence reviewable description
     * @param refs        issue references, e.g.
     *                    {@code IKE-Network/ike-issues#648}
     * @param files       repository-relative files the change touches
     */
    public record ChangeEntity(String id, String title, String description,
                               List<String> refs, List<String> files) {
    }

    /**
     * The manifest's change entities, in authored or derived order.
     *
     * @return the change entities
     */
    public List<ChangeEntity> changes() {
        return changes;
    }

    /**
     * Build a manifest from already-constructed entities — used to merge
     * trailer-derived entities for a committed range with a synthetic
     * uncommitted entity when the to side is the working tree.
     *
     * @param changes the entities, in presentation order
     * @return the manifest
     */
    public static ChangeManifest of(List<ChangeEntity> changes) {
        return new ChangeManifest(changes);
    }

    /**
     * Load an authored manifest.
     *
     * @param yamlFile the {@code changes.yaml} path
     * @return the manifest
     * @throws IOException when the file cannot be read or parsed
     */
    public static ChangeManifest load(Path yamlFile) throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object root = yaml.load(Files.readString(yamlFile));
        List<ChangeEntity> out = new ArrayList<>();
        if (root instanceof Map<?, ?> map && map.get("changes") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    out.add(new ChangeEntity(
                            str(m.get("id"), "change"),
                            str(m.get("title"), "(untitled change)"),
                            String.join(" ", str(m.get("description"), "").split("\\s+")).strip(),
                            strList(m.get("refs")),
                            strList(m.get("files"))));
                }
            }
        }
        return new ChangeManifest(out);
    }

    /**
     * Derive a manifest from a commit range by grouping commits on
     * their first {@code Refs:}/{@code Fixes:} trailer. Commits without
     * a trailer become singleton changes named by their subject.
     *
     * @param commits the range's commits, oldest first
     * @return the derived manifest
     */
    public static ChangeManifest derive(List<GitSource.CommitMeta> commits) {
        Map<String, List<GitSource.CommitMeta>> groups = new LinkedHashMap<>();
        for (GitSource.CommitMeta c : commits) {
            groups.computeIfAbsent(groupKey(c), k -> new ArrayList<>()).add(c);
        }
        List<ChangeEntity> out = new ArrayList<>();
        for (Map.Entry<String, List<GitSource.CommitMeta>> e : groups.entrySet()) {
            List<GitSource.CommitMeta> group = e.getValue();
            boolean byIssue = !e.getKey().startsWith("commit:");
            List<String> subjects = group.stream().map(GitSource.CommitMeta::subject).toList();
            List<String> files = group.stream()
                    .flatMap(c -> c.files().stream())
                    .distinct()
                    .toList();
            String id = byIssue
                    ? "chg-" + e.getKey().replaceAll("[^A-Za-z0-9]+", "-").toLowerCase()
                    : "chg-" + group.get(0).id();
            out.add(new ChangeEntity(
                    id,
                    subjects.get(0),
                    byIssue && subjects.size() > 1
                            ? String.join("; ", subjects)
                            : subjects.get(0),
                    byIssue ? List.of(e.getKey()) : List.of(),
                    files));
        }
        return new ChangeManifest(out);
    }

    /**
     * Extract the grouping key for one commit: its first
     * {@code Refs:}/{@code Fixes:} trailer target, or a per-commit key
     * when no trailer is present.
     *
     * @param commit the commit metadata
     * @return the grouping key
     */
    static String groupKey(GitSource.CommitMeta commit) {
        Matcher m = TRAILER.matcher(commit.fullMessage());
        return m.find() ? m.group(1) : "commit:" + commit.id();
    }

    private static String str(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    private static List<String> strList(Object o) {
        if (o instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
