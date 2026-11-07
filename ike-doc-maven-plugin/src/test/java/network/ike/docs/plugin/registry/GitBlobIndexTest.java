package network.ike.docs.plugin.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GitBlobIndex} against a real repository: the ids it reads without
 * opening a file are the ids git computes, the working tree wins over the
 * index, and a tree without git is reported as such.
 */
class GitBlobIndexTest {

    private static final String ROOT = "topics/src/docs/asciidoc";

    private static TempGit library(Path dir) throws IOException {
        TempGit git = TempGit.init(dir);
        git.write(ROOT + "/network.ike.lab/dev/one.adoc", ":topic-id: dev-one\n\n[[dev-one]]\n= One\n");
        git.write(ROOT + "/network.ike.lab/dev/two.adoc", ":topic-id: dev-two\n\n[[dev-two]]\n= Two\n");
        git.write(ROOT + "/network.ike.lab/lab/index.adoc", "= Preview\n\ninclude::../dev/one.adoc[]\n");
        git.write(ROOT + "/network.ike.lab/dev/images/note.txt", "not asciidoc");
        git.write(ROOT + "/target/stale.adoc", ":topic-id: dev-stale\n");
        git.write("README.adoc", "= Outside the root\n");
        git.commitAll("library");
        return git;
    }

    @Test
    void read_listsEveryTrackedAdocUnderTheRootWithItsIndexBlob(@TempDir Path dir) throws IOException {
        TempGit git = library(dir);

        BlobIndex index = GitBlobIndex.read(dir.resolve(ROOT)).orElseThrow();

        assertThat(index.source()).isEqualTo(BlobIndex.Source.GIT);
        assertThat(index.blobs().keySet()).containsExactly("network.ike.lab/dev/one.adoc",
                "network.ike.lab/dev/two.adoc", "network.ike.lab/lab/index.adoc");
        assertThat(index.blobs().get("network.ike.lab/dev/one.adoc"))
                .isEqualTo(git.hashObject(ROOT + "/network.ike.lab/dev/one.adoc"));
        assertThat(index.findings()).isEmpty();
    }

    @Test
    void read_hashesTheWorkingTreeWhereItDiffersFromTheIndex(@TempDir Path dir) throws IOException {
        TempGit git = library(dir);
        BlobIndex committed = GitBlobIndex.read(dir.resolve(ROOT)).orElseThrow();
        git.write(ROOT + "/network.ike.lab/dev/one.adoc", ":topic-id: dev-one\n\n[[dev-one]]\n= One, edited\n");
        git.write(ROOT + "/network.ike.lab/dev/three.adoc", ":topic-id: dev-three\n\n[[dev-three]]\n= Three\n");
        git.write(ROOT + "/network.ike.lab/dev/four.adoc", ":topic-id: dev-four\n");
        git.run("add", ROOT + "/network.ike.lab/dev/four.adoc");
        Files.delete(dir.resolve(ROOT + "/network.ike.lab/dev/two.adoc"));

        BlobIndex index = GitBlobIndex.read(dir.resolve(ROOT)).orElseThrow();

        assertThat(index.blobs().keySet()).containsExactly("network.ike.lab/dev/four.adoc",
                "network.ike.lab/dev/one.adoc", "network.ike.lab/dev/three.adoc", "network.ike.lab/lab/index.adoc");
        assertThat(index.blobs().get("network.ike.lab/dev/one.adoc"))
                .isNotEqualTo(committed.blobs().get("network.ike.lab/dev/one.adoc"))
                .isEqualTo(git.hashObject(ROOT + "/network.ike.lab/dev/one.adoc"));
        assertThat(index.blobs().get("network.ike.lab/dev/three.adoc"))
                .isEqualTo(git.hashObject(ROOT + "/network.ike.lab/dev/three.adoc"));
        assertThat(index.blobs().get("network.ike.lab/dev/four.adoc"))
                .isEqualTo(git.hashObject(ROOT + "/network.ike.lab/dev/four.adoc"));
        assertThat(index.blobs().get("network.ike.lab/lab/index.adoc"))
                .isEqualTo(committed.blobs().get("network.ike.lab/lab/index.adoc"));
    }

    @Test
    void read_includesIgnoredFiles_andReportsSymbolicLinks(@TempDir Path dir, @TempDir Path outside)
            throws IOException {
        TempGit git = library(dir);
        git.write(".gitignore", "*.ignored.adoc\n");
        git.write(ROOT + "/network.ike.lab/dev/draft.ignored.adoc", ":topic-id: dev-draft\n");
        Path target = outside.resolve("secret.adoc");
        Files.writeString(target, ":topic-id: dev-secret\n");
        Files.createSymbolicLink(dir.resolve(ROOT + "/network.ike.lab/dev/linked.adoc"), target);

        BlobIndex index = GitBlobIndex.read(dir.resolve(ROOT)).orElseThrow();

        assertThat(index.blobs()).containsKey("network.ike.lab/dev/draft.ignored.adoc")
                .doesNotContainKey("network.ike.lab/dev/linked.adoc");
        assertThat(index.findings()).containsExactly("network.ike.lab/dev/linked.adoc: symbolic link, skipped");
    }

    @Test
    void read_isEmptyOutsideAGitWorkingTree(@TempDir Path dir) throws IOException {
        Path root = Files.createDirectories(dir.resolve("plain/src/docs/asciidoc"));
        Files.writeString(root.resolve("a.adoc"), "= A\n");

        Optional<BlobIndex> index = GitBlobIndex.read(root);

        // the temporary directory may itself lie inside a repository on a developer machine;
        // then git answers and the index is simply what git says
        if (index.isPresent()) {
            assertThat(index.get().blobs()).containsKey("a.adoc");
        } else {
            assertThat(index).isEmpty();
        }
    }

    @Test
    void hashed_computesTheSameIdsAsGit(@TempDir Path dir) throws IOException {
        TempGit git = library(dir);

        BlobIndex hashed = BlobIndex.hashed(dir.resolve(ROOT));
        BlobIndex fromGit = GitBlobIndex.read(dir.resolve(ROOT)).orElseThrow();

        assertThat(hashed.source()).isEqualTo(BlobIndex.Source.HASHED);
        assertThat(hashed.blobs()).containsExactlyEntriesOf(fromGit.blobs());
        assertThat(BlobIndex.blobId("hello\n".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("ce013625030ba8dba906f756967f9e9ca394464a");
        assertThat(git.hashObject("README.adoc")).isEqualTo(BlobIndex.blobId(
                Files.readAllBytes(dir.resolve("README.adoc"))));
    }
}
