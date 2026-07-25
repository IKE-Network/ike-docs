package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the adoc SVG badge is honest about the component kind (ike-issues#638) and converged
 * to the identicon-pill look (ike-issues#864): the spec's light pill with a small-caps IKE-blue
 * label — no solid-blue box, white text, or {@code K} prefix — a coloured letter sigil for the
 * marked kinds, the locked gray pentagon for a stamp, and the retired treatment for an inactive
 * referent. The same values every other renderer reads from {@code KonceptAppearance}.
 */
class KonceptSvgRendererTest {

    @Test
    void conceptIsTheLightPillNotTheBlueKBox() {
        String svg = KonceptSvgRenderer.render("HeartFailure", "Heart Failure", KonceptKind.CONCEPT);
        assertFalse(svg.contains(">K</text>"), "the K prefix is retired (#864 convergence)");
        assertTrue(svg.contains("fill=\"#e9eff6\""), "the pill is the spec's light fill");
        assertTrue(svg.contains("fill=\"#2a5a8a\""), "the label is the spec's IKE blue");
        assertTrue(svg.contains("font-variant:small-caps"), "the label renders in small caps");
        assertTrue(svg.contains("rx=\"6\""), "the corner radius is the spec's 6");
        assertFalse(svg.contains("#b8860b"), "a concept carries no kind-sigil colour");
        assertFalse(svg.contains("<polygon"), "a concept is not a pentagon");
    }

    @Test
    void retiredConceptStrikesThroughInTheRetiredColour() {
        String svg = KonceptSvgRenderer.render("OldConcept", "Old Concept", KonceptKind.CONCEPT, true);
        assertTrue(svg.contains("fill=\"#b00020\""), "the retired label colour (#742 parity)");
        assertTrue(svg.contains("text-decoration:line-through"), "the dedicated retired signal");
        assertFalse(svg.contains("fill=\"#2a5a8a\""), "not the active blue");
    }

    @Test
    void descriptionPrependsItsAmberLetterSigil() {
        String svg = KonceptSvgRenderer.render("Foo", "Foo (en)", KonceptKind.DESCRIPTION);
        assertTrue(svg.contains("fill=\"#b8860b\""), "the amber D sigil colour");
        assertTrue(svg.contains(">D</text>"), "the D glyph");
        assertTrue(svg.contains(">Foo (en)</text>"), "the label is still shown");
    }

    @Test
    void stampRendersTheLockedPentagonPlusCompactText() {
        String svg = KonceptSvgRenderer.render("ExampleStamp", "Active · 2024-06-23 14:30 · KEC", KonceptKind.STAMP);
        assertTrue(svg.contains("<polygon points="), "the stamp pentagon outline");
        assertTrue(svg.contains("stroke=\"#888780\""), "the locked stamp gray");
        int circles = svg.split("<circle", -1).length - 1;
        assertEquals(6, circles, "five asymmetric reading dots plus the centre hub");
        assertTrue(svg.contains(">Active · 2024-06-23 14:30 · KEC</text>"),
                "the stamp shows its compact provenance text, not a name");
        // render(...) for a stamp delegates to the same output as renderStampSigil(...).
        assertEquals(KonceptSvgRenderer.renderStampSigil("ExampleStamp", "Active · 2024-06-23 14:30 · KEC"), svg);
    }
}
