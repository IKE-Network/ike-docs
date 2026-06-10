package network.ike.docs.plugin.diff;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AdocDiffMarker}.
 *
 * <p>Pure-function tests over the marking rules, including the two
 * corruption traps found during the validating spike: attribute-value
 * continuation lines and table interiors.
 */
class AdocDiffMarkerTest {

    private static final List<String> BASE = List.of(
            "// arch-sample",
            ":topic-id: arch-sample",
            ":topic-keywords: one, two",
            "",
            "[[arch-sample]]",
            "= Sample Topic",
            "",
            "A property graph",
            "with tree-shaped content",
            "(directed acyclic graphs, abstract syntax trees)",
            "in its nodes.");

    @Test
    void wordMerge_marksChangedWordsAndKeepsContext() {
        List<String> changed = List.of(
                "// arch-sample",
                ":topic-id: arch-sample",
                ":topic-keywords: one, two",
                "",
                "[[arch-sample]]",
                "= Sample Topic",
                "",
                "A property graph",
                "with tree-shaped content",
                "(directed acyclic graphs, expression trees)",
                "in its nodes.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(BASE, changed);
        String marked = String.join("\n", r.lines());
        // Both removed words sit inside diff-del roles (grouping may vary
        // with the diff algorithm's alignment), the inserted word inside
        // diff-ins, and the unchanged context stays unmarked.
        assertThat(marked).containsPattern("\\[\\.diff-del\\]##.*abstract.*##");
        assertThat(marked).containsPattern("\\[\\.diff-del\\]##.*syntax.*##");
        assertThat(marked).contains("[.diff-ins]##expression##");
        assertThat(marked).contains("(directed acyclic graphs,");
        assertThat(r.delWords()).isGreaterThan(0);
        assertThat(r.insWords()).isGreaterThan(0);
    }

    @Test
    void headerRegion_isNeverMarkedInline() {
        List<String> changed = List.of(
                "// arch-sample",
                ":topic-id: arch-sample",
                ":topic-keywords: one, two, three",
                "",
                "[[arch-sample]]",
                "= Sample Topic",
                "",
                "A property graph",
                "with tree-shaped content",
                "(directed acyclic graphs, abstract syntax trees)",
                "in its nodes.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(BASE, changed);
        assertThat(r.lines()).contains(":topic-keywords: one, two, three");
        assertThat(String.join("\n", r.lines()))
                .doesNotContain("[.diff-ins]##:topic-keywords:");
        assertThat(r.notes()).anySatisfy(n -> assertThat(n).contains("topic-keywords"));
    }

    @Test
    void continuationLines_areProtected() {
        List<String> oldLines = List.of(
                "[[t]]", "= T", "", "Body text here.");
        List<String> newLines = List.of(
                ":topic-scope-note: Covers the mechanics -- pattern and \\",
                "  semantic primitives, storage engine.",
                "[[t]]", "= T", "", "Body text here.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(oldLines, newLines);
        assertThat(r.lines()).contains("  semantic primitives, storage engine.");
        assertThat(String.join("\n", r.lines()))
                .doesNotContain("[.diff-ins]##  semantic primitives");
    }

    @Test
    void tableInteriors_areProtected() {
        List<String> oldLines = List.of(
                "[[t]]", "= T", "",
                "|===", "| Layer | Role", "| 1 | old role text", "|===");
        List<String> newLines = List.of(
                "[[t]]", "= T", "",
                "|===", "| Layer | Role", "| 1 | new role text", "|===");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(oldLines, newLines);
        String marked = String.join("\n", r.lines());
        assertThat(marked).doesNotContain("diff-ins]#| 1");
        assertThat(marked).contains("| 1 | new role text");
        assertThat(r.notes()).isNotEmpty();
    }

    @Test
    void insertedHeading_keepsMarkerOutsideRole() {
        List<String> oldLines = List.of("[[t]]", "= T", "", "Para.");
        List<String> newLines = List.of("[[t]]", "= T", "", "Para.", "", "== Preview", "", "New text.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(oldLines, newLines);
        assertThat(r.lines()).contains("== [.diff-ins]##Preview##");
    }

    @Test
    void riskyInlineMacros_fallBackToNotes() {
        List<String> oldLines = List.of("[[t]]", "= T", "", "See xref:a-b[] for detail.");
        List<String> newLines = List.of("[[t]]", "= T", "", "See xref:a-c[] for detail.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(oldLines, newLines);
        assertThat(String.join("\n", r.lines())).doesNotContain("diff-ins]#See xref");
        assertThat(r.notes()).anySatisfy(n -> assertThat(n).contains("xref:a-b"));
    }

    @Test
    void deletedClause_appearsStruckInFlow() {
        List<String> oldLines = List.of("[[t]]", "= T", "", "First clause,", "second clause,", "third clause.");
        List<String> newLines = List.of("[[t]]", "= T", "", "First clause,", "third clause.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(oldLines, newLines);
        assertThat(r.lines()).contains("[.diff-del]##second clause,##");
    }

    @Test
    void stamps_oneRefPerBoundary_collapsedWhenParagraphUniform() {
        AdocDiffMarker.StampSource stamps = new AdocDiffMarker.StampSource() {
            @Override
            public String activeRef(int newLineIndex) {
                return "footnote:s1a[STAMP Active]";
            }

            @Override
            public String inactiveRef() {
                return "footnote:s1i[STAMP Inactive]";
            }
        };
        List<String> oldLines = List.of("[[t]]", "= T", "",
                "First clause,", "second clause,", "third clause.",
                "",
                "Another paragraph here.");
        List<String> newLines = List.of("[[t]]", "= T", "",
                "First clause, revised,", "second clause, also revised,", "third clause.",
                "",
                "Another paragraph there.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(oldLines, newLines, stamps);
        String marked = String.join("\n", r.lines());
        // Two paragraphs, each with uniform stamps -> exactly one ref each.
        assertThat(marked.split("footnote:s1a", -1)).hasSize(3);
        assertThat(marked).contains("footnote:s1a[STAMP Active]");
    }

    @Test
    void stamps_pureDeletion_getsInactiveRef() {
        AdocDiffMarker.StampSource stamps = new AdocDiffMarker.StampSource() {
            @Override
            public String activeRef(int newLineIndex) {
                return "footnote:s1a[STAMP Active]";
            }

            @Override
            public String inactiveRef() {
                return "footnote:s1i[STAMP Inactive]";
            }
        };
        List<String> oldLines = List.of("[[t]]", "= T", "",
                "Kept line,", "removed line,", "kept end.");
        List<String> newLines = List.of("[[t]]", "= T", "",
                "Kept line,", "kept end.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(oldLines, newLines, stamps);
        String marked = String.join("\n", r.lines());
        assertThat(marked).contains("[.diff-del]##removed line,##footnote:s1i[STAMP Inactive]");
    }

    @Test
    void diagramHistory_appendsPreviousVersionWhenBodyChanged() {
        List<String> oldLines = List.of(
                "[[t]]", "= T", "",
                "[plantuml]", "----", "@startuml", "A --> B", "@enduml", "----");
        List<String> marked = List.of(
                "[[t]]", "= T", "",
                "[plantuml]", "----", "@startuml", "A --> C", "@enduml", "----");
        List<String> out = AdocDiffMarker.withDiagramHistory(oldLines, marked);
        assertThat(out).contains("[.diff-meta]##This diagram changed — source delta, then the previous rendering:##");
        assertThat(out).contains("- A --> B");
        assertThat(out).contains("+ A --> C");
        assertThat(out).contains("A --> B");
        assertThat(out.indexOf("A --> B")).isGreaterThan(out.indexOf("A --> C"));
    }

    @Test
    void diagramHistory_leavesUnchangedDiagramsAlone() {
        List<String> lines = List.of(
                "[[t]]", "= T", "",
                "[plantuml]", "----", "@startuml", "A --> B", "@enduml", "----");
        List<String> out = AdocDiffMarker.withDiagramHistory(lines, lines);
        assertThat(out).isEqualTo(lines);
    }

    @Test
    void markAdded_carriesBannerAfterTitle() {
        AdocDiffMarker.MarkResult r = AdocDiffMarker.markAdded(
                List.of("[[t]]", "= Brand New", "", "Body."), "HEAD");
        int title = r.lines().indexOf("= Brand New");
        assertThat(r.lines().get(title + 2)).isEqualTo("[NOTE]");
        assertThat(String.join("\n", r.lines())).contains("did not exist at HEAD");
        assertThat(r.insWords()).isGreaterThan(0);
    }

    @Test
    void changeSummaryNote_isInjectedAfterTitle() {
        List<String> changed = List.of(
                "// arch-sample",
                ":topic-id: arch-sample",
                ":topic-keywords: one, two",
                "",
                "[[arch-sample]]",
                "= Sample Topic",
                "",
                "A property graph",
                "with graph-shaped content",
                "(directed acyclic graphs, abstract syntax trees)",
                "in its nodes.");
        AdocDiffMarker.MarkResult r = AdocDiffMarker.mark(BASE, changed);
        int title = r.lines().indexOf("= Sample Topic");
        assertThat(r.lines().get(title + 2)).isEqualTo("[NOTE]");
        assertThat(String.join("\n", r.lines())).contains("Change summary: ~");
    }
}
