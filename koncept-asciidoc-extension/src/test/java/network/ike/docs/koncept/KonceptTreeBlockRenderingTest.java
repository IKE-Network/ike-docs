package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the {@code [koncept-tree]} block: an indented list of Koncept chips that is the static
 * projection of the live assistant tree ({@code IKE-Network/ike-issues#827}).
 */
class KonceptTreeBlockRenderingTest {

    /** SNOMED CT id of {@code HeartFailure} in the bundled {@code koncepts.yml}. */
    private static final String HEART_FAILURE_SCTID = "84114007";
    /** The non-breaking space used for indentation, three per nesting level. */
    private static final String NBSP = "\u00A0";

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(KonceptTreeBlockProcessor.class);
    }

    @Test
    void rendersIndentedTreeWithChips() {
        String html = convert("""
                [koncept-tree]
                ----
                k:HeartFailure[]
                  k:AorticStenosis[Aortic Stenosis]
                ----
                """);

        assertTrue(html.contains("class=\"koncept-tree\""), "Should render the tree container");
        assertTrue(html.contains("class=\"koncept-tree-row\""), "Should render row wrappers");
        assertTrue(html.contains("href=\"#koncept-HeartFailure\""),
                "A name-key node links to its glossary anchor (the cross-reference)");
        assertTrue(html.contains("src=\"data:image/png;base64,"),
                "Resolvable nodes carry the identicon as an inline data URI");
        assertTrue(html.contains("Heart Failure"), "Root label from the definition");
        assertTrue(html.contains("Aortic Stenosis"), "Child label from the bracket override");
    }

    @Test
    void indentsWithNonBreakingSpaces_notLineArt() {
        String html = convert("""
                [koncept-tree]
                ----
                k:HeartFailure[]
                  k:AorticStenosis[Child]
                    k:AcuteMyocardialInfarction[Grandchild]
                ----
                """);

        // Depth 1 → 3 nbsp; depth 2 → 6 nbsp. Copy-survivable indentation, no box-drawing glyphs.
        assertTrue(html.contains(NBSP.repeat(3)), "Depth-1 node indented by three nbsp");
        assertTrue(html.contains(NBSP.repeat(6)), "Depth-2 node indented by six nbsp");
        assertFalse(html.contains("├") || html.contains("└") || html.contains("│"),
                "No box-drawing connectors (they break on copy/paste)");
    }

    @Test
    void typedSctidResolvesSameIdenticonAsNameKey() {
        // The whole point of "one source, every medium": a Komet-emitted typed token and the
        // doc-native name key for the same concept produce a byte-identical identicon.
        String expectedDataUri = IdenticonRenderer.dataUri(
                KonceptIdentity.idString(List.of(SnomedUuids.fromSnomed(HEART_FAILURE_SCTID))));

        String html = convert("""
                [koncept-tree]
                ----
                k:HeartFailure[]
                k:sctid=84114007[Heart failure by id]
                ----
                """);

        int occurrences = countOccurrences(html, expectedDataUri);
        assertEquals(2, occurrences,
                "Name-key and typed-sctid nodes for the same concept render the same identicon");
    }

    @Test
    void identitySurvivesPlainTextCopy_viaAltText() {
        String html = convert("""
                [koncept-tree]
                ----
                k:sctid=84114007[Heart failure]
                ----
                """);

        // The alt carries "name · PublicId" (the concept's UUID, preferred over the sctid) so a
        // plain-text paste keeps the identity. 84114007 is HeartFailure in koncepts.yml (sctid only),
        // so its PublicId is the SNOMED-derived UUID.
        String publicId = SnomedUuids.fromSnomed("84114007").toString();
        assertTrue(html.contains("alt=\"Heart failure · " + publicId + "\""),
                "Identicon alt carries name-plus-PublicId for plain-text copy fidelity");
        // Resolving the sctid through koncepts.yml also yields the glossary cross-reference.
        assertTrue(html.contains("href=\"#koncept-HeartFailure\""),
                "A typed sctid for a curated concept cross-references its glossary entry");
    }

    @Test
    void publicIdPreferredOverSctidDerivation() {
        // DualId (in koncept-tree-test.yml) has sctid 12345678 AND an explicit PublicId whose UUIDs
        // are not the SNOMED-derived one. A k:sctid= token must resolve THROUGH koncepts.yml and use
        // the curated PublicId, never a UUID derived from the sctid.
        String publicIdIcon = IdenticonRenderer.dataUri(KonceptIdentity.idString(List.of(
                UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001"),
                UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002"))));
        String sctidDerivedIcon = IdenticonRenderer.dataUri(
                KonceptIdentity.idString(List.of(SnomedUuids.fromSnomed("12345678"))));

        String html = convertWith("/koncept-tree-test.yml", """
                [koncept-tree]
                ----
                k:sctid=12345678[Dual]
                ----
                """);

        assertTrue(html.contains(publicIdIcon), "Curated PublicId drives the identicon");
        assertFalse(html.contains(sctidDerivedIcon), "The sctid-derived UUID is NOT used");
        assertTrue(html.contains("href=\"#koncept-DualId\""),
                "The typed token resolves through koncepts.yml, gaining the cross-reference");
    }

    @Test
    void multipleIdsInOneToken_useTheCarriedPublicId() {
        // A k:uuid= token may carry several UUIDs — one multi-id PublicId. Uncurated, it renders the
        // identicon of exactly those UUIDs, in order.
        UUID u1 = UUID.fromString("11111111-0000-4000-8000-000000000001");
        UUID u2 = UUID.fromString("22222222-0000-4000-8000-000000000002");
        String expected = IdenticonRenderer.dataUri(KonceptIdentity.idString(List.of(u1, u2)));

        String html = convert("""
                [koncept-tree]
                ----
                k:uuid=11111111-0000-4000-8000-000000000001,22222222-0000-4000-8000-000000000002[Multi]
                ----
                """);

        assertTrue(html.contains(expected), "Multi-UUID token renders the identicon of both UUIDs");
        assertTrue(html.contains("Multi"), "…with its authoring label");
    }

    @Test
    void nidNodeFallsBackToLabel_liveOnly() {
        String html = convert("""
                [koncept-tree]
                ----
                k:nid=-2147480000[Live-only node]
                ----
                """);

        assertTrue(html.contains("class=\"koncept-tree\""), "Still a well-formed tree");
        assertTrue(html.contains("Live-only node"), "The nid node shows its authoring label");
        assertFalse(html.contains("src=\"data:image/png;base64,"),
                "A nid has no static identicon — it is a live-store id");
    }

    @Test
    void malformedBlockFallsBackToLiteral() {
        String html = convert("""
                [koncept-tree]
                ----
                this is not a k: token
                ----
                """);

        assertFalse(html.contains("class=\"koncept-tree\""),
                "A block with a malformed line declines to a literal, never a partial tree");
        assertTrue(html.contains("this is not a k: token"), "The raw content is preserved");
    }

    @Test
    void identiconToggleOff_dropsImagesKeepsLabels() {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(org.asciidoctor.Attributes.builder()
                        .attribute("koncept-identicon", "false").build())
                .build();
        String html = asciidoctor.convert("""
                [koncept-tree]
                ----
                k:HeartFailure[]
                ----
                """, options);

        assertFalse(html.contains("src=\"data:image/png;base64,"),
                ":koncept-identicon: false suppresses the identicon");
        assertTrue(html.contains("Heart Failure"), "…but the labelled node remains");
    }

    private String convert(String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .build();
        return asciidoctor.convert(adoc, options);
    }

    private String convertWith(String classpathYml, String adoc) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .attributes(org.asciidoctor.Attributes.builder()
                        .attribute("koncept-definitions-classpath", classpathYml).build())
                .build();
        return asciidoctor.convert(adoc, options);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }
}
