package network.ike.docs.koncept;

import java.util.Optional;

/**
 * Strategy interface for resolving Koncept definitions.
 * <p>
 * Implementations may source definitions from:
 * <ul>
 *   <li>YAML files on the classpath or filesystem</li>
 *   <li>OWL ontology files via OWL API</li>
 *   <li>IKE knowledge graph REST endpoints</li>
 *   <li>SNOMED CT terminology servers (FHIR or native)</li>
 * </ul>
 */
public interface KonceptDefinitionSource {

    /**
     * Look up the definition for the given koncept identifier.
     *
     * @param identifier CamelCase koncept identifier (e.g., "HeartFailure")
     * @return the definition if found
     */
    Optional<KonceptDefinition> lookup(String identifier);

    /**
     * All identifiers this source can enumerate, for a comprehensive glossary that
     * lists every known koncept (not only those referenced in the document) and for
     * inverting the {@code broader} taxonomy into children. Sources that cannot
     * enumerate (for example a live terminology server queried on demand) return an
     * empty collection, and the glossary falls back to referenced koncepts only.
     *
     * @return every known identifier, or an empty collection if enumeration is unsupported
     */
    default java.util.Collection<String> identifiers() {
        return java.util.List.of();
    }

    /**
     * Create a source backed by a YAML file on the classpath.
     *
     * @param classpathResource resource path (e.g., "/koncepts.yml")
     * @return a YAML-backed definition source
     */
    static KonceptDefinitionSource fromClasspath(String classpathResource) {
        return YamlKonceptDefinitionSource.fromClasspath(classpathResource);
    }

    /**
     * Create a source backed by a YAML file on the filesystem.
     *
     * @param filePath absolute or relative filesystem path
     * @return a YAML-backed definition source
     */
    static KonceptDefinitionSource fromFile(String filePath) {
        return YamlKonceptDefinitionSource.fromFile(filePath);
    }
}
