package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry;

/**
 * SPI entry point for automatic registration of Koncept extensions
 * with AsciidoctorJ.
 * <p>
 * Registered via {@code META-INF/services/org.asciidoctor.jruby.extension.spi.ExtensionRegistry}.
 * When this JAR is on the classpath of the {@code asciidoctor-maven-plugin},
 * the extensions are activated automatically — no explicit configuration required.
 */
public class KonceptExtensionRegistry implements ExtensionRegistry {

    /** Creates a new extension registry instance. */
    public KonceptExtensionRegistry() {
    }

    @Override
    public void register(Asciidoctor asciidoctor) {
        // Always register the inline macro — works with all backends
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(KonceptInlineMacro.class);

        // The koncept-sigil:kind[] inline macro: a component-kind sigil on its own (amber D,
        // green S, violet P, the grey stamp pentagon, red ?) rendered from the same locked
        // KonceptKind/StampSigilGeometry data as the badges — for prose that talks about the
        // sigil scheme itself (ike-issues#883). Safe for all backends: SVG on the html family,
        // letter-glyph/name text elsewhere.
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(KonceptSigilInlineMacro.class);

        // The [koncept-tree] block: an indented list of Koncept chips — the static projection of
        // the live assistant tree (IKE-Network/ike-issues#827). Renders on the html5 family;
        // declines gracefully to a literal block on other backends. Safe for all backends.
        asciidoctor.javaExtensionRegistry()
                .block(KonceptTreeBlockProcessor.class);

        // The koncept-narrative::Identifier[] block macro: splices a koncept's curated,
        // long-form narrative field into the document as real AsciiDoc source, re-parsed so
        // embedded k: chips resolve normally (IKE-Network/ike-issues#879). Safe for all
        // backends — parseContent is backend-agnostic.
        asciidoctor.javaExtensionRegistry()
                .blockMacro(KonceptNarrativeBlockMacro.class);

        // The koncept-pattern-list::[] block macro: a dedicated "List of Patterns" index —
        // the "List of Tables"/"List of Figures" book convention, styled like the document's
        // own table of contents — one entry per kind: pattern koncept, linking to its full
        // shape in the Koncept Glossary (IKE-Network/ike-issues#880). A plain HTML pass
        // block — safe for the html5 family; renders as an inert raw-HTML passthrough on
        // other backends.
        asciidoctor.javaExtensionRegistry()
                .blockMacro(KonceptPatternListBlockMacro.class);

        // The koncept-pattern-table::Identifier[] block macro: a pattern's shape as a
        // real, captioned/numbered, xref-able AsciiDoc table — one Model Feature
        // (referenced component, then each field) per three-row group, k: badges in the
        // cells (IKE-Network/ike-issues#883). Safe for all backends — parseContent is
        // backend-agnostic, and the emitted source is plain AsciiDoc table markup.
        asciidoctor.javaExtensionRegistry()
                .blockMacro(KonceptPatternTableBlockMacro.class);

        // Tag [preface]/[appendix] etc. with front-/back-matter roles so the
        // loose-leaf print stylesheet can route them (chapter numbering
        // excludes them, keeping composite folios aligned with section
        // numbers). AST-only treeprocessor — safe for all backends, including
        // Prawn. See dev-pdf-loose-leaf-variant; IKE-Network/ike-issues#558.
        asciidoctor.javaExtensionRegistry()
                .treeprocessor(FrontBackMatterTreeprocessor.class);

        // The comprehensive (koncept-glossary-all) glossary, grouped by taxonomy
        // section into real, TOC-visible AST sections (IKE-Network/ike-issues#877).
        // AST-only treeprocessor — safe for every backend; no-ops unless
        // koncept-glossary-all is set, and defers to KonceptGlossaryProcessor for
        // pdf/DocBook (see its own javadoc for why).
        asciidoctor.javaExtensionRegistry()
                .treeprocessor(KonceptGlossaryTreeprocessor.class);

        // The glossary Postprocessor is NOT registered here because it is
        // incompatible with the asciidoctorj-pdf (Prawn) backend. The PDF
        // converter returns a Ruby object instead of a String, causing a
        // TypeError in PostprocessorProxy before our Java code is reached.
        //
        // Instead, the Postprocessor is registered explicitly per-execution
        // in the asciidoctor-maven-plugin configuration via <extensions>:
        //
        //   <extensions>
        //     <extension>
        //       <className>network.ike.docs.koncept.KonceptGlossaryProcessor</className>
        //     </extension>
        //   </extensions>
        //
        // This allows HTML, DocBook, and CSS-based PDF backends to generate
        // the glossary while avoiding the crash with the Prawn PDF backend.
    }
}
