package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the figure block macros (IKE-Network/ike-issues#883):
 * {@code koncept-identicon-figure::} and {@code koncept-badge-anatomy::} render real,
 * captioned figures — numbered by Asciidoctor, with alt text per the image standards —
 * from the same primitives the badges use.
 */
class KonceptFigureMacrosTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().blockMacro(KonceptIdenticonFigureBlockMacro.class);
        asciidoctor.javaExtensionRegistry().blockMacro(KonceptBadgeAnatomyBlockMacro.class);
    }

    @Test
    void identiconFigure_rendersACaptionedNumberedImageBlock() {
        String html = convert("""
                .The identicon of Heart Failure
                koncept-identicon-figure::HeartFailure[width=96]
                """);

        assertTrue(html.contains("imageblock"), "renders a real image block:\n" + html);
        assertTrue(html.contains("src=\"data:image/png;base64,"),
                "the html family embeds the LifeHash PNG as a data URI:\n" + html);
        assertTrue(html.contains("Figure 1. The identicon of Heart Failure"),
                "the block title becomes a numbered figure caption:\n" + html);
        assertTrue(html.contains("alt=\"Heart Failure identicon\""),
                "alt text names the koncept, per the image standards:\n" + html);
        assertTrue(html.contains("width=\"96\""), "the width attribute sizes the display:\n" + html);
    }

    @Test
    void badgeAnatomy_rendersACaptionedFigureComposedFromTheRealPrimitives() {
        String html = convert("""
                .Anatomy of a Koncept Badge
                koncept-badge-anatomy::HeartFailurePreferredName[]
                """);

        assertTrue(html.contains("imageblock"), "renders a real image block:\n" + html);
        assertTrue(html.contains("src=\"data:image/png;base64,"),
                "the composed anatomy PNG embeds as a data URI:\n" + html);
        assertTrue(html.contains("Figure 1. Anatomy of a Koncept Badge"),
                "the block title becomes a numbered figure caption:\n" + html);
        assertTrue(html.contains("kind sigil, identicon, name"),
                "alt text names the parts:\n" + html);
    }

    @Test
    void stampAnatomy_rendersProvenanceShapedViaACuratedStamp() {
        String html = convert("""
                .Anatomy of a STAMP badge
                koncept-badge-anatomy::ExampleStamp[]
                """);

        assertTrue(html.contains("imageblock") && html.contains("src=\"data:image/png;base64,"),
                "the curated demo stamp renders as a composed figure:\n" + html);
        assertTrue(html.contains("kind sigil, identicon, provenance"),
                "a stamp anatomy labels the text part provenance, not name:\n" + html);
    }

    @Test
    void stampAnatomy_acceptsKindAndLabelOverridesForAnUncuratedTypedTarget() {
        String html = convert("""
                .Anatomy of a STAMP badge
                koncept-badge-anatomy::uuid=1e041b79-2aa5-5d19-ad09-05a079cb396f[kind=stamp, label="Active · 2026-07-12 00:00 · IKE Community"]
                """);

        assertTrue(html.contains("imageblock") && html.contains("src=\"data:image/png;base64,"),
                "a source-declared identity with asserted kind and label renders:\n" + html);
        assertTrue(html.contains("Anatomy of the Active · 2026-07-12 00:00 · IKE Community badge"),
                "the asserted label carries into the alt text:\n" + html);
        assertTrue(html.contains("kind sigil, identicon, provenance"),
                "the asserted stamp kind drives the provenance part label:\n" + html);
    }

    @Test
    void unresolvableTarget_rendersAVisiblePlaceholderNotABrokenFigure() {
        String html = convert("koncept-identicon-figure::NoSuchKoncept[]");

        assertTrue(html.contains("No computable identity"),
                "an unresolvable target degrades to a visible placeholder:\n" + html);
        assertFalse(html.contains("imageblock"), "…not a broken image block:\n" + html);
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
