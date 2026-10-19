/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.typst.asciidoc;

import network.ike.docs.koncept.KonceptInlineMacro;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One small AsciiDoc input per construct, asserting the Typst the
 * {@code typst} backend emits (IKE-Network/ike-issues#1307 spike).
 */
class TypstConverterTest {

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaConverterRegistry().register(TypstConverter.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @AfterAll
    static void teardown() {
        if (asciidoctor != null) {
            asciidoctor.close();
        }
    }

    /** Body-only conversion (the {@code embedded} transform). */
    private static String body(String source) {
        return asciidoctor.convert(source, Options.builder()
                .backend("typst").standalone(false).safe(SafeMode.UNSAFE)
                .attributes(Attributes.builder().attribute("stem", "latexmath").build())
                .build());
    }

    /** Full-document conversion with the page setup. */
    private static String document(String source) {
        return asciidoctor.convert(source, Options.builder()
                .backend("typst").standalone(true).safe(SafeMode.UNSAFE)
                .attributes(Attributes.builder().attribute("stem", "latexmath").build())
                .build());
    }

    private static void assertContains(String haystack, String needle) {
        assertTrue(haystack.contains(needle), "expected:\n" + needle + "\nin:\n" + haystack);
    }

    @Test
    void documentTitleAttributesAndPreamble() {
        String typst = document("[[my-doc]]\n= The Title\n:topic-id: my-doc\n:topic-status: draft\n\nBody.\n");
        assertContains(typst, "#set document(title: \"The Title\")");
        assertContains(typst, "#set page(");
        assertContains(typst, "#set text(font: (\"Noto Serif\", \"Noto Sans\")");
        assertContains(typst, "#ike-title[The Title] <my-doc>");
        assertContains(typst, "\"topic-id\": \"my-doc\",");
        assertContains(typst, "\"topic-status\": \"draft\",");
        assertContains(typst, ")) <ike-attributes>");
        assertFalse(typst.contains("@preview/mitex"), "no math, no mitex import:\n" + typst);
    }

    @Test
    void sectionsToDepthFourWithLabels() {
        String typst = body("== One\n\n[[two-id]]\n=== Two\n\n==== Three\n\n===== Four\n\ntext\n");
        assertContains(typst, "= One <_one>\n");
        assertContains(typst, "== Two <two-id>\n");
        assertContains(typst, "=== Three <_three>\n");
        assertContains(typst, "==== Four <_four>\n");
    }

    @Test
    void paragraphAndInlineFormatting() {
        String typst = body("Plain *strong* and _emph_ and `mono` text.\n");
        assertEquals("Plain #strong[strong] and #emph[emph] and #raw(\"mono\") text.\n\n", typst);
    }

    @Test
    void markupSpecialCharactersAreEscapedAndEntitiesDecoded() {
        String typst = body("Hash # star * under_score at @ brackets [x] tilde ~ dollar $ angle <y> amp &.\n");
        assertContains(typst, "Hash \\# star \\* under\\_score at \\@ brackets \\[x\\] tilde \\~ dollar \\$ angle \\<y\\> amp &.");
    }

    @Test
    void expressionFollowedByPunctuationIsTerminated() {
        String typst = body("Bold *b*. Then (*c*).\n");
        assertContains(typst, "#strong[b];.");
        assertContains(typst, "(#strong[c])");
    }

    @Test
    void derivedSectionIdIgnoresInlineMarkupInTheTitle() {
        String typst = body("== Using *bold* here\n\nSee <<_using_bold_here>>.\n");
        assertContains(typst, "= Using #strong[bold] here <_using_bold_here>");
        assertContains(typst, "#link(<_using_bold_here>)[Using #strong[bold] here]");
    }

    @Test
    void shorthandsAndSemicolonsAreEscaped() {
        String typst = body("Semi *b*; dash a--b and -? soft and dots...\n");
        assertContains(typst, "Semi #strong[b]\\; dash a");
        assertContains(typst, "\\-? soft and dots");
    }

    @Test
    void unorderedAndNestedLists() {
        String typst = body("* one\n* two\n** nested\n");
        assertContains(typst, "- one\n- two\n");
        assertContains(typst, "  - nested\n");
    }

    @Test
    void orderedList() {
        String typst = body(". first\n. second\n");
        assertContains(typst, "+ first\n+ second\n");
    }

    @Test
    void descriptionList() {
        String typst = body("term:: its definition\nother:: more\n");
        assertContains(typst, "/ term: its definition\n/ other: more\n");
    }

    @Test
    void listingWithLanguageAndLiteralBlock() {
        String typst = body("[source,java]\n----\nint x = a < b; // c\n----\n\n....\nraw `text`\n....\n");
        assertContains(typst, "```java\nint x = a < b; // c\n```\n");
        assertContains(typst, "```\nraw `text`\n```\n");
    }

    @Test
    void listingTitleBecomesAFigureCaption() {
        String typst = body(".Listing title\n----\ncode\n----\n");
        assertContains(typst, "#figure(caption: [Listing title], kind: raw, supplement: [Listing])[\n```\ncode\n```\n]");
    }

    @Test
    void fenceGrowsBeyondBackticksInContent() {
        String typst = body("----\na ``` b\n----\n");
        assertContains(typst, "````\na ``` b\n````");
    }

    @Test
    void admonitionsSimpleAndCompound() {
        String typst = body("NOTE: Simple *note*.\n\n[WARNING]\n====\nFirst.\n\nSecond.\n====\n");
        assertContains(typst, "#ike-admonition(\"NOTE\")[\nSimple #strong[note];.\n]");
        assertContains(typst, "#ike-admonition(\"WARNING\")[\nFirst.\n\n\nSecond.\n]");
    }

    @Test
    void tableWithColsProportionsAndRepeatingHeader() {
        String typst = body(".Cap\n[[t1]]\n[cols=\"1,3\", options=\"header\"]\n|===\n| H1 | H2\n| a | b *c*\n| d | e\n|===\n");
        assertContains(typst, "columns: (25fr, 75fr, )");
        assertContains(typst, "table.header(repeat: true,\n      [#strong[H1]], [#strong[H2]], \n    )");
        assertContains(typst, "[a], [b #strong[c]], \n");
        assertContains(typst, "caption: [Cap],\n  kind: table,\n) <t1>");
    }

    @Test
    void tableSpansAndAsciidocCells() {
        String typst = body("[cols=\"1,1\"]\n|===\n2+| wide\na| para one\n\npara two\n| x\n|===\n");
        assertContains(typst, "table.cell(colspan: 2, )[wide]");
        assertContains(typst, "[para one\n\npara two]");
    }

    @Test
    void footnotesNativeAndRepeated() {
        String typst = body("Text.footnote:[A note with _emph_ and `code`.] More.footnote:n1[Named.] Again.footnote:n1[]\n");
        assertContains(typst, "Text.#footnote[A note with #emph[emph] and #raw(\"code\");.] More.");
        assertContains(typst, "#footnote[Named.]<fn-n1>");
        assertContains(typst, "Again.#footnote(<fn-n1>)");
    }

    @Test
    void crossReferencesResolvedAndUnresolved() {
        String typst = body("See <<sec-a>>, <<sec-a,custom>> and <<nowhere>>.\n\n[[sec-a]]\n== Section A\n");
        assertContains(typst, "#link(<sec-a>)[Section A]");
        assertContains(typst, "#link(<sec-a>)[custom]");
        assertContains(typst, "#ike-xref-unresolved(\"nowhere\")[nowhere]");
    }

    @Test
    void externalLink() {
        String typst = body("Visit https://ike.network[IKE] now.\n");
        assertContains(typst, "#link(\"https://ike.network\")[IKE] now.");
    }

    @Test
    void indexTermsBecomeMetadata() {
        String typst = body("indexterm:[alpha, beta]\n((gamma)) visible.\n");
        assertContains(typst, "#ike-index(\"alpha\", \"beta\")");
        assertContains(typst, "#ike-index(\"gamma\")gamma visible.");
    }

    @Test
    void bibliographyEntriesAndCitations() {
        String typst = body("Cited <<ref1>>.\n\n[bibliography]\n== References\n\n- [[[ref1]]] An entry.\n");
        assertContains(typst, "#link(<ref1>)[\\[ref1\\]]");
        assertContains(typst, "- #metadata(\"bibref\") <ref1>\\[ref1\\] An entry.");
    }

    @Test
    void imageBlockBecomesAFigure() {
        String typst = body(".Figure caption\nimage::pics/a.svg[Alt]\n");
        assertContains(typst, "#figure(\n  image(\"pics/a.svg\", width: 100%),\n  caption: [Figure caption],\n)");
    }

    @Test
    void imagePathIsRootRelativeUnderTypstRoot() {
        String typst = asciidoctor.convert("image::/tmp/root/sub/a.png[]\n", Options.builder()
                .backend("typst").standalone(false).safe(SafeMode.UNSAFE)
                .attributes(Attributes.builder().attribute("typst-root", "/tmp/root").build())
                .build());
        assertContains(typst, "image(\"/sub/a.png\"");
    }

    @Test
    void stemBlockAndInlineGoThroughMitex() {
        String typst = document("[stem]\n++++\n\\frac{a}{b}\n++++\n\nInline stem:[x^2].\n");
        assertContains(typst, "#import \"@preview/mitex:0.2.7\": *");
        assertContains(typst, "#mitex(\"\\\\frac{a}{b}\")");
        assertContains(typst, "#mi(\"x^2\")");
    }

    @Test
    void unknownBlockIsVisiblyMarkedNotDropped() {
        String typst = body("++++\n<b>raw html</b>\n++++\n");
        assertContains(typst, "#ike-unconverted(\"pass\")[\n```\n<b>raw html</b>\n```\n]");
    }

    @Test
    void konceptInlineMacroRendersAsBadge() {
        String typst = body("Patient has k:HeartFailure[] today.\n");
        assertContains(typst, "#ike-koncept(\"Heart Failure\", kind: \"CONCEPT\")");
        assertFalse(typst.contains("<svg"), "no SVG markup leaks into Typst:\n" + typst);
    }

    @Test
    void exampleQuoteAndThematicBreak() {
        String typst = body(".Ex\n====\nInside.\n====\n\n[quote, Who, Where]\n____\nSaid.\n____\n\n'''\n");
        assertContains(typst, "#ike-example([Ex])[\nInside.\n]");
        assertContains(typst, "#quote(block: true, attribution: [Who, #emph[Where]])[\nSaid.\n]");
        assertContains(typst, "#line(length: 100%, stroke: 0.5pt + luma(160))");
    }

    @Test
    void convertFileWritesATypFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws java.io.IOException {
        java.nio.file.Path source = dir.resolve("topic.adoc");
        java.nio.file.Files.writeString(source, "= T\n\nBody.\n");
        asciidoctor.convertFile(source.toFile(), Options.builder()
                .backend("typst").standalone(true).safe(SafeMode.UNSAFE).toDir(dir.toFile()).build());
        assertTrue(java.nio.file.Files.exists(dir.resolve("topic.typ")), "topic.typ written beside the source");
        assertEquals(TypstConverter.class, asciidoctor.javaConverterRegistry().resolve("typst"));
    }
}
