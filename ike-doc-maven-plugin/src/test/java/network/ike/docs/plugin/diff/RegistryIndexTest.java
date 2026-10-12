package network.ike.docs.plugin.diff;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RegistryIndex} and {@link RegistryDelta}: the registry
 * each side of a doc diff generates from its files, with no hand-kept
 * registry file.
 */
class RegistryIndexTest {

    @TempDir
    Path repo;

    @Test
    void includeTargets_stripsAttributePrefixes() {
        List<String> targets = RegistryIndex.includeTargets("""
                = Guide
                include::{topicsdir}/topics/arch/one.adoc[leveloffset=+1]
                include::./local.adoc[]
                  include::indented.adoc[] is not a directive
                """);
        assertThat(targets).containsExactly("topics/arch/one.adoc", "local.adoc");
    }

    @Test
    void resolve_matchesTopicFilesBySuffix() {
        Map<String, String> files = Map.of("arch-one", "topics/arch/one.adoc",
                "arch-two", "topics/arch/two.adoc");
        assertThat(RegistryIndex.resolve(List.of("topics/arch/two.adoc",
                        "x/topics-asciidoc/topics/arch/one.adoc", "topics/arch/two.adoc",
                        "other.adoc"), files))
                .containsExactly("arch-two", "arch-one");
    }

    @Test
    void assemblyIdOf_usesModuleDirectory() {
        assertThat(RegistryIndex.assemblyIdOf("arch-guide/src/docs/asciidoc/arch-guide.adoc"))
                .isEqualTo("arch-guide");
        assertThat(RegistryIndex.assemblyIdOf("src/docs/asciidoc/compendium.adoc"))
                .isEqualTo("compendium");
    }

    @Test
    void generatesBothSidesFromFilesAndDiffsThem() throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).setInitialBranch("main").call()) {
            write("topics/src/docs/asciidoc/topics/arch/one.adoc",
                    topic("arch-one", "One", "draft", "Old summary."));
            write("guide/src/docs/asciidoc/guide.adoc", """
                    = Guide
                    include::{topicsdir}/topics/arch/one.adoc[leveloffset=+1]
                    """);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("one").setSign(false).call();

            write("topics/src/docs/asciidoc/topics/arch/one.adoc",
                    topic("arch-one", "One", "review", "New summary."));
            write("topics/src/docs/asciidoc/topics/ops/two.adoc",
                    topic("ops-two", "Two", "draft", "Two summary."));
            write("guide/src/docs/asciidoc/guide.adoc", """
                    = Guide
                    include::{topicsdir}/topics/arch/one.adoc[leveloffset=+1]
                    include::{topicsdir}/topics/ops/two.adoc[leveloffset=+1]
                    """);
        }

        try (GitSource source = GitSource.open(repo)) {
            String root = RegistryIndex.findRoot(source, GitSource.WORKTREE);
            assertThat(root).isEqualTo("topics/src/docs/asciidoc");

            RegistryIndex before = RegistryIndex.load(source, "HEAD", root);
            RegistryIndex after = RegistryIndex.load(source, GitSource.WORKTREE, root);
            assertThat(before.topicFile("arch-one")).isEqualTo("topics/arch/one.adoc");
            assertThat(before.assemblyRefs("guide")).containsExactly("arch-one");
            assertThat(after.assemblyRefs("guide")).containsExactly("arch-one", "ops-two");

            String delta = new RegistryDelta("HEAD", GitSource.WORKTREE).render(before, after);
            assertThat(delta)
                    .contains("`arch-one` — ")
                    .contains("status: draft → review")
                    .contains("summary rewritten")
                    .contains(".New entries (1)")
                    .contains("`ops-two` — Two (concept/draft)")
                    .contains("* total topic-count: 1 → 2")
                    .contains("* ops: new domain → 1")
                    .contains("* `guide` — +[ops-two]")
                    .doesNotContain("char-count");

            assertThat(RegistryIndex.membershipDelta(before, after, "HEAD", GitSource.WORKTREE, "guide"))
                    .contains("* added `ops-two`");
            assertThat(new RegistryDelta("HEAD", "HEAD").render(before, before)).isEmpty();
        }
    }

    private void write(String path, String content) throws Exception {
        Path file = repo.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static String topic(String id, String title, String status, String summary) {
        return """
                :topic-id: %s
                :topic-type: concept
                :topic-status: %s
                :topic-keywords: alpha, beta, gamma
                :topic-summary: %s

                [[%s]]
                = %s

                Body.
                """.formatted(id, status, summary, id, title);
    }
}
