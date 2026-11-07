package network.ike.docs.plugin.registry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The blob-keyed incremental registry ({@code dev-adoc-packages},
 * IKE-Network/ike-issues#1341). Every entry records the blob id of the
 * content it was parsed from, and a run decides each file by one rule
 * against the registry it wrote last time:
 *
 * <ul>
 *   <li>same path and same blob: keep the entry, without opening the file;</li>
 *   <li>same blob at a path that is gone, a rename: re-key the entry to the
 *       new path, without opening the file;</li>
 *   <li>a new or changed blob: parse the file;</li>
 *   <li>a path that is gone: drop the entry.</li>
 * </ul>
 *
 * <p>Files that are not topics get an entry too, with their blob, so they
 * are not opened again either. What is derived across files is derived
 * again on every run from the entries: the duplicate-id findings, the
 * counts, the domain grouping and the order. The result has the shape of
 * {@link TopicRegistry#model}, so it reads back through
 * {@link TopicRegistry#load} and serves as the previous registry of the
 * next run. A first run, or a run after the previous registry was
 * deleted, parses everything.
 *
 * @since 136
 */
public final class IncrementalRegistry {

    /** The {@code registry-version} of a registry whose entries carry blobs. */
    public static final String VERSION = "1.3";

    /**
     * What one update did.
     *
     * @param model   the registry model, in the shape of {@link TopicRegistry#model}
     * @param kept    entries kept as they were: same path, same blob
     * @param rekeyed entries re-keyed to a new path: same blob, path gone
     * @param parsed  files parsed: a new or changed blob
     * @param dropped entries dropped: path gone and blob not seen elsewhere
     */
    public record Outcome(Map<String, Object> model, int kept, int rekeyed, int parsed, int dropped) {
    }

    /** One entry of the previous registry with where it was filed. */
    private record Previous(Map<String, Object> entry, boolean topic, boolean assembly, List<String> findings) {
    }

    /** One entry of the new registry, in path order. */
    private record Current(String file, Map<String, Object> entry, boolean topic, boolean assembly,
                           List<String> findings) {
    }

    private IncrementalRegistry() {
    }

    /**
     * Update a registry against the files a blob index lists.
     *
     * @param previous the registry written last time, from
     *                 {@link TopicRegistry#load}, or {@code null} for a first run
     * @param root     the registry root the index paths are relative to
     * @param index    every {@code .adoc} file under the root with its blob id
     * @param base     the directory the root is shown relative to (the module
     *                 base directory)
     * @param now      the generation instant
     * @return the outcome, with the new model
     * @throws IOException if a file that must be parsed cannot be read
     */
    public static Outcome update(Map<String, Object> previous, Path root, BlobIndex index, Path base,
                                 Instant now) throws IOException {
        Map<String, Previous> byPath = new HashMap<>();
        Map<String, List<String>> pathsByBlob = new HashMap<>();
        if (previous != null) {
            collectPrevious(previous, byPath, pathsByBlob);
        }

        Set<String> consumed = new HashSet<>();
        List<String> toParse = new ArrayList<>();
        Map<String, Current> current = new TreeMap<>();
        int kept = 0;
        int rekeyed = 0;
        for (Map.Entry<String, String> file : index.blobs().entrySet()) {
            String path = file.getKey();
            String blob = file.getValue();
            Previous same = byPath.get(path);
            if (same != null && blob.equals(same.entry().get("blob"))) {
                consumed.add(path);
                current.put(path, new Current(path, copy(same.entry()), same.topic(), same.assembly(),
                        same.findings()));
                kept++;
                continue;
            }
            String movedFrom = null;
            for (String candidate : pathsByBlob.getOrDefault(blob, List.of())) {
                if (!index.blobs().containsKey(candidate) && !consumed.contains(candidate)) {
                    movedFrom = candidate;
                    break;
                }
            }
            if (movedFrom != null) {
                consumed.add(movedFrom);
                Previous moved = byPath.get(movedFrom);
                Map<String, Object> entry = copy(moved.entry());
                entry.put("file", path);
                current.put(path, new Current(path, entry, moved.topic(), moved.assembly(), moved.findings()));
                rekeyed++;
                continue;
            }
            toParse.add(path);
        }
        int dropped = 0;
        for (String path : byPath.keySet()) {
            if (!index.blobs().containsKey(path) && !consumed.contains(path)) {
                dropped++;
            }
        }

        Path real = root.toRealPath();
        List<Current> parsed;
        try {
            parsed = toParse.parallelStream().map(path -> {
                try {
                    return parse(real, path, index.blobs().get(path));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        for (Current c : parsed) {
            current.put(c.file(), c);
        }
        return new Outcome(model(current.values(), index.findings(), real, base, now), kept, rekeyed,
                toParse.size(), dropped);
    }

    /**
     * Parse one file into its entry: the header through {@link TopicHeader},
     * the index terms through {@link TermIndex}, the blob after the path.
     */
    private static Current parse(Path real, String path, String blob) throws IOException {
        String content = Files.readString(real.resolve(path), StandardCharsets.UTF_8);
        TopicHeader header = TopicHeader.parse(path, content);
        Map<String, Object> entry;
        if (header.topic()) {
            entry = withBlob(TopicRegistry.topicEntry(header, path), blob);
            List<String> terms = TermIndex.termsOf(content);
            if (!terms.isEmpty()) {
                entry.put("index-terms", terms);
            }
        } else {
            entry = withBlob(TopicRegistry.otherEntry(header, path), blob);
        }
        return new Current(path, entry, header.topic(), header.includes() > 0, header.findings());
    }

    /** The entry with {@code blob} right after {@code file}, and {@code topic: false} on a file that is not one. */
    private static Map<String, Object> withBlob(Map<String, Object> entry, String blob) {
        Map<String, Object> out = new LinkedHashMap<>();
        boolean topic = entry.containsKey("id");
        for (Map.Entry<String, Object> e : entry.entrySet()) {
            out.put(e.getKey(), e.getValue());
            if (e.getKey().equals("file")) {
                if (!topic) {
                    out.put("topic", false);
                }
                out.put("blob", blob);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void collectPrevious(Map<String, Object> previous, Map<String, Previous> byPath,
                                        Map<String, List<String>> pathsByBlob) {
        Map<String, List<String>> findingsByFile = new HashMap<>();
        for (Object f : (List<Object>) previous.getOrDefault("findings", List.of())) {
            String finding = String.valueOf(f);
            int colon = finding.indexOf(": ");
            if (colon < 0 || isDerived(finding)) {
                continue;
            }
            findingsByFile.computeIfAbsent(finding.substring(0, colon), k -> new ArrayList<>())
                    .add(finding.substring(colon + 2));
        }
        for (Object d : (List<Object>) previous.getOrDefault("domains", List.of())) {
            for (Object t : (List<Object>) ((Map<String, Object>) d).getOrDefault("topics", List.of())) {
                remember((Map<String, Object>) t, true, false, byPath, pathsByBlob, findingsByFile);
            }
        }
        for (Object a : (List<Object>) previous.getOrDefault("assemblies", List.of())) {
            remember((Map<String, Object>) a, false, true, byPath, pathsByBlob, findingsByFile);
        }
        for (Object o : (List<Object>) previous.getOrDefault("other-files", List.of())) {
            remember((Map<String, Object>) o, false, false, byPath, pathsByBlob, findingsByFile);
        }
    }

    private static void remember(Map<String, Object> entry, boolean topic, boolean assembly,
                                 Map<String, Previous> byPath, Map<String, List<String>> pathsByBlob,
                                 Map<String, List<String>> findingsByFile) {
        String file = String.valueOf(entry.get("file"));
        Object blob = entry.get("blob");
        if (blob == null || byPath.containsKey(file)) {
            return;   // an entry without a blob, from a registry of the full scan, is parsed again
        }
        byPath.put(file, new Previous(entry, topic, assembly,
                List.copyOf(findingsByFile.getOrDefault(file, List.of()))));
        pathsByBlob.computeIfAbsent(String.valueOf(blob), k -> new ArrayList<>()).add(file);
    }

    /** A finding derived across files or from the tree, computed afresh on every run rather than carried. */
    private static boolean isDerived(String finding) {
        return finding.contains(": duplicate id '") || finding.endsWith(": symbolic link, skipped");
    }

    private static Map<String, Object> copy(Map<String, Object> entry) {
        return new LinkedHashMap<>(entry);
    }

    /**
     * Assemble the model from the entries in path order, as
     * {@link TopicRegistry#model} assembles it from headers: topics grouped
     * by domain and sorted by id (a duplicate id kept in path order), the
     * other files split into assemblies and plain files, the tree findings
     * first and then each file's, with the duplicate-id findings derived
     * again.
     */
    private static Map<String, Object> model(Iterable<Current> entries, List<String> treeFindings, Path real,
                                             Path base, Instant now) {
        Map<String, Object> registry = new LinkedHashMap<>();
        registry.put("registry-version", VERSION);
        registry.put("generated", now.truncatedTo(ChronoUnit.SECONDS).toString());
        registry.put("scanned-from", base.toAbsolutePath().normalize().toString());
        registry.put("roots", List.of(relativeTo(base, real)));
        TreeMap<String, List<Map<String, Object>>> domains = new TreeMap<>();
        List<Object> assemblies = new ArrayList<>();
        List<Object> otherFiles = new ArrayList<>();
        List<String> findings = new ArrayList<>(treeFindings);
        findings.sort(null);
        Map<String, String> firstFileById = new HashMap<>();
        int files = 0;
        int topics = 0;
        for (Current c : entries) {
            files++;
            if (c.topic()) {
                topics++;
                String id = String.valueOf(c.entry().get("id"));
                domains.computeIfAbsent(TopicRegistry.domainOf(id), k -> new ArrayList<>()).add(c.entry());
                String earlier = firstFileById.putIfAbsent(id, c.file());
                if (earlier != null) {
                    findings.add(c.file() + ": duplicate id '" + id + "', already declared by " + earlier);
                }
            } else {
                (c.assembly() ? assemblies : otherFiles).add(c.entry());
            }
            for (String f : c.findings()) {
                findings.add(c.file() + ": " + f);
            }
        }
        registry.put("topic-count", topics);
        registry.put("file-count", files);
        List<Object> domainList = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : domains.entrySet()) {
            Map<String, Object> domain = new LinkedHashMap<>();
            domain.put("id", e.getKey());
            e.getValue().sort((x, y) -> String.valueOf(x.get("id")).compareTo(String.valueOf(y.get("id"))));
            domain.put("topics", e.getValue());
            domainList.add(domain);
        }
        registry.put("domains", domainList);
        registry.put("assemblies", assemblies);
        registry.put("other-files", otherFiles);
        registry.put("findings", findings);
        return registry;
    }

    private static String relativeTo(Path base, Path path) {
        try {
            Path b = base.toRealPath();
            if (path.startsWith(b)) {
                String relative = b.relativize(path).toString().replace('\\', '/');
                return relative.isEmpty() ? "." : relative;
            }
        } catch (IOException e) {
            // fall through to the absolute form
        }
        return path.toString();
    }
}
