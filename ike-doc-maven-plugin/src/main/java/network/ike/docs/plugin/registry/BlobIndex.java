package network.ike.docs.plugin.registry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Every {@code .adoc} file under a registry root with the git blob id of its
 * content: the key the incremental registry decides staleness by
 * ({@code dev-adoc-packages}, IKE-Network/ike-issues#1341). Dates are the
 * wrong key, since a checkout, a rebase, a sync between machines or a
 * restored file all change dates without changing content; the blob id
 * addresses the content itself, and git already keeps it for every tracked
 * file. {@link GitBlobIndex} reads it from git without opening a file;
 * {@link #hashed(Path)} is the fallback for a tree without git, which
 * reads every file once to compute the same id, so the two never disagree
 * about an unchanged file.
 *
 * <p>Paths are relative to the root with {@code /} separators, in path
 * order, with the directories {@link TopicRegistry#SKIPPED_DIRS} left out,
 * as the full scan leaves them out. A symbolic link is never followed; it
 * is a finding instead, as it is in the full scan.
 *
 * @param blobs    the files, path to blob id, iterating in path order
 * @param findings what the index could not take: a symbolic link, prefixed
 *                 with the path it concerns
 * @param source   where the ids came from
 * @since 136
 */
public record BlobIndex(Map<String, String> blobs, List<String> findings, Source source) {

    /** Where a {@link BlobIndex} took its blob ids from. */
    public enum Source {
        /** From {@code git ls-files}, {@code git status} and {@code git hash-object}. */
        GIT,
        /** Computed in Java by reading every file: the tree has no git. */
        HASHED
    }

    /**
     * Index a root by reading every {@code .adoc} file under it and hashing
     * the content as git hashes a blob ({@code "blob " + length + NUL +
     * content}, SHA-1). This is the full scan's cost, paid when git is not
     * there to answer for free.
     *
     * @param root the registry root
     * @return the index
     * @throws IOException if the tree cannot be read
     */
    public static BlobIndex hashed(Path root) throws IOException {
        Path real = root.toRealPath();
        Map<String, String> blobs = new TreeMap<>();
        List<String> findings = new ArrayList<>();
        Files.walkFileTree(real, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(real) && TopicRegistry.SKIPPED_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!file.getFileName().toString().endsWith(".adoc")) {
                    return FileVisitResult.CONTINUE;
                }
                String relative = real.relativize(file).toString().replace('\\', '/');
                if (attrs.isSymbolicLink()) {
                    findings.add(relative + ": symbolic link, skipped");
                } else if (attrs.isRegularFile()) {
                    try {
                        blobs.put(relative, blobId(Files.readAllBytes(file)));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        findings.sort(null);
        return new BlobIndex(Collections.unmodifiableMap(blobs), List.copyOf(findings), Source.HASHED);
    }

    /**
     * The git blob id of content: the SHA-1 of {@code "blob <length>\0"}
     * followed by the bytes, as {@code git hash-object} computes it for a
     * file with no clean filter.
     *
     * @param content the file content
     * @return the forty-character lowercase hex id
     */
    public static String blobId(byte[] content) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(("blob " + content.length + "\0").getBytes(StandardCharsets.US_ASCII));
            byte[] digest = sha1.digest(content);
            StringBuilder hex = new StringBuilder(40);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    /**
     * Whether a relative path lies under a directory the scan never enters.
     *
     * @param relative a path relative to the root with {@code /} separators
     * @return true when some directory of the path is in {@link TopicRegistry#SKIPPED_DIRS}
     */
    static boolean skipped(String relative) {
        int from = 0;
        int slash;
        while ((slash = relative.indexOf('/', from)) >= 0) {
            if (TopicRegistry.SKIPPED_DIRS.contains(relative.substring(from, slash))) {
                return true;
            }
            from = slash + 1;
        }
        return false;
    }
}
