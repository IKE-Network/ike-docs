package network.ike.docs.plugin.registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Read a {@link BlobIndex} from git without opening a file
 * ({@code dev-adoc-packages}, IKE-Network/ike-issues#1341). One call,
 * {@code git ls-files -s}, lists every tracked path with the blob id of
 * its content as the index holds it. The working tree may differ from the
 * index, so {@code git status} is asked for what moved since: every path
 * it names that still exists is hashed on the spot with
 * {@code git hash-object}, which costs the same as the parse it causes,
 * and every path it names that is gone is dropped. Untracked and ignored
 * files are included and hashed too, so the index covers exactly the files
 * the full scan would read. Renames are not asked for; a rename is a drop
 * and an add here, and the registry re-keys it by blob.
 *
 * <p>Nothing here needs the tree to be committed, or clean. A tree that is
 * not inside a git repository, or a machine without {@code git} on the
 * path, gives {@link Optional#empty()}, and the caller falls back to
 * {@link BlobIndex#hashed(Path)}.
 *
 * @since 136
 */
public final class GitBlobIndex {

    /** The most milliseconds one git call may take before it is given up on. */
    private static final long TIMEOUT_MILLIS = 60_000;

    private GitBlobIndex() {
    }

    /**
     * Index the {@code .adoc} files under a root from git.
     *
     * @param root the registry root, inside a git working tree
     * @return the index, or empty when the root is not in a git working tree
     *         or git cannot be run
     * @throws IOException if git runs but fails, or its output cannot be read
     */
    public static Optional<BlobIndex> read(Path root) throws IOException {
        Path real = root.toRealPath();
        Result prefix;
        try {
            prefix = run(real, null, "git", "rev-parse", "--show-prefix");
        } catch (IOException e) {
            // git is not installed or not on the path
            return Optional.empty();
        }
        if (prefix.exit() != 0) {
            return Optional.empty();
        }
        String rootPrefix = prefix.text().strip();

        Map<String, String> blobs = new TreeMap<>();
        List<String> findings = new ArrayList<>();
        Result listed = run(real, null, "git", "ls-files", "-s", "-z", "--", ".");
        if (listed.exit() != 0) {
            throw new IOException("git ls-files failed in " + real + ": " + listed.text().strip());
        }
        List<String> stale = new ArrayList<>();
        for (String record : listed.text().split("\0")) {
            if (record.isEmpty()) {
                continue;
            }
            // "<mode> <blob> <stage>\t<path>"
            int tab = record.indexOf('\t');
            if (tab < 0) {
                continue;
            }
            String[] head = record.substring(0, tab).split(" ");
            String path = record.substring(tab + 1);
            if (!path.endsWith(".adoc") || BlobIndex.skipped(path)) {
                continue;
            }
            if (head[0].equals("120000")) {
                findings.add(path + ": symbolic link, skipped");
            } else if (head[0].equals("160000")) {
                continue;
            } else if (!head[2].equals("0")) {
                stale.add(path);
            } else {
                blobs.put(path, head[1]);
            }
        }

        Result status = run(real, null, "git", "status", "--porcelain=v1", "-z",
                "--untracked-files=all", "--ignored=traditional", "--no-renames", "--", ".");
        if (status.exit() != 0) {
            throw new IOException("git status failed in " + real + ": " + status.text().strip());
        }
        for (String record : status.text().split("\0")) {
            if (record.length() < 4) {
                continue;
            }
            // "XY <path>", the path relative to the repository root
            String full = record.substring(3);
            if (!full.startsWith(rootPrefix)) {
                continue;
            }
            String path = full.substring(rootPrefix.length());
            if (!path.endsWith(".adoc") || BlobIndex.skipped(path)) {
                continue;
            }
            blobs.remove(path);
            findings.remove(path + ": symbolic link, skipped");
            Path file = real.resolve(path);
            if (Files.isSymbolicLink(file)) {
                findings.add(path + ": symbolic link, skipped");
            } else if (Files.isRegularFile(file)) {
                stale.add(path);
            }
        }
        stale.removeIf(p -> !Files.isRegularFile(real.resolve(p)) || Files.isSymbolicLink(real.resolve(p)));
        if (!stale.isEmpty()) {
            // hash-object reads stdin paths relative to the repository top, not the current directory
            StringBuilder in = new StringBuilder();
            for (String path : stale) {
                in.append(rootPrefix).append(path).append('\n');
            }
            Result hashed = run(real, in.toString(), "git", "hash-object", "--stdin-paths");
            if (hashed.exit() != 0) {
                throw new IOException("git hash-object failed in " + real + ": " + hashed.text().strip());
            }
            String[] ids = hashed.text().strip().split("\n");
            if (ids.length != stale.size()) {
                throw new IOException("git hash-object returned " + ids.length + " ids for "
                        + stale.size() + " paths in " + real);
            }
            for (int i = 0; i < ids.length; i++) {
                blobs.put(stale.get(i), ids[i].strip());
            }
        }
        findings.sort(null);
        return Optional.of(new BlobIndex(Collections.unmodifiableMap(blobs), List.copyOf(findings),
                BlobIndex.Source.GIT));
    }

    /** One git call's exit code and standard output (standard error merged in on failure). */
    private record Result(int exit, String text) {
    }

    private static Result run(Path cwd, String stdin, String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(cwd.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try {
            try (OutputStream in = process.getOutputStream()) {
                if (stdin != null) {
                    in.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream from = process.getInputStream()) {
                from.transferTo(out);
            }
            if (!process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException(String.join(" ", command) + " did not finish in "
                        + TIMEOUT_MILLIS / 1000 + " s in " + cwd);
            }
            return new Result(process.exitValue(), out.toString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException(String.join(" ", command) + " was interrupted", e);
        }
    }
}
