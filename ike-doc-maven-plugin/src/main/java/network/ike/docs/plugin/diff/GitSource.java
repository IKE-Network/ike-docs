package network.ike.docs.plugin.diff;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Read-only access to both sides of a doc-diff comparison
 * (ike-issues#649): any two commits, or a commit and the working tree.
 *
 * <p>The to side accepts the pseudo-ref {@link #WORKTREE}, meaning the
 * uncommitted working tree — the default review case. Rename detection
 * is on, so a moved fragment is compared across the move rather than
 * reported as delete-plus-add.
 *
 * <p>All paths are repository-relative with forward slashes, as JGit
 * reports them.
 */
public final class GitSource implements AutoCloseable {

    /** Pseudo-ref naming the uncommitted working tree as a side. */
    public static final String WORKTREE = "WORKTREE";

    private final Repository repo;

    private GitSource(Repository repo) {
        this.repo = repo;
    }

    /**
     * Open the repository that contains the given directory, walking
     * upward to find the git dir (a module's basedir is typically below
     * the repository root).
     *
     * @param within a directory inside the repository
     * @return an open source; close it when done
     * @throws IOException when no repository contains the directory
     */
    public static GitSource open(Path within) throws IOException {
        Repository repo = new FileRepositoryBuilder()
                .findGitDir(within.toFile())
                .readEnvironment()
                .build();
        return new GitSource(repo);
    }

    /**
     * The repository's working-tree root.
     *
     * @return the working-tree root path
     */
    public Path workTree() {
        return repo.getWorkTree().toPath();
    }

    /**
     * One changed file between the two sides.
     *
     * @param status  the change status
     * @param oldPath the from-side path (equal to {@code newPath} except
     *                for renames; {@code null} for additions)
     * @param newPath the to-side path ({@code null} for deletions)
     */
    public record Change(ChangeStatus status, String oldPath, String newPath) {

        /**
         * The path a reader should know this file by — the to side when
         * present, otherwise the from side.
         *
         * @return the display path
         */
        public String displayPath() {
            return newPath != null ? newPath : oldPath;
        }
    }

    /**
     * List the files that differ between the two sides, with rename
     * detection, filtered to a path prefix and suffix.
     *
     * @param fromRef    the from-side ref (any committish)
     * @param toRef      the to-side ref, or {@link #WORKTREE}
     * @param pathPrefix repository-relative prefix filter (may be empty)
     * @param suffix     file-name suffix filter, e.g. {@code .adoc}
     *                   (may be empty)
     * @return the matching changes in path order
     * @throws IOException on repository access failure
     */
    public List<Change> changes(String fromRef, String toRef,
                                String pathPrefix, String suffix) throws IOException {
        try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            df.setRepository(repo);
            df.setDetectRenames(true);
            List<DiffEntry> entries = df.scan(treeIterator(fromRef), treeIterator(toRef));
            List<Change> out = new ArrayList<>();
            for (DiffEntry e : entries) {
                String oldPath = e.getOldPath();
                String newPath = e.getNewPath();
                ChangeStatus status = switch (e.getChangeType()) {
                    case ADD, COPY -> ChangeStatus.ADDED;
                    case DELETE -> ChangeStatus.DELETED;
                    case RENAME -> ChangeStatus.RENAMED;
                    case MODIFY -> ChangeStatus.MODIFIED;
                };
                String probe = status == ChangeStatus.DELETED ? oldPath : newPath;
                if (!probe.startsWith(pathPrefix) || !probe.endsWith(suffix)) {
                    continue;
                }
                out.add(new Change(status,
                        status == ChangeStatus.ADDED ? null : oldPath,
                        status == ChangeStatus.DELETED ? null : newPath));
            }
            return out;
        }
    }

    /**
     * Read one file's text from a side.
     *
     * @param ref  a committish, or {@link #WORKTREE}
     * @param path the repository-relative path
     * @return the file text, or {@code null} when absent on that side
     * @throws IOException on repository access failure
     */
    public String read(String ref, String path) throws IOException {
        if (WORKTREE.equals(ref)) {
            Path f = workTree().resolve(path);
            return Files.exists(f) ? Files.readString(f, StandardCharsets.UTF_8) : null;
        }
        ObjectId commitId = repo.resolve(ref + "^{commit}");
        if (commitId == null) {
            throw new IOException("Cannot resolve ref: " + ref);
        }
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(commitId);
            try (TreeWalk tw = TreeWalk.forPath(repo, path, commit.getTree())) {
                if (tw == null) {
                    return null;
                }
                byte[] bytes = repo.open(tw.getObjectId(0)).getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * List the immediate {@code .yaml} children of a directory on a
     * side — used to enumerate per-domain registry files on both sides
     * of the comparison.
     *
     * @param ref a committish, or {@link #WORKTREE}
     * @param dir the repository-relative directory
     * @return the repository-relative file paths found
     * @throws IOException on repository access failure
     */
    public List<String> listYaml(String ref, String dir) throws IOException {
        List<String> out = new ArrayList<>();
        if (WORKTREE.equals(ref)) {
            Path d = workTree().resolve(dir);
            if (Files.isDirectory(d)) {
                try (Stream<Path> stream = Files.list(d)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".yaml"))
                            .sorted()
                            .forEach(p -> out.add(dir + "/" + p.getFileName()));
                }
            }
            return out;
        }
        ObjectId commitId = repo.resolve(ref + "^{commit}");
        if (commitId == null) {
            throw new IOException("Cannot resolve ref: " + ref);
        }
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(commitId);
            try (TreeWalk tw = TreeWalk.forPath(repo, dir, commit.getTree())) {
                if (tw == null || !tw.isSubtree()) {
                    return out;
                }
                tw.enterSubtree();
                while (tw.next()) {
                    if (tw.getPathString().endsWith(".yaml")) {
                        out.add(tw.getPathString());
                    }
                }
            }
        }
        return out;
    }

    /**
     * Find the first path on a side that ends with the given suffix —
     * used to discover the topic-registry source root from an assembly
     * module (ike-issues#649 subproject scoping).
     *
     * @param ref    a committish, or {@link #WORKTREE}
     * @param suffix the path suffix to match, e.g.
     *               {@code src/docs/asciidoc/topic-registry.yaml}
     * @return the first matching repository-relative path in walk
     *         order, or {@code null} when none matches
     * @throws IOException on repository access failure
     */
    public String findPath(String ref, String suffix) throws IOException {
        if (WORKTREE.equals(ref)) {
            Path root = workTree();
            try (Stream<Path> stream = Files.walk(root)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(p -> root.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                        .filter(p -> p.endsWith(suffix) && !p.contains("/target/")
                                && !p.startsWith(".") && !p.contains("/."))
                        .sorted()
                        .findFirst()
                        .orElse(null);
            }
        }
        ObjectId commitId = repo.resolve(ref + "^{commit}");
        if (commitId == null) {
            throw new IOException("Cannot resolve ref: " + ref);
        }
        try (RevWalk walk = new RevWalk(repo);
             TreeWalk tw = new TreeWalk(repo)) {
            RevCommit commit = walk.parseCommit(commitId);
            tw.addTree(commit.getTree());
            tw.setRecursive(true);
            while (tw.next()) {
                if (tw.getPathString().endsWith(suffix)) {
                    return tw.getPathString();
                }
            }
        }
        return null;
    }

    /**
     * Metadata of one commit in a compared range, for change-entity
     * derivation from trailers (ike-issues#652) and stamp endnotes
     * (ike-issues#656).
     *
     * @param id          the abbreviated commit id
     * @param author      the commit author's name
     * @param date        the author date, ISO local-date form
     * @param subject     the first line of the commit message
     * @param fullMessage the full commit message
     * @param files       repository-relative paths the commit touched
     */
    public record CommitMeta(String id, String author, String date,
                             String subject, String fullMessage, List<String> files) {
    }

    /**
     * The current branch name — the git-era analogue of the STAMP path
     * coordinate.
     *
     * @return the branch name, or the abbreviated detached-head id
     * @throws IOException on repository access failure
     */
    public String branch() throws IOException {
        return repo.getBranch();
    }

    /**
     * The configured user name, used as the author of uncommitted
     * (working-tree) stamps.
     *
     * @return the configured {@code user.name}, or {@code "(local)"}
     */
    public String userName() {
        String name = repo.getConfig().getString("user", null, "name");
        return name == null ? "(local)" : name;
    }

    /**
     * Attribute each line of a file's to-side text to the range commit
     * that introduced it (ike-issues#656). Lines introduced before
     * {@code fromRef} map to {@code null} (unmarked context never asks
     * for a stamp); working-tree lines not yet committed map to
     * {@link #UNCOMMITTED}.
     *
     * @param toRef          the to side ({@link #WORKTREE} or committish)
     * @param path           the repository-relative file path
     * @param rangeCommitIds abbreviated ids of the commits in range
     * @return one attribution per to-side line: an abbreviated commit
     *         id, {@link #UNCOMMITTED}, or {@code null}
     * @throws IOException on repository access failure
     */
    public List<String> blameInRange(String toRef, String path, java.util.Set<String> rangeCommitIds)
            throws IOException {
        String blameRef = WORKTREE.equals(toRef) ? "HEAD" : toRef;
        String blamedText = read(blameRef, path);
        List<String> blamedLineIds = new ArrayList<>();
        if (blamedText != null) {
            try {
                org.eclipse.jgit.blame.BlameResult result = new org.eclipse.jgit.api.Git(repo)
                        .blame()
                        .setFilePath(path)
                        .setStartCommit(repo.resolve(blameRef + "^{commit}"))
                        .call();
                int n = blamedText.split("\n", -1).length;
                for (int i = 0; i < n; i++) {
                    RevCommit c = result != null && i < result.getResultContents().size()
                            ? result.getSourceCommit(i) : null;
                    String id = c == null ? null : c.abbreviate(8).name();
                    blamedLineIds.add(id != null && rangeCommitIds.contains(id) ? id : null);
                }
            } catch (org.eclipse.jgit.api.errors.GitAPIException e) {
                throw new IOException("blame failed for " + path + ": " + e.getMessage(), e);
            }
        }
        if (!WORKTREE.equals(toRef)) {
            return blamedLineIds;
        }
        // Map working-tree lines onto the blamed (HEAD) lines; lines the
        // working tree changed or added are uncommitted.
        String wtText = read(WORKTREE, path);
        if (wtText == null) {
            return List.of();
        }
        List<String> headLines = blamedText == null
                ? List.of() : java.util.Arrays.asList(blamedText.split("\n", -1));
        List<String> wtLines = java.util.Arrays.asList(wtText.split("\n", -1));
        String[] out = new String[wtLines.size()];
        java.util.Arrays.fill(out, UNCOMMITTED);
        int headPos = 0;
        int wtPos = 0;
        for (com.github.difflib.patch.AbstractDelta<String> d
                : com.github.difflib.DiffUtils.diff(headLines, wtLines).getDeltas()) {
            while (wtPos < d.getTarget().getPosition()) {
                if (wtPos < out.length && headPos < blamedLineIds.size()) {
                    out[wtPos] = blamedLineIds.get(headPos);
                }
                wtPos++;
                headPos++;
            }
            headPos += d.getSource().getLines().size();
            wtPos += d.getTarget().getLines().size();
        }
        while (wtPos < out.length && headPos < blamedLineIds.size()) {
            out[wtPos] = blamedLineIds.get(headPos);
            wtPos++;
            headPos++;
        }
        return java.util.Arrays.asList(out);
    }

    /** Attribution value for working-tree lines not yet committed. */
    public static final String UNCOMMITTED = "UNCOMMITTED";

    /**
     * Walk the commits reachable from {@code toRef} and not from
     * {@code fromRef} (i.e. {@code fromRef..toRef}), oldest first, with
     * each commit's touched files.
     *
     * @param fromRef the exclusive lower bound (committish)
     * @param toRef   the inclusive upper bound (committish; not
     *                {@link #WORKTREE})
     * @return commit metadata, oldest first
     * @throws IOException on repository access failure
     */
    public List<CommitMeta> commitsBetween(String fromRef, String toRef) throws IOException {
        List<CommitMeta> out = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repo)) {
            walk.markStart(walk.parseCommit(repo.resolve(toRef + "^{commit}")));
            walk.markUninteresting(walk.parseCommit(repo.resolve(fromRef + "^{commit}")));
            for (RevCommit c : walk) {
                out.add(new CommitMeta(
                        c.abbreviate(8).name(),
                        c.getAuthorIdent().getName(),
                        c.getAuthorIdent().getWhenAsInstant()
                                .atZone(c.getAuthorIdent().getZoneId())
                                .toLocalDate().toString(),
                        c.getShortMessage(),
                        c.getFullMessage(),
                        commitFiles(c)));
            }
        }
        // RevWalk yields newest first; the record of changes reads oldest first.
        List<CommitMeta> reversed = new ArrayList<>(out.size());
        for (int i = out.size() - 1; i >= 0; i--) {
            reversed.add(out.get(i));
        }
        return reversed;
    }

    private List<String> commitFiles(RevCommit commit) throws IOException {
        List<String> files = new ArrayList<>();
        try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
             RevWalk walk = new RevWalk(repo)) {
            df.setRepository(repo);
            AbstractTreeIterator parent;
            if (commit.getParentCount() > 0) {
                RevCommit p = walk.parseCommit(commit.getParent(0).getId());
                parent = treeParser(p.getTree().getId());
            } else {
                parent = new CanonicalTreeParser();
            }
            for (DiffEntry e : df.scan(parent, treeParser(commit.getTree().getId()))) {
                files.add(e.getChangeType() == DiffEntry.ChangeType.DELETE
                        ? e.getOldPath() : e.getNewPath());
            }
        }
        return files;
    }

    private AbstractTreeIterator treeIterator(String ref) throws IOException {
        if (WORKTREE.equals(ref)) {
            return new FileTreeIterator(repo);
        }
        ObjectId commitId = repo.resolve(ref + "^{commit}");
        if (commitId == null) {
            throw new IOException("Cannot resolve ref: " + ref);
        }
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(commitId);
            return treeParser(commit.getTree().getId());
        }
    }

    private CanonicalTreeParser treeParser(ObjectId treeId) throws IOException {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        try (org.eclipse.jgit.lib.ObjectReader reader = repo.newObjectReader()) {
            parser.reset(reader, treeId);
        }
        return parser;
    }

    @Override
    public void close() {
        repo.close();
    }
}
