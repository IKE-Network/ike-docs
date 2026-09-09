package network.ike.docs.plugin.diff;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PacketAssembler#resolveInclude} and
 * {@link PacketAssembler#rewriteIncludes} — the include staging that
 * lets a marked topic's relative includes resolve from {@code _diff/}
 * (ike-issues#1096).
 */
class PacketAssemblerIncludeTest {

    private static final String TOPICS = "topics/src/docs/asciidoc/topics/";
    private static final String TOPIC = TOPICS + "anf/fourth-separation-mim-absence.adoc";

    /** The mojo's naming rule: path below the topics prefix, slashes folded. */
    private static final Function<String, String> FLAT =
            p -> (p.startsWith(TOPICS) ? p.substring(TOPICS.length()) : p).replace('/', '-');

    private static final List<String> OLD = List.of(
            "// anf-fourth-separation-mim-absence",
            ":topic-id: anf-fourth-separation-mim-absence",
            "",
            "[[anf-fourth-separation-mim-absence]]",
            "= Absence",
            "",
            "Clinical absence is a contrary determination.",
            "",
            ".Contradictory vs. Contrary",
            "[plantuml, contradictory-vs-contrary-principle, svg]",
            "....",
            "include::fourth-separation-mim-contrary.puml[]",
            "....",
            "",
            "A representation meeting these requirements is sufficient.");

    @Test
    void unchangedIncludedPuml_survivesMarkingAndIsRepointedAtStagedCopy() {
        List<String> changed = new ArrayList<>(OLD);
        changed.set(6, "Clinical absence is a contrary determination with an unexcluded middle.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(OLD, changed);
        List<String> marked = AdocDiffMarker.withDiagramHistory(OLD, r.lines());

        PacketAssembler.IncludeRewrite rw = PacketAssembler.rewriteIncludes(marked, TOPIC, FLAT);

        // The prose change is marked; the diagram, unchanged, gets no history block.
        assertThat(String.join("\n", rw.lines())).contains("[.diff-ins]##");
        assertThat(rw.lines()).noneMatch(l -> l.contains("This diagram changed"));
        // The include now names the staged copy and nothing else moved.
        assertThat(rw.lines()).contains("include::anf-fourth-separation-mim-contrary.puml[]");
        assertThat(rw.lines()).noneMatch(l -> l.equals("include::fourth-separation-mim-contrary.puml[]"));
        assertThat(rw.targets()).containsExactly(
                Map.entry(TOPICS + "anf/fourth-separation-mim-contrary.puml",
                        "anf-fourth-separation-mim-contrary.puml"));
    }

    @Test
    void changedDiagram_previousRenderingIncludesTheSameStagedCopy() {
        List<String> changed = new ArrayList<>(OLD);
        changed.set(10, "----");
        changed.set(12, "----");
        List<String> old = new ArrayList<>(OLD);
        old.set(10, "----");
        old.set(11, "include::fourth-separation-mim-contrary.puml[lines=1..10]");
        old.set(12, "----");
        List<String> marked = AdocDiffMarker.withDiagramHistory(
                old, AdocDiffMarker.mark(old, changed).lines());
        assertThat(marked).anyMatch(l -> l.contains("This diagram changed"));

        PacketAssembler.IncludeRewrite rw = PacketAssembler.rewriteIncludes(marked, TOPIC, FLAT);

        // Both the current block and the previous-rendering block point at the
        // one staged file; each keeps its own attribute list.
        assertThat(rw.lines()).contains(
                "include::anf-fourth-separation-mim-contrary.puml[]",
                "include::anf-fourth-separation-mim-contrary.puml[lines=1..10]");
        assertThat(rw.targets()).hasSize(1);
    }

    @Test
    void resolveInclude_walksRelativePathsFromTheIncludingFilesDirectory() {
        assertThat(PacketAssembler.resolveInclude(TOPIC, "diagram.puml"))
                .isEqualTo(TOPICS + "anf/diagram.puml");
        assertThat(PacketAssembler.resolveInclude(TOPIC, "./diagram.puml"))
                .isEqualTo(TOPICS + "anf/diagram.puml");
        assertThat(PacketAssembler.resolveInclude(TOPIC, "../shared/diagram.puml"))
                .isEqualTo(TOPICS + "shared/diagram.puml");
        assertThat(PacketAssembler.resolveInclude("a.adoc", "b.adoc")).isEqualTo("b.adoc");
    }

    @Test
    void resolveInclude_leavesUnresolvableTargetsAlone() {
        assertThat(PacketAssembler.resolveInclude(TOPIC, "{topics}/topics/dev/overview.adoc")).isNull();
        assertThat(PacketAssembler.resolveInclude(TOPIC, "/abs/path.adoc")).isNull();
        assertThat(PacketAssembler.resolveInclude(TOPIC, "https://example.org/x.adoc")).isNull();
        assertThat(PacketAssembler.resolveInclude("a.adoc", "../../escape.adoc")).isNull();
    }

    @Test
    void rewriteIncludes_leavesDeclinedAndUnresolvableIncludesAsWritten() {
        List<String> lines = List.of(
                "include::{topics}/topics/dev/overview.adoc[]",
                "include::missing.puml[]",
                "include::present.puml[tag=core]",
                "\\include::escaped.puml[]",
                "prose mentioning include::not-a-directive.puml[] mid-line");
        PacketAssembler.IncludeRewrite rw = PacketAssembler.rewriteIncludes(lines, TOPIC,
                p -> p.endsWith("missing.puml") ? null : FLAT.apply(p));

        assertThat(rw.lines()).containsExactly(
                "include::{topics}/topics/dev/overview.adoc[]",
                "include::missing.puml[]",
                "include::anf-present.puml[tag=core]",
                "\\include::escaped.puml[]",
                "prose mentioning include::not-a-directive.puml[] mid-line");
        assertThat(rw.targets()).containsOnlyKeys(TOPICS + "anf/present.puml");
    }

    @Test
    void rewriteIncludes_ofAMarkedTopic_landsOnItsMarkedCopyName() {
        List<String> lines = List.of("include::../dev/overview.adoc[leveloffset=+1]");
        PacketAssembler.IncludeRewrite rw = PacketAssembler.rewriteIncludes(lines, TOPIC, FLAT);
        // Same flat rule as the marked topic's outName, so the include
        // resolves to the marked copy the packet already stages.
        assertThat(rw.lines()).containsExactly("include::dev-overview.adoc[leveloffset=+1]");
    }
}
