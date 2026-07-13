package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@code leaves:}/{@code children:} computed-query directives inside a
 * {@code [koncept-tree]} block (IKE-Network/ike-issues#879): a directive line expands,
 * before token parsing, into the equivalent {@code k:Identifier[]} lines, computed
 * fresh from the {@code broader} relationship already in {@code koncepts.yml}.
 */
class KonceptTreeDirectiveRenderingTest {

    /** The non-breaking space used for indentation, three per nesting level. */
    private static final String NBSP = " ";

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(KonceptTreeBlockProcessor.class);
    }

    @Test
    void leavesDirective_walksRecursivelyPastIntermediateNodes() {
        String html = convert("""
                [koncept-tree]
                ----
                k:Root[]
                  leaves: Root
                ----
                """);

        assertTrue(html.contains("Leaf One"), "Leaf1 is a leaf descendant of Root");
        assertTrue(html.contains("Leaf Two"), "Leaf2 is a leaf descendant of Root");
        assertTrue(html.contains("Direct Child"), "DirectChild has no children, so it is a leaf too");
        assertFalse(html.contains(">Mid<"),
                "Mid is not itself a leaf (it has children), so it is not emitted by leaves:");
    }

    @Test
    void childrenDirective_isDirectOnly_excludesGrandchildren() {
        String html = convert("""
                [koncept-tree]
                ----
                k:Root[]
                  children: Root
                ----
                """);

        assertTrue(html.contains("Mid"), "Mid is a direct child of Root");
        assertTrue(html.contains("Direct Child"), "DirectChild is a direct child of Root");
        assertFalse(html.contains("Leaf One"), "Leaf1 is a grandchild, not a direct child of Root");
        assertFalse(html.contains("Leaf Two"), "Leaf2 is a grandchild, not a direct child of Root");
    }

    @Test
    void directiveExpansion_nestsAtItsOwnIndent() {
        String html = convert("""
                [koncept-tree]
                ----
                k:Mid[]
                  leaves: Mid
                ----
                """);

        // Both leaves of Mid are its direct children in this fixture, so they should render at
        // the same nesting depth (3 nbsp) as a literal indented child line would.
        assertTrue(html.contains("   "), "Expanded lines nest at the directive's own indent");
        assertTrue(html.contains("Leaf One") && html.contains("Leaf Two"));
    }

    @Test
    void directiveForUnknownRoot_producesNoLinesRatherThanError() {
        String html = convert("""
                [koncept-tree]
                ----
                k:Root[]
                  leaves: NoSuchKoncept
                ----
                """);

        assertTrue(html.contains("class=\"koncept-tree\""), "Still a well-formed tree (Root alone parses)");
        assertTrue(html.contains("Root"));
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(org.asciidoctor.Attributes.builder()
                        .attribute("koncept-definitions-classpath", "/koncept-tree-directive-test.yml").build())
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
