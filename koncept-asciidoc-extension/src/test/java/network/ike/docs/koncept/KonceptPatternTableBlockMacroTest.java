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
 * Exercises the {@code koncept-pattern-table::Identifier[]} block macro
 * (IKE-Network/ike-issues#883): a pattern's shape spliced into the document as a real,
 * captioned AsciiDoc table — Model Feature label rows, Meaning | Purpose | Data type
 * badge rows, and live {@code e.g.} example rows — re-parsed so the {@code k:} badges
 * in its cells resolve exactly as hand-typed ones would.
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
    void rendersACaptionedNumberedXrefableTable() {
        String html = convert("koncept-pattern-table::PatternWithExample[]",
                "/pattern-shape-example-test.yml");

        assertTrue(html.contains("koncept-pattern-table"),
                "the table carries its stylesheet-hook role:\n" + html);
        assertTrue(html.contains("id=\"koncept-pattern-table-PatternWithExample\""),
                "the table carries a stable anchor for <<xref>> from prose:\n" + html);
        assertTrue(html.contains("Table 1. Pattern With Example shape"),
                "Asciidoctor numbers the caption — the reason this is a real table, "
                        + "not glossary-style raw HTML:\n" + html);
        assertTrue(html.contains("<th") && html.contains("Meaning")
                        && html.contains("Purpose") && html.contains("Data type"),
                "the header row names the three columns:\n" + html);
    }

    @Test
    void rendersOneThreeRowGroupPerModelFeature() {
        String html = convert("koncept-pattern-table::PatternWithExample[]",
                "/pattern-shape-example-test.yml");

        assertTrue(html.contains("colspan=\"3\""),
                "label and e.g. rows span all three columns:\n" + html);
        assertTrue(html.contains("Referenced component"),
                "the referenced component leads the Model Features:\n" + html);
        // "pattern's" is asserted without its apostrophe — Asciidoctor's typographic
        // substitution curls it in the output.
        assertTrue(html.contains("semantics attach to") && html.contains("STAMP"),
                "its label row carries the generic any-component phrase, not concept-only:\n" + html);
        assertTrue(html.contains("Field 1") && html.contains("Field 2"),
                "fields display 1-indexed:\n" + html);
        assertFalse(html.contains("Field 0"),
                "the referenced component is never numbered as Field 0:\n" + html);

        assertTrue(html.contains("href=\"#koncept-ExampleTestMeaning\"")
                        && html.contains("href=\"#koncept-ExampleTestPurpose\""),
                "the referenced component's meaning/purpose render as resolved badges:\n" + html);
        assertTrue(html.contains("href=\"#koncept-ExampleFieldOneMeaning\"")
                        && html.contains("href=\"#koncept-ExampleFieldOnePurpose\"")
                        && html.contains("href=\"#koncept-ExampleFieldOneType\""),
                "a field's meaning/purpose/dataType render as resolved badges:\n" + html);
        assertFalse(html.contains("k:ExampleFieldOneMeaning[]"),
                "no raw k: token text survives un-parsed:\n" + html);
    }

    @Test
    void referencedComponentDataTypeCellIsAnEmDash() {
        String html = convert("koncept-pattern-table::PatternWithExample[]",
                "/pattern-shape-example-test.yml");

        assertTrue(html.contains("—"),
                "the referenced component has no data type — its cell is an em dash:\n" + html);
    }

    @Test
    void exampleRowsBadgeResolvableValuesAndPassThroughLiterals() {
        String html = convert("koncept-pattern-table::PatternWithExample[]",
                "/pattern-shape-example-test.yml");

        assertTrue(html.contains("e.g."), "example rows carry the e.g. marker:\n" + html);
        assertTrue(html.contains("koncept-feature-example"),
                "example rows carry their stylesheet-hook role:\n" + html);
        assertTrue(html.contains("a literal example value"),
                "an example that resolves to no koncept renders as plain text:\n" + html);
        assertFalse(html.contains("href=\"#koncept-a literal example value\""),
                "…and is not wrapped as a badge link:\n" + html);
    }

    @Test
    void patternWithoutExamples_omitsExampleRows() {
        String html = convert("koncept-pattern-table::TestPattern[]", "/pattern-shape-test.yml");

        assertTrue(html.contains("Table 1. Test Pattern shape"),
                "the shape table still renders without examples:\n" + html);
        assertFalse(html.contains("e.g."),
                "no example semantic extracted means no e.g. rows, not blank ones:\n" + html);
    }

    @Test
    void plainConcept_rendersPlaceholderNotBlank() {
        String html = convert("koncept-pattern-table::PlainConcept[]", "/pattern-shape-test.yml");

        assertTrue(html.contains("No pattern shape"),
                "a concept with no pattern shape renders a visible placeholder:\n" + html);
        assertFalse(html.contains("<table"), "…and no table:\n" + html);
    }

    @Test
    void unknownIdentifier_rendersPlaceholderNotError() {
        String html = convert("koncept-pattern-table::NoSuchKoncept[]", "/pattern-shape-test.yml");

        assertTrue(html.contains("No pattern shape"),
                "an unresolvable identifier degrades to the same placeholder, not a crash:\n" + html);
    }

    private String convert(String adoc, String definitionsClasspath) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(Attributes.builder()
                        .attribute("koncept-definitions-classpath", definitionsClasspath).build())
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
