package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@code koncept-pattern-table::[]} block macro (IKE-Network/ike-issues#880):
 * a "Table of Patterns" index listing every {@code kind: pattern} koncept, alphabetical by
 * label, each linking to its own Koncept Glossary entry.
 */
class KonceptPatternTableBlockMacroTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
        asciidoctor.javaExtensionRegistry().blockMacro(KonceptPatternTableBlockMacro.class);
    }

    @Test
    void listsOnlyPatterns_notPlainConcepts() {
        String html = convert("koncept-pattern-table::[]");

        assertTrue(html.contains("koncept-pattern-table"), "must render the pattern-table:\n" + html);
        assertTrue(html.contains("#koncept-TestPattern"), "must link to TestPattern:\n" + html);
        assertTrue(html.contains("#koncept-AnotherPattern"), "must link to AnotherPattern:\n" + html);
        assertFalse(html.contains("#koncept-PlainConcept"),
                "a plain (non-pattern) concept must not appear in the table:\n" + html);
    }

    @Test
    void rowsAreSortedAlphabeticallyByLabel() {
        String html = convert("koncept-pattern-table::[]");

        int anotherIndex = html.indexOf("#koncept-AnotherPattern");
        int testIndex = html.indexOf("#koncept-TestPattern");
        assertTrue(anotherIndex >= 0 && testIndex >= 0 && anotherIndex < testIndex,
                "\"Another Pattern\" must sort before \"Test Pattern\":\n" + html);
    }

    @Test
    void aPatternWithNoFieldsStillRendersItsReferencedComponent() {
        String html = convert("koncept-pattern-table::[]");

        assertTrue(html.contains("#koncept-TestMeaning") && html.contains("#koncept-TestPurpose"),
                "AnotherPattern's referenced-component meaning/purpose must still render:\n" + html);
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
