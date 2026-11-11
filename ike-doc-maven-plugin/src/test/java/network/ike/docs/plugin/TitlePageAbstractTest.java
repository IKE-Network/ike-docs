package network.ike.docs.plugin;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TitlePageAbstract}: the abstract of a document is moved
 * into the header of the print HTML, so the print stylesheet sets it on the
 * title page.
 *
 * <p>Every document here is converted by the real {@code html5} backend, so
 * the tests follow what Asciidoctor writes and not a copy of it.
 */
class TitlePageAbstractTest {

    private static final String BRIEF =
            "We propose a way for a reviewer to decide what evidence to ask for.";

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void startAsciidoctor() {
        asciidoctor = Asciidoctor.Factory.create();
    }

    @AfterAll
    static void stopAsciidoctor() {
        asciidoctor.close();
    }

    // ── What is moved ────────────────────────────────────────────────

    @Test
    void openBlockAbstract_goesIntoTheHeaderBeforeTheContents() {
        String html = html("""
                = A Book
                Keith E Campbell
                :doctype: book
                :toc:

                [abstract]
                --
                %s

                The record names two things.

                * The decision.
                * The evidence.
                --

                == The problem

                Body text.
                """.formatted(BRIEF));

        String moved = TitlePageAbstract.relocate(html);

        String header = header(moved);
        assertThat(header)
                .contains("<div class=\"openblock ike-title-abstract\">")
                .contains("<p>" + BRIEF + "</p>")
                .contains("<p>The record names two things.</p>")
                .contains("<p>The evidence.</p>");
        assertThat(header.indexOf("class=\"details\""))
                .isLessThan(header.indexOf("ike-title-abstract"));
        assertThat(header.indexOf("ike-title-abstract"))
                .isLessThan(header.indexOf("<div id=\"toc\""));
        // The abstract was all the preamble held, so the body begins at the
        // first section.
        assertThat(content(moved))
                .doesNotContain(BRIEF)
                .doesNotContain("id=\"preamble\"")
                .startsWith("<div id=\"content\">\n<div class=\"sect1\">");
        assertThat(moved).doesNotContain("quoteblock abstract");
        assertThat(count(moved, "<div")).isEqualTo(count(html, "<div") - 1);
        assertThat(count(moved, "</div>")).isEqualTo(count(html, "</div>") - 1);
    }

    @Test
    void paragraphAbstract_isWrittenAsAParagraph() {
        String html = html("""
                = An Article
                :toc:

                [abstract]
                %s

                == The problem

                Body text.
                """.formatted(BRIEF));

        String header = header(TitlePageAbstract.relocate(html));

        assertThat(header).contains("""
                <div class="openblock ike-title-abstract">
                <div class="content">
                <div class="paragraph">
                <p>%s</p>
                </div>
                </div>
                </div>
                """.formatted(BRIEF));
    }

    @Test
    void abstractIsNotAnEntryInTheContents() {
        String html = html("""
                = A Book
                :doctype: book
                :toc:

                [abstract]
                %s

                == The problem

                Body text.
                """.formatted(BRIEF));

        String header = header(TitlePageAbstract.relocate(html));

        String toc = header.substring(header.indexOf("<div id=\"toc\""));
        assertThat(toc).contains("The problem").doesNotContain("propose");
    }

    @Test
    void taggedRegion_isShownOnTheTitlePageAndWholeInTheBody(@TempDir Path dir)
            throws IOException {
        Files.writeString(dir.resolve("summary.adoc"), """
                = The Proposal in Brief

                // tag::brief[]
                %s

                // end::brief[]

                The rest of the proposal.
                """.formatted(BRIEF));
        String html = html(dir, """
                = A Book
                :doctype: book
                :toc:

                [abstract]
                --
                include::summary.adoc[tag=brief]
                --

                include::summary.adoc[leveloffset=+1]

                == The problem

                Body text.
                """);

        String moved = TitlePageAbstract.relocate(html);

        assertThat(header(moved))
                .contains("<p>" + BRIEF + "</p>")
                .doesNotContain("The rest of the proposal.");
        assertThat(content(moved))
                .contains("The Proposal in Brief</h2>")
                .contains("<p>" + BRIEF + "</p>")
                .contains("The rest of the proposal.");
    }

    @Test
    void idAndTitleOfTheBlock_areKept() {
        String html = html("""
                = A Book
                :doctype: book

                [abstract#brief]
                .In short
                --
                %s
                --

                == The problem

                See <<brief>>.
                """.formatted(BRIEF));

        String header = header(TitlePageAbstract.relocate(html));

        assertThat(header)
                .contains("<div id=\"brief\" class=\"openblock ike-title-abstract\">")
                .contains("<div class=\"title\">In short</div>");
    }

    @Test
    void headerWithNoContents_takesTheAbstractAtItsEnd() {
        String html = html("""
                = A Book
                Keith E Campbell
                :doctype: book

                [abstract]
                %s

                == The problem

                Body text.
                """.formatted(BRIEF));

        String header = header(TitlePageAbstract.relocate(html));

        // The paragraph, the block's content, the block, then the header.
        assertThat(header).endsWith("""
                <p>%s</p>
                </div>
                </div>
                </div>
                </div>
                """.formatted(BRIEF));
    }

    // ── What stays in the body ───────────────────────────────────────

