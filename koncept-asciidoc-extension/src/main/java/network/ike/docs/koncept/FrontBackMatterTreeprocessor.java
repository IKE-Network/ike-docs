package network.ike.docs.koncept;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.Treeprocessor;

import java.util.Set;

/**
 * Tags level-1 special sections so the print stylesheets can route them to
 * front- or back-matter page schemes instead of the chapter scheme.
 *
 * <p>Under the book doctype an unnumbered {@code [preface]}, {@code [colophon]},
 * {@code [dedication]}, {@code [abstract]} or {@code [acknowledgments]} section,
 * and a {@code [appendix]}, {@code [glossary]}, {@code [bibliography]} or
 * {@code [index]} section, all render as a bare {@code <div class="sect1">} —
 * indistinguishable in CSS from a numbered chapter. This treeprocessor adds the
 * role {@code front-matter} or {@code back-matter}, which the HTML5 backend
 * emits as an extra class ({@code <div class="sect1 front-matter">}), so the
 * loose-leaf overlay can exclude them from chapter numbering (keeping composite
 * folios aligned with the document's section numbers) and give them their own
 * folio scheme.
 *
 * <p>Registered via the SPI ({@link KonceptExtensionRegistry}) so the
 * {@code asciidoctor-maven-plugin} auto-discovers it for every
 * {@code process-asciidoc} render. Operates on the parsed AST only, so it is
 * backend-agnostic and safe for every backend, including Prawn PDF.
 */
public final class FrontBackMatterTreeprocessor extends Treeprocessor {

    private static final Set<String> FRONT_MATTER = Set.of(
            "preface", "colophon", "dedication", "abstract", "acknowledgments");
    private static final Set<String> BACK_MATTER = Set.of(
            "appendix", "glossary", "bibliography", "index");

    /** Creates the treeprocessor. */
    public FrontBackMatterTreeprocessor() {
    }

    /**
     * Add a {@code front-matter}/{@code back-matter} role to level-1 special
     * sections.
     *
     * @param document the parsed document
     * @return the same document, with roles added to special sections
     */
    @Override
    public Document process(Document document) {
        for (StructuralNode node : document.getBlocks()) {
            if (node instanceof Section section && section.getLevel() == 1) {
                String kind = sectionKind(section);
                if (FRONT_MATTER.contains(kind)) {
                    section.addRole("front-matter");
                } else if (BACK_MATTER.contains(kind)) {
                    section.addRole("back-matter");
                }
            }
        }
        return document;
    }

    /**
     * Classify a section by its explicit style ({@code [preface]} etc.),
     * falling back to its sectname.
     *
     * @param section the level-1 section
     * @return the lowercase classifier, or an empty string if unknown
     */
    private static String sectionKind(Section section) {
        String style = section.getStyle();
        if (style != null && !style.isBlank()) {
            return style;
        }
        String name = section.getSectionName();
        return name != null ? name : "";
    }
}
