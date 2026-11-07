package network.ike.docs.plugin.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link IncrementalRegistry} against a real repository: the four arms of
 * the blob rule, the registry round-tripping through
 * {@link TopicRegistry#load} as the next run's input, and the term index
 * derived from the entries without reopening a file.
 */
class IncrementalRegistryTest {

    private static final String ROOT = "topics/src/docs/asciidoc";
    private static final Instant T1 = Instant.parse("2026-11-06T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-06T11:00:00Z");

    private static String topic(String id, String title, String body) {
        return """
                :topic-id: %s
                :topic-type: concept
                :topic-status: draft
                :topic-keywords: one, two

                [[%s]]
                = %s

                %s
                """.formatted(id, id, title, body);
    }

    private static TempGit library(Path dir) throws IOException {
        TempGit git = TempGit.init(dir);
        git.write(ROOT + "/network.ike.lab/dev/one.adoc", topic("dev-one", "One",
                "indexterm:[workflow, GitFlow]\nThe ((feature branch)) carries the work."));
        git.write(ROOT + "/network.ike.lab/dev/two.adoc", topic("dev-two", "Two",
                "indexterm:[workflow, release]\nText."));
        git.write(ROOT + "/network.ike.lab/arch/three.adoc", topic("arch-three", "Three", "Text."));
        git.write(ROOT + "/network.ike.lab/lab/index.adoc", "= Preview\n:doctype: book\n\ninclude::../dev/one.adoc[]\n");
        git.write(ROOT + "/network.ike.lab/lab/notes.adoc", "== Notes\n\nText.\n");
        git.commitAll("library");
        return git;
    }

    private static IncrementalRegistry.Outcome run(Map<String, Object> previous, Path dir, Instant now)
            throws IOException {
        Path root = dir.resolve(ROOT);
        BlobIndex index = GitBlobIndex.read(root).orElseThrow();
        return IncrementalRegistry.update(previous, root, index, dir.resolve("topics"), now);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entry(Map<String, Object> model, String file) {
        for (Object d : (List<Object>) model.get("domains")) {
            for (Object t : (List<Object>) ((Map<String, Object>) d).get("topics")) {
                Map<String, Object> topic = (Map<String, Object>) t;
                if (file.equals(topic.get("file"))) {
                    return topic;
                }
            }
        }
        for (String key : List.of("assemblies", "other-files")) {
            for (Object o : (List<Object>) model.get(key)) {
                Map<String, Object> other = (Map<String, Object>) o;
                if (file.equals(other.get("file"))) {
                    return other;
                }
            }
        }
        return null;
    }

    @Test
    void firstRun_parsesEveryFile_andRecordsBlobsRelativeToTheRoot(@TempDir Path dir) throws IOException {
        TempGit git = library(dir);

        IncrementalRegistry.Outcome outcome = run(null, dir, T1);

        assertThat(outcome.parsed()).isEqualTo(5);
        assertThat(outcome.kept()).isZero();
        Map<String, Object> model = outcome.model();
        assertThat(model.keySet()).containsExactly("registry-version", "generated", "scanned-from", "roots",
                "topic-count", "file-count", "domains", "assemblies", "other-files", "findings");
        assertThat(model.get("registry-version")).isEqualTo(IncrementalRegistry.VERSION);
        assertThat(model.get("roots")).isEqualTo(List.of("src/docs/asciidoc"));
        assertThat(model.get("topic-count")).isEqualTo(3);
        assertThat(model.get("file-count")).isEqualTo(5);
        Map<String, Object> one = entry(model, "network.ike.lab/dev/one.adoc");
        assertThat(one.keySet()).startsWith("id", "file", "blob", "title");
        assertThat(one.get("blob")).isEqualTo(git.hashObject(ROOT + "/network.ike.lab/dev/one.adoc"));
        assertThat(one.get("index-terms")).isEqualTo(List.of("workflow > GitFlow", "feature branch"));
        Map<String, Object> index = entry(model, "network.ike.lab/lab/index.adoc");
        assertThat(index.keySet()).startsWith("file", "topic", "blob", "title");
        assertThat(index.get("topic")).isEqualTo(false);
        assertThat(index.get("includes")).isEqualTo(1);
        assertThat(entry(model, "network.ike.lab/lab/notes.adoc").get("blob"))
                .isEqualTo(git.hashObject(ROOT + "/network.ike.lab/lab/notes.adoc"));
        assertThat((List<?>) model.get("findings")).isEmpty();
    }

    @Test
    void secondRun_keepsUnchanged_rekeysMoved_parsesChanged_andDropsGone(@TempDir Path dir)
            throws IOException {
        TempGit git = library(dir);
        Map<String, Object> first = run(null, dir, T1).model();
        Path registryFile = dir.resolve("topics/target/topic-registry.yaml");
        Files.createDirectories(registryFile.getParent());
        Files.writeString(registryFile, TopicRegistry.yaml(first));
        // the sentinel proves a kept entry is never re-derived from the file
        Map<String, Object> previous = TopicRegistry.load(registryFile);
        entry(previous, "network.ike.lab/dev/two.adoc").put("title", "Kept, not reparsed");

        git.run("mv", ROOT + "/network.ike.lab/arch/three.adoc", ROOT + "/network.ike.lab/arch/moved.adoc");
        git.write(ROOT + "/network.ike.lab/dev/one.adoc", topic("dev-one", "One, edited",
                "indexterm:[workflow, GitFlow]\nindexterm:[review]\nText."));
        git.write(ROOT + "/network.ike.lab/dev/new.adoc", topic("dev-new", "New", "Text."));
        Files.delete(dir.resolve(ROOT + "/network.ike.lab/lab/notes.adoc"));

        IncrementalRegistry.Outcome outcome = run(previous, dir, T2);

        assertThat(outcome.kept()).isEqualTo(2);        // two.adoc, index.adoc
        assertThat(outcome.rekeyed()).isEqualTo(1);     // three.adoc -> moved.adoc
        assertThat(outcome.parsed()).isEqualTo(2);      // one.adoc, new.adoc
        assertThat(outcome.dropped()).isEqualTo(1);     // notes.adoc
        Map<String, Object> model = outcome.model();
        assertThat(model.get("generated")).isEqualTo("2026-11-06T11:00:00Z");
        assertThat(model.get("topic-count")).isEqualTo(4);
        assertThat(model.get("file-count")).isEqualTo(5);
        assertThat(entry(model, "network.ike.lab/dev/two.adoc").get("title")).isEqualTo("Kept, not reparsed");
        Map<String, Object> moved = entry(model, "network.ike.lab/arch/moved.adoc");
        assertThat(moved.get("id")).isEqualTo("arch-three");
        assertThat(moved.get("blob")).isEqualTo(entry(first, "network.ike.lab/arch/three.adoc").get("blob"));
        assertThat(entry(model, "network.ike.lab/arch/three.adoc")).isNull();
        Map<String, Object> one = entry(model, "network.ike.lab/dev/one.adoc");
        assertThat(one.get("title")).isEqualTo("One, edited");
        assertThat(one.get("index-terms")).isEqualTo(List.of("workflow > GitFlow", "review"));
        assertThat(entry(model, "network.ike.lab/dev/new.adoc").get("id")).isEqualTo("dev-new");
        assertThat(entry(model, "network.ike.lab/lab/notes.adoc")).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> domains = (List<Map<String, Object>>) model.get("domains");
        assertThat(domains).extracting(d -> d.get("id")).containsExactly("arch", "dev");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dev = (List<Map<String, Object>>) domains.get(1).get("topics");
        assertThat(dev).extracting(t -> t.get("id")).containsExactly("dev-new", "dev-one", "dev-two");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findings_areCarriedForKeptEntries_andDuplicateIdsDerivedAgain(@TempDir Path dir) throws IOException {
        TempGit git = library(dir);
        git.write(ROOT + "/network.ike.lab/dev/broken.adoc", ":topic-id: dev-broken\n:topic-type: concept\n\n[[wrong]]\n= Broken\n");
        git.commitAll("broken");
        Map<String, Object> first = run(null, dir, T1).model();
        assertThat((List<Object>) first.get("findings")).containsExactly(
                "network.ike.lab/dev/broken.adoc: missing required attribute :topic-status:",
                "network.ike.lab/dev/broken.adoc: missing required attribute :topic-keywords:",
                "network.ike.lab/dev/broken.adoc: anchor [[wrong]] does not match :topic-id: dev-broken");

        git.write(ROOT + "/network.ike.lab/dev/copy.adoc", topic("dev-one", "Copy of one", "Text."));
        IncrementalRegistry.Outcome second = run(first, dir, T2);

        assertThat(second.kept()).isEqualTo(6);
        assertThat(second.parsed()).isEqualTo(1);
        assertThat((List<Object>) second.model().get("findings")).containsExactly(
                "network.ike.lab/dev/broken.adoc: missing required attribute :topic-status:",
                "network.ike.lab/dev/broken.adoc: missing required attribute :topic-keywords:",
                "network.ike.lab/dev/broken.adoc: anchor [[wrong]] does not match :topic-id: dev-broken",
                // in path order copy.adoc comes first, so it is one.adoc that duplicates, as the full scan says
                "network.ike.lab/dev/one.adoc: duplicate id 'dev-one', already declared by network.ike.lab/dev/copy.adoc");

        Files.delete(dir.resolve(ROOT + "/network.ike.lab/dev/copy.adoc"));
        IncrementalRegistry.Outcome third = run(second.model(), dir, T2);
        assertThat(third.dropped()).isEqualTo(1);
        assertThat((List<?>) third.model().get("findings")).hasSize(3)
                .noneSatisfy(f -> assertThat(String.valueOf(f)).contains("duplicate id"));
    }

    @Test
    void registry_roundTripsThroughLoad_andIsTheNextRunsInput(@TempDir Path dir) throws IOException {
        library(dir);
        Map<String, Object> model = run(null, dir, T1).model();
        Path file = dir.resolve("topics/target/rt.yaml");
        Files.createDirectories(file.getParent());
        String once = TopicRegistry.yaml(model);
        Files.writeString(file, once);

        Map<String, Object> loaded = TopicRegistry.load(file);
        assertThat(TopicRegistry.yaml(loaded)).isEqualTo(once);

        IncrementalRegistry.Outcome again = run(loaded, dir, T2);
        assertThat(again.kept()).isEqualTo(5);
        assertThat(again.parsed()).isZero();
        assertThat(TopicRegistry.yaml(again.model()).replace("2026-11-06T11:00:00Z", "2026-11-06T10:00:00Z"))
                .isEqualTo(once);
    }

    @Test
    void aRegistryOfTheFullScan_withoutBlobs_isParsedAgain(@TempDir Path dir) throws IOException {
        library(dir);
        Path root = dir.resolve(ROOT);
        Map<String, Object> legacy = TopicRegistry.model(List.of(TopicRegistry.scan(root, 100)), dir.resolve("topics"), T1);

        IncrementalRegistry.Outcome outcome = run(legacy, dir, T2);

        assertThat(outcome.kept()).isZero();
        assertThat(outcome.parsed()).isEqualTo(5);
        assertThat(outcome.dropped()).isZero();
    }

    @Test
    void hashedIndex_givesTheSameRegistryAsGit(@TempDir Path dir) throws IOException {
        library(dir);
        Path root = dir.resolve(ROOT);
        Map<String, Object> fromGit = run(null, dir, T1).model();

        Map<String, Object> hashed = IncrementalRegistry.update(null, root, BlobIndex.hashed(root),
                dir.resolve("topics"), T1).model();

        assertThat(TopicRegistry.yaml(hashed)).isEqualTo(TopicRegistry.yaml(fromGit));
        assertThat(IncrementalRegistry.update(fromGit, root, BlobIndex.hashed(root), dir.resolve("topics"), T2)
                .kept()).isEqualTo(5);
    }

    @Test
    void termIndex_isDerivedFromTheEntries(@TempDir Path dir) throws IOException {
        library(dir);
        Map<String, Object> model = run(null, dir, T1).model();

        Map<String, Object> terms = TermIndex.model(model, T1);
        String yaml = TermIndex.yaml(terms);

        assertThat(terms.get("generated")).isEqualTo("2026-11-06");
        assertThat(terms.get("term-count")).isEqualTo(4);   // workflow, GitFlow, release, feature branch
        assertThat(yaml).isEqualTo("""
                # term-index.yaml, generated by idoc:topic-registry from the index terms. Do not edit.
                generated: '2026-11-06'
                term-count: 4
                terms:
                  feature branch:
                    _topics:
                      - dev-one
                  workflow:
                    _topics: []
                    GitFlow:
                      _topics:
                        - dev-one
                    release:
                      _topics:
                        - dev-two
                """);
    }
}