    @Test
    void blocksAfterTheAbstract_stayInThePreamble() {
        String html = html("""
                = A Book
                :doctype: book
                :toc:

                [abstract]
                %s

                A paragraph before the first section.

                == The problem

                Body text.
                """.formatted(BRIEF));

        String moved = TitlePageAbstract.relocate(html);

        assertThat(header(moved)).contains(BRIEF);
        assertThat(content(moved))
                .startsWith("<div id=\"content\">\n<div id=\"preamble\">\n"
                        + "<div class=\"sectionbody\">\n<div class=\"paragraph\">\n"
                        + "<p>A paragraph before the first section.</p>")
                .doesNotContain(BRIEF);
    }

    @Test
    void preambleLeftWithNothingThatPrints_isTakenOut() {
        String html = html("""
                = A Book
                :doctype: book
                :toc:

                ++++
                <style>
                .note { color: gray; }
                </style>
                ++++

                [abstract]
                %s

                == The problem

                Body text.
                """.formatted(BRIEF));

        String moved = TitlePageAbstract.relocate(html);

        assertThat(header(moved)).contains(BRIEF);
        // A book's preamble starts a page; one that prints nothing would be
        // a blank page 1. The style element is kept in its place.
        assertThat(content(moved))
                .doesNotContain("id=\"preamble\"")
                .startsWith("<div id=\"content\">\n<style>\n.note { color: gray; }\n"
                        + "</style>\n<div class=\"sect1\">");
    }

    @Test
    void contentsPlacedInThePreamble_stayThere() {
        String html = html("""
                = A Book
                :doctype: book
                :toc: preamble

                [abstract]
                %s

                == The problem

                Body text.
                """.formatted(BRIEF));

        String moved = TitlePageAbstract.relocate(html);

        assertThat(header(moved)).contains(BRIEF).doesNotContain("id=\"toc\"");
        assertThat(content(moved))
                .contains("<div id=\"preamble\">")
                .contains("<div id=\"toc\"");
    }

    // ── What is left alone ───────────────────────────────────────────

    @Test
    void documentWithNoAbstract_isReturnedAsItIs() {
        String html = html("""
                = A Book
                :doctype: book
                :toc:

                A paragraph before the first section.

                == The problem

                [quote]
                A quotation is not an abstract.
                """);

        assertThat(TitlePageAbstract.relocate(html)).isSameAs(html);
    }

    @Test
    void abstractWithNothingAfterIt_isLeftWhereItIs() {
        String html = html("""
                = An Article

                [abstract]
                %s
                """.formatted(BRIEF));

        assertThat(html).contains("quoteblock abstract");
        assertThat(TitlePageAbstract.relocate(html)).isSameAs(html);
    }

    @Test
    void abstractInsideASection_isLeftWhereItIs() {
        String html = html("""
                = A Book
                :doctype: book

                == The problem

                [abstract]
                %s

                Body text.
                """.formatted(BRIEF));

        assertThat(html).contains("quoteblock abstract");
        assertThat(TitlePageAbstract.relocate(html)).isSameAs(html);
    }

    @Test
    void abstractInsideAnotherBlock_isLeftWhereItIs() {
        String html = html("""
                = A Book
                :doctype: book

                ====
                [abstract]
                %s
                ====

                == The problem

                Body text.
                """.formatted(BRIEF));

        assertThat(html).contains("quoteblock abstract");
        assertThat(TitlePageAbstract.relocate(html)).isSameAs(html);
    }

    @Test
    void onlyTheFirstAbstract_isMoved() {
        String html = html("""
                = A Book
                :doctype: book

                [abstract]
                %s

                [abstract]
                A second block with the same style.

                == The problem

                Body text.
                """.formatted(BRIEF));

        String moved = TitlePageAbstract.relocate(html);

        assertThat(header(moved))
                .contains(BRIEF)
                .doesNotContain("A second block with the same style.");
        assertThat(content(moved))
                .contains("quoteblock abstract")
                .contains("A second block with the same style.");
    }

    @Test
    void documentWithNoTitle_isReturnedAsItIs() {
        String html = html("""
                [abstract]
                %s

                == The problem

                Body text.
                """.formatted(BRIEF));

        assertThat(TitlePageAbstract.relocate(html)).isSameAs(html);
    }

    @Test
    void secondPass_changesNothing() {
        String html = html("""
                = A Book
                :doctype: book
                :toc:

                [abstract]
                %s

                == The problem

                Body text.
                """.formatted(BRIEF));

        String moved = TitlePageAbstract.relocate(html);

        assertThat(moved).isNotEqualTo(html);
        assertThat(TitlePageAbstract.relocate(moved)).isSameAs(moved);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String html(String adoc) {
        return html(null, adoc);
    }

    /** Convert a document with the {@code html5} backend, as a whole page. */
    private static String html(Path baseDir, String adoc) {
        var options = Options.builder()
                .backend("html5")
                .standalone(true)
                .safe(SafeMode.UNSAFE);
        if (baseDir != null) {
            options.baseDir(baseDir.toFile());
        }
        return asciidoctor.convert(adoc, options.build());
    }

    /** The header of a page, from its opening tag to the content's. */
    private static String header(String html) {
        return html.substring(html.indexOf("<div id=\"header\">"),
                html.indexOf("<div id=\"content\">"));
    }

    /** The content of a page, from its opening tag to the footer's. */
    private static String content(String html) {
        return html.substring(html.indexOf("<div id=\"content\">"),
                html.indexOf("<div id=\"footer\">"));
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + 1)) {
            count++;
        }
        return count;
    }
}
