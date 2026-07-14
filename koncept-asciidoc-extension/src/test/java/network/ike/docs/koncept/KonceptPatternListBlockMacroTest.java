package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@code koncept-pattern-list::[]} block macro (IKE-Network/ike-issues#880):
 * a "List of Patterns" index — the "List of Tables"/"List of Figures" book convention,
 * styled like the document's own table of contents — listing every {@code kind: pattern}
 * koncept, alphabetical by label, each a plain link to its own Koncept Glossary entry.
 */
class KonceptPatternListBlockMacroTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
        asciidoctor.javaExtensionRegistry().blockMacro(KonceptPatternListBlockMacro.class);
    }

    @Test
    void listsOnlyPatterns_notPlainConcepts() {
        String html = convert("koncept-pattern-list::[]");

        assertTrue(html.contains("koncept-pattern-list"), "must render the pattern list:\n" + html);
        assertTrue(html.contains("List of Patterns"), "must carry the List of Patterns title:\n" + html);
        assertTrue(html.contains("#koncept-TestPattern"), "must link to TestPattern:\n" + html);
        assertTrue(html.contains("#koncept-AnotherPattern"), "must link to AnotherPattern:\n" + html);
        assertFalse(html.contains("#koncept-PlainConcept"),
                "a plain (non-pattern) concept must not appear in the list:\n" + html);
    }

    @Test
    void entriesAreSortedAlphabeticallyByLabelAndNumbered() {
        String html = convert("koncept-pattern-list::[]");

        int anotherIndex = html.indexOf("#koncept-AnotherPattern");
        int testIndex = html.indexOf("#koncept-TestPattern");
        assertTrue(anotherIndex >= 0 && testIndex >= 0 && anotherIndex < testIndex,
                "\"Another Pattern\" must sort before \"Test Pattern\":\n" + html);
        assertTrue(html.contains(">1. Another Pattern<"), "the first entry must be numbered 1:\n" + html);
        assertTrue(html.contains(">2. Test Pattern<"), "the second entry must be numbered 2:\n" + html);
    }

    @Test
    void entriesAreLinkOnly_styledLikeTheTableOfContents_noDataColumns() {
        String html = convert("koncept-pattern-list::[]");

        assertTrue(html.contains("<ul class=\"sectlevel1\">"),
                "must reuse the same list styling as the document's own table of contents:\n" + html);
        assertFalse(html.contains("<table"), "must not render as a data table:\n" + html);
        assertFalse(html.contains("#koncept-TestMeaning") || html.contains("#koncept-TestPurpose"),
                "must not render referenced-component columns -- that belongs to the glossary's own"
                        + " pattern-shape table, not this index:\n" + html);
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(org.asciidoctor.Attributes.builder()
                        .attribute("koncept-definitions-classpath", "/pattern-shape-test.yml").build())
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
