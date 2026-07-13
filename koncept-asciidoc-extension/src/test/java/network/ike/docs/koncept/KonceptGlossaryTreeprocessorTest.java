package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link KonceptGlossaryTreeprocessor}: the grouped,
 * {@code koncept-glossary-all} glossary that builds real, TOC-visible AST
 * sections (IKE-Network/ike-issues#877), against the dedicated
 * {@code koncept-glossary-tree-test.yml} fixture (section/since/comments/
 * retiredComments — data the bundled demo {@code koncepts.yml} doesn't have).
 */
class KonceptGlossaryTreeprocessorTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(KonceptInlineMacro.class)
                .treeprocessor(KonceptGlossaryTreeprocessor.class)
                .postprocessor(KonceptGlossaryProcessor.class);
    }

    @Test
    void groupsAppearAsRealTocVisibleSections() {
        String html = convertAll("An empty document body.");

        // Asciidoctor's own TOC is built from the parsed AST during conversion --
        // if these titles appear there, they are real Section nodes the TOC-builder
        // walked, not string matches against a postprocessor's raw HTML append.
        int tocStart = html.indexOf("id=\"toc\"");
        assertTrue(tocStart >= 0, "Document should render a table of contents:\n" + html);
        String toc = html.substring(tocStart);

        assertTrue(toc.contains("Koncept Glossary"), "TOC should list the glossary parent section:\n" + toc);
        assertTrue(toc.contains("Root A"), "TOC should list the Root A group as its own section:\n" + toc);
        assertTrue(toc.contains("Root B"), "TOC should list the Root B group as its own section:\n" + toc);
        assertTrue(toc.contains("Unclassified Koncepts"),
                "TOC should list the no-section fallback group:\n" + toc);
    }

    @Test
    void memberEntriesGroupedUnderTheirOwnRoot() {
        String html = convertAll("An empty document body.");

        int rootAIndex = html.indexOf("id=\"koncept-RootA\"");
        int childA1Index = html.indexOf("id=\"koncept-ChildA1\"");
        int childA2Index = html.indexOf("id=\"koncept-ChildA2\"");
        int rootBIndex = html.indexOf("id=\"koncept-RootB\"");
        int childB1Index = html.indexOf("id=\"koncept-ChildB1\"");

        assertTrue(rootAIndex >= 0 && childA1Index >= 0 && childA2Index >= 0
                && rootBIndex >= 0 && childB1Index >= 0, "Every koncept should render an entry:\n" + html);
        assertTrue(rootAIndex < rootBIndex, "Root A's group should render before Root B's");
        assertTrue(childA1Index < rootBIndex && childA2Index < rootBIndex,
                "Root A's members must render within Root A's group, before Root B's group starts");
    }

    @Test
    void sectionNarrativeComesFromTheRootsOwnDefinition() {
        String html = convertAll("An empty document body.");

        assertTrue(html.contains("koncept-section-narrative"), "A group narrative block should render");
        assertTrue(html.contains("Definition of root A, used as the group&#8217;s narrative.")
                        || html.contains("Definition of root A, used as the group's narrative."),
                "Root A's own definition should appear as its group's narrative:\n" + html);
    }

    @Test
    void historyFieldsRenderOnlyWhenPresent() {
        String html = convertAll("An empty document body.");

        String childA1Entry = entryHtml(html, "ChildA1");
        assertTrue(childA1Entry.contains("koncept-since") && childA1Entry.contains("2020-01-01T00:00:00Z"),
                "ChildA1 has a since: field, should render it:\n" + childA1Entry);
        assertTrue(childA1Entry.contains("koncept-comments") && childA1Entry.contains("Still active comment"),
                "ChildA1 has an active comment, should render it:\n" + childA1Entry);
        assertTrue(childA1Entry.contains("koncept-retired-comments") && childA1Entry.contains("Old comment text"),
                "ChildA1 has a retired comment, should render it:\n" + childA1Entry);

        String childA2Entry = entryHtml(html, "ChildA2");
        assertFalse(childA2Entry.contains("koncept-since"), "ChildA2 has no since:, must not render one");
        assertFalse(childA2Entry.contains("koncept-comments"), "ChildA2 has no comments:, must not render any");
        assertFalse(childA2Entry.contains("koncept-retired-comments"),
                "ChildA2 has no retiredComments:, must not render any");
    }

    /**
     * Extracts one koncept's {@code <dt>}/{@code <dd>} entry from converted HTML, from its
     * own {@code id="koncept-ID"} to the next entry's {@code <dt>} (or the enclosing
     * {@code </dl>}), whichever comes first — robust to member order, which reflects
     * however the YAML parser happened to iterate its map, not necessarily file order.
     */
    private static String entryHtml(String html, String id) {
        int start = html.indexOf("id=\"koncept-" + id + "\"");
        assertTrue(start >= 0, id + " should have rendered an entry:\n" + html);
        int nextDt = html.indexOf("<dt ", start + 1);
        int closeDl = html.indexOf("</dl>", start + 1);
        int end = (nextDt >= 0 && nextDt < closeDl) ? nextDt : closeDl;
        return html.substring(start, end >= 0 ? end : html.length());
    }

    @Test
    void referencedOnlyModeUnaffectedWhenGlossaryAllNotSet() {
        String html = convert("Referencing only k:ChildA1[].");

        assertFalse(html.contains("Koncept Glossary"),
                "Without koncept-glossary-all, the grouped glossary must not appear:\n" + html);
        assertTrue(html.contains("Referenced Koncepts"),
                "The default, referenced-only glossary should still render");
        assertTrue(html.contains("id=\"koncept-ChildA1\""), "The referenced koncept should still get an entry");
        assertFalse(html.contains("id=\"koncept-ChildB1\""),
                "An unreferenced koncept must not appear in referenced-only mode");
    }

    private String convertAll(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(Attributes.builder()
                        .attribute("toc", "")
                        .attribute("koncept-definitions-classpath", "/koncept-glossary-tree-test.yml")
                        .attribute("koncept-glossary-all", "")
                        .build())
                .build();
        return asciidoctor.convert(adoc, options);
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(Attributes.builder()
                        .attribute("koncept-definitions-classpath", "/koncept-glossary-tree-test.yml")
                        .build())
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
