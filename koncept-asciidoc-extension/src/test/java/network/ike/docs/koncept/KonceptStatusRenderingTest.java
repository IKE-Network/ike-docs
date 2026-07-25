package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Koncept logical-status cluster (ike-issues#742 design amendment, #940):
 * an inline Koncept badge leads with the copula from its stated EL++ expression —
 * {@code ≡} sufficiently defined, {@code ⊑} primitive, {@code ⊤} root — with the
 * {@code ⋎} fork appended for a multi-parent concept, always visible, never
 * hover-dependent. Kind sigils and status glyphs never co-occur.
 */
class KonceptStatusRenderingTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @AfterAll
    static void teardown() {
        if (asciidoctor != null) {
            asciidoctor.close();
        }
    }

    @Test
    void definedConceptLeadsWithTheEquivalenceCopula() {
        String html = convert("k:DefinedSingle[]");

        assertTrue(html.contains("koncept-status-defined") && html.contains("≡"),
                "a sufficiently defined concept leads with ≡:\n" + html);
        assertTrue(html.contains("#3b8c2f"), "…in the defined green:\n" + html);
        assertFalse(html.contains("⋎"), "…with no fork on a single parent:\n" + html);
        assertTrue(html.contains("Sufficiently defined"),
                "…and the accessible name as the explanatory title:\n" + html);
    }

    @Test
    void primitiveMultiParentCarriesTheCopulaPlusFork() {
        String html = convert("k:PrimitiveMulti[]");

        assertTrue(html.contains("koncept-status-primitive") && html.contains("⊑"),
                "a primitive concept leads with ⊑:\n" + html);
        assertTrue(html.contains("koncept-status-multiparent") && html.contains("⋎"),
                "…with the ⋎ fork appended for more than one parent:\n" + html);
        assertTrue(html.contains("#6b7682") && html.contains("#185fa5"),
                "…each glyph in its own colour:\n" + html);
        assertTrue(html.contains("Primitive · Multiple parents"),
                "…and both accessible names in the title:\n" + html);
    }

    @Test
    void rootLeadsWithTop() {
        String html = convert("k:RootThing[]");

        assertTrue(html.contains("koncept-status-root") && html.contains("⊤"),
                "a taxonomy root leads with ⊤:\n" + html);
    }

    @Test
    void statuslessConceptStaysTrulyBare() {
        String html = convert("k:Statusless[]");

        assertFalse(html.contains("koncept-status"),
                "a concept with no stated definition carries no status mark:\n" + html);
        assertFalse(html.contains("koncept-sigil"),
                "…and no kind sigil — Koncept bare:\n" + html);
    }

    @Test
    void retiredConceptRendersStruckThroughInTheRetiredColour() {
        String html = convert("k:RetiredConcept[]");

        assertTrue(html.contains("text-decoration:line-through"),
                "a retired referent's label is struck through (#742 parity, #864):\n" + html);
        assertTrue(html.contains("#b00020"),
                "…in the retired colour:\n" + html);
        assertTrue(html.contains("koncept-status-primitive"),
                "…and the status cluster still leads — retirement and status are orthogonal:\n" + html);
    }

    @Test
    void kindSigilWinsOverAStrayStatusField() {
        String html = convert("k:StatusedPattern[]");

        assertTrue(html.contains("koncept-sigil-pattern"),
                "a pattern leads with its P sigil:\n" + html);
        assertFalse(html.contains("koncept-status"),
                "…never a status cluster — the marks are mutually exclusive:\n" + html);
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(Attributes.builder()
                        .attribute("koncept-definitions-classpath", "/koncept-status-test.yml")
                        .build())
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
