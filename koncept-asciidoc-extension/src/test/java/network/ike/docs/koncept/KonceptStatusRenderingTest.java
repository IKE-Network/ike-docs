package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import network.ike.docs.konceptcore.KonceptStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Koncept logical-status cluster (ike-issues#742 design amendment, #940):
 * an inline Koncept badge leads with the copula from its stated EL++ expression —
 * {@code ≡} sufficiently defined, {@code ⊑} primitive, {@code ⊤} root — with the
 * {@code ⋎} fork appended for a multi-parent concept, always visible, never
 * hover-dependent. A concept with no stated definition leads with {@code C}. Kind sigils
 * and status glyphs never co-occur.
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
    void aConceptWithNoStatedDefinitionLeadsWithC() {
        String html = convert("k:Statusless[]");

        assertTrue(html.contains("koncept-status-undefined"),
                "a concept with no stated definition carries the undefined mark:\n" + html);
        assertTrue(html.contains(">C</span>") && html.contains("#2a5a8a"),
                "…a C in the name colour:\n" + html);
        assertTrue(html.contains("font-weight:bold"), "…set bold, as a kind sigil is:\n" + html);
        assertTrue(html.contains("No stated definition"),
                "…and the accessible name as the explanatory title:\n" + html);
        assertFalse(html.contains("koncept-sigil"), "…and no kind sigil:\n" + html);
    }

    @Test
    void theUndefinedLetterTakesNoFork() {
        String html = convert("k:StatuslessMulti[]");

        assertTrue(html.contains("koncept-status-undefined"),
                "two parents do not make a definition:\n" + html);
        assertFalse(html.contains("⋎"), "…and the fork follows only a copula:\n" + html);
        assertFalse(html.contains("Multiple parents"), "…so its title omits it too:\n" + html);
    }

    @Test
    void anAxiomWithNoRecordedStatusIsNotMarkedUndefined() {
        String html = convert("k:AxiomUnclassified[]");

        assertFalse(html.contains("koncept-status"),
                "a concept with an axiom has a stated definition, so it gets no C:\n" + html);
    }

    @Test
    void anUnrecognisedStatusNameIsNotMarkedUndefined() {
        assertEquals(KonceptStatus.NONE, KonceptDefinition.builder().identifier("Typo")
                .status("primtive").build().konceptStatus());
    }

    @Test
    void anUndefinedStatusNameIsHonouredBesideAnAxiom() {
        assertEquals(KonceptStatus.UNDEFINED, KonceptDefinition.builder().identifier("Named")
                .status("undefined").axiom("⊑ RootThing").build().konceptStatus());
    }

    @Test
    void glossaryHeadingCarriesTheSameC() {
        String mark = KonceptGlossaryEntryRenderer.markHtml(Optional.of(
                KonceptDefinition.builder().identifier("Plain").build()));

        assertTrue(mark.contains("koncept-status-undefined") && mark.contains(">C</span>"),
                "the glossary leads with the C an inline badge does:\n" + mark);
    }

    @Test
    void aNonConceptKindNeverLeadsWithC() {
        String mark = KonceptGlossaryEntryRenderer.markHtml(Optional.of(
                KonceptDefinition.builder().identifier("Plain").kind("pattern").build()));

        assertTrue(mark.contains("koncept-sigil-pattern"), "a pattern keeps its sigil:\n" + mark);
        assertFalse(mark.contains("koncept-status"), "…and carries no status:\n" + mark);
    }

    @Test
    void docbookCarriesTheCAsTextBeforeTheIdenticon() {
        String xml = convert("k:Statusless[]", "docbook5");

        assertTrue(xml.contains("<phrase role=\"koncept-status\">C </phrase>"),
                "FO print carries the glyph as text:\n" + xml);
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
        return convert(adoc, "html5");
    }

    private String convert(String adoc, String backend) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend(backend)
                .attributes(Attributes.builder()
                        .attribute("koncept-definitions-classpath", "/koncept-status-test.yml")
                        .build())
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
