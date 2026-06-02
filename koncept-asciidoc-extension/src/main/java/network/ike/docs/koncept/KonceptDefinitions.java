package network.ike.docs.koncept;

import org.asciidoctor.ast.Document;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolves and caches the {@link KonceptDefinitionSource} for a document.
 * <p>
 * The pipeline's classpath {@code /koncepts.yml} is always loaded as the
 * base; a project-local YAML named by the {@code :koncept-definitions-file:}
 * attribute is layered on top (overriding by identifier). The classpath
 * resource may be overridden with {@code :koncept-definitions-classpath:}.
 * <p>
 * Both {@link KonceptInlineMacro} (once per reference, during conversion)
 * and {@link KonceptGlossaryProcessor} (once, at the end) need the same
 * source, so it is parsed once and cached per {@link Document} in a
 * {@link WeakHashMap} keyed by document identity.
 */
final class KonceptDefinitions {

    private KonceptDefinitions() {
    }

    private static final String DEFAULT_CLASSPATH_RESOURCE = "/koncepts.yml";
    private static final String ATTR_DEFS_FILE = "koncept-definitions-file";
    private static final String ATTR_DEFS_CLASSPATH = "koncept-definitions-classpath";

    private static final Map<Document, KonceptDefinitionSource> CACHE = new WeakHashMap<>();

    /**
     * Returns the definition source for a document, parsing and caching it
     * on first access.
     *
     * @param document the AsciiDoc document being converted
     * @return the resolved definition source (never {@code null})
     */
    static synchronized KonceptDefinitionSource forDocument(Document document) {
        return CACHE.computeIfAbsent(document, KonceptDefinitions::resolve);
    }

    /**
     * Builds the definition source from the document's attributes: classpath
     * base plus optional filesystem overlay.
     *
     * @param document the AsciiDoc document being converted
     * @return the resolved definition source (never {@code null})
     */
    private static KonceptDefinitionSource resolve(Document document) {
        Object cpPath = document.getAttribute(ATTR_DEFS_CLASSPATH);
        String classpathResource = (cpPath != null && !cpPath.toString().isBlank())
                ? cpPath.toString()
                : DEFAULT_CLASSPATH_RESOURCE;
        KonceptDefinitionSource base = KonceptDefinitionSource.fromClasspath(classpathResource);

        Object filePath = document.getAttribute(ATTR_DEFS_FILE);
        if (filePath != null && !filePath.toString().isBlank()) {
            KonceptDefinitionSource overlay = KonceptDefinitionSource.fromFile(filePath.toString());
            return new CompositeKonceptDefinitionSource(List.of(overlay, base));
        }
        return base;
    }
}
