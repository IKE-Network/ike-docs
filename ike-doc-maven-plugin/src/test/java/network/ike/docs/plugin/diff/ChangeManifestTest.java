package network.ike.docs.plugin.diff;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChangeManifest} trailer-based derivation
 * (ike-issues#652).
 */
class ChangeManifestTest {

    @Test
    void groupKey_readsRefsTrailer() {
        GitSource.CommitMeta c = new GitSource.CommitMeta("abc12345", "A. Author", "2026-06-01",
                "Add glossary",
                "Add glossary\n\nBody text.\n\nRefs: IKE-Network/ike-issues#648\n",
                List.of("a.adoc"));
        assertThat(ChangeManifest.groupKey(c)).isEqualTo("IKE-Network/ike-issues#648");
    }

    @Test
    void groupKey_readsFixesTrailer() {
        GitSource.CommitMeta c = new GitSource.CommitMeta("abc12345", "A. Author", "2026-06-01",
                "Fix counts",
                "Fix counts\n\nFixes: IKE-Network/ike-issues#635\n",
                List.of("a.yaml"));
        assertThat(ChangeManifest.groupKey(c)).isEqualTo("IKE-Network/ike-issues#635");
    }

    @Test
    void groupKey_withoutTrailer_isPerCommit() {
        GitSource.CommitMeta c = new GitSource.CommitMeta("abc12345", "A. Author", "2026-06-01",
                "Tidy wording", "Tidy wording\n", List.of("a.adoc"));
        assertThat(ChangeManifest.groupKey(c)).isEqualTo("commit:abc12345");
    }

    @Test
    void derive_groupsCommitsByIssueAndUnionsFiles() {
        List<GitSource.CommitMeta> commits = List.of(
                new GitSource.CommitMeta("c1", "A. Author", "2026-06-01", "Add topic",
                        "Add topic\n\nRefs: IKE-Network/ike-issues#648\n",
                        List.of("topics/a.adoc")),
                new GitSource.CommitMeta("c2", "A. Author", "2026-06-02", "Register topic",
                        "Register topic\n\nRefs: IKE-Network/ike-issues#648\n",
                        List.of("topic-registry/arch.yaml", "topics/a.adoc")),
                new GitSource.CommitMeta("c3", "B. Author", "2026-06-03", "Unrelated tidy",
                        "Unrelated tidy\n", List.of("topics/b.adoc")));
        ChangeManifest m = ChangeManifest.derive(commits);

        assertThat(m.changes()).hasSize(2);
        ChangeManifest.ChangeEntity issue = m.changes().get(0);
        assertThat(issue.refs()).containsExactly("IKE-Network/ike-issues#648");
        assertThat(issue.title()).isEqualTo("Add topic");
        assertThat(issue.description()).isEqualTo("Add topic; Register topic");
        assertThat(issue.files())
                .containsExactly("topics/a.adoc", "topic-registry/arch.yaml");

        ChangeManifest.ChangeEntity single = m.changes().get(1);
        assertThat(single.id()).isEqualTo("chg-c3");
        assertThat(single.refs()).isEmpty();
        assertThat(single.files()).containsExactly("topics/b.adoc");
    }
}
