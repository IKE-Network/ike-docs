package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@code koncept-sigil:kind[]} inline macro (IKE-Network/ike-issues#883):
 * standalone kind sigils rendered from the same locked {@code KonceptKind}/
 * {@code StampSigilGeometry} data as the badges, so prose about the sigil scheme shows
 * the real marks.
 */
class KonceptSigilInlineMacroTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptSigilInlineMacro.class);
    }

    @Test
    void letterSigilsCarryTheLockedGlyphAndColour() {
        String html = convert("koncept-sigil:description[] koncept-sigil:semantic[] koncept-sigil:pattern[]");

        assertTrue(html.contains("#b8860b") && html.contains(">D</text>"),
                "description renders the amber D:\n" + html);
        assertTrue(html.contains("#3b8c2f") && html.contains(">S</text>"),
                "semantic renders the green S:\n" + html);
        assertTrue(html.contains("#7a4fb5") && html.contains(">P</text>"),
                "pattern renders the violet P:\n" + html);
        assertTrue(html.contains("koncept-sigil"), "sigils carry their stylesheet hook:\n" + html);
        assertFalse(html.contains("koncept-sigil:"), "no raw macro text survives un-parsed:\n" + html);
    }

    @Test
    void stampRendersThePentagonNotALetter() {
        String html = convert("koncept-sigil:stamp[]");

        assertTrue(html.contains("<polygon"), "the stamp sigil is the locked pentagon:\n" + html);
        assertTrue(html.contains("<circle"), "…with its reading dots and hub:\n" + html);
        assertTrue(html.contains("Stamp kind sigil"), "…carrying its accessible title:\n" + html);
    }

    @Test
    void kindNameIsCaseInsensitive() {
        String html = convert("koncept-sigil:STAMP[]");

        assertTrue(html.contains("<polygon"), "kind names resolve case-insensitively:\n" + html);
    }

    @Test
    void unknownKindRendersTheRedQuestionMark() {
        String html = convert("koncept-sigil:unknown[]");

        assertTrue(html.contains("#b00020") && html.contains(">?</text>"),
                "unknown renders the red ?:\n" + html);
    }

    @Test
    void bareConceptRendersNothing() {
        String html = convert("before koncept-sigil:concept[] after");

        assertTrue(html.contains("before") && html.contains("after"));
        assertFalse(html.contains("<svg"),
                "the bare concept kind has no sigil, by design:\n" + html);
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .build();
        return asciidoctor.convert(adoc, options);
    }
}
