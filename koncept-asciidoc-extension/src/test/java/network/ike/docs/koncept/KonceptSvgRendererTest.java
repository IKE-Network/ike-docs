package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the adoc SVG badge is honest about the component kind (ike-issues#638): a bare concept,
 * a coloured letter sigil for the others, and the locked gray pentagon for a stamp — the same
 * geometry the JavaFX node and the Java2D PNG render.
 */
class KonceptSvgRendererTest {

    @Test
    void conceptIsBareWithNoSigil() {
        String svg = KonceptSvgRenderer.render("HeartFailure", "Heart Failure", KonceptKind.CONCEPT);
        assertTrue(svg.contains(">K</text>"), "the concept badge keeps its K prefix");
        assertFalse(svg.contains("#b8860b"), "a concept carries no kind-sigil colour");
        assertFalse(svg.contains("<polygon"), "a concept is not a pentagon");
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
