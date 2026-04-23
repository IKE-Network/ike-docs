package network.ike.docs.koncept;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads Koncept definitions from a YAML file.
 * <p>
 * Expected YAML structure:
 * <pre>
 * HeartFailure:
 *   label: Heart Failure
 *   definition: >
 *     A clinical syndrome characterized by the heart's inability
 *     to pump sufficient blood to meet metabolic demands.
 *   axiom: "≡ ClinicalSyndrome ⊓ ∃hasPathology.(InsufficientCardiacOutput)"
 *   sctid: "84114007"
 *   iri: "http://snomed.info/id/84114007"
 *
 * AorticStenosis:
 *   label: Aortic Stenosis
 *   definition: Narrowing of the aortic valve orifice.
 *   axiom: "≡ StenosisOfValve ⊓ ∃hasMorphology.Stenosis ⊓ ∃findingSite.AorticValve"
 *   sctid: "60573004"
 * </pre>
 */
public class YamlKonceptDefinitionSource implements KonceptDefinitionSource {

    private static final Logger LOG = LoggerFactory.getLogger(YamlKonceptDefinitionSource.class);

    private final Map<String, KonceptDefinition> definitions;

    private YamlKonceptDefinitionSource(Map<String, KonceptDefinition> definitions) {
        this.definitions = new ConcurrentHashMap<>(definitions);
    }

    @Override
    public Optional<KonceptDefinition> lookup(String identifier) {
        return Optional.ofNullable(definitions.get(identifier));
    }

    /**
     * Loads definitions from a classpath resource.
     *
     * @param resource the classpath resource path (e.g., {@code /koncepts.yml})
     * @return a definition source backed by the parsed YAML, or an empty source if not found
     */
    public static YamlKonceptDefinitionSource fromClasspath(String resource) {
        InputStream is = YamlKonceptDefinitionSource.class.getResourceAsStream(resource);
        if (is == null) {
            LOG.debug("Koncept definitions not found on classpath: {}", resource);
            return new YamlKonceptDefinitionSource(Map.of());
        }
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            LOG.error("Failed to read koncept definitions from classpath: {}", resource, e);
            return new YamlKonceptDefinitionSource(Map.of());
        }
    }

    /**
     * Loads definitions from a filesystem path.
     *
     * @param filePath the absolute or relative path to the YAML file
     * @return a definition source backed by the parsed YAML, or an empty source if not found
     */
    public static YamlKonceptDefinitionSource fromFile(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            LOG.warn("Koncept definitions file not found: {}", filePath);
            return new YamlKonceptDefinitionSource(Map.of());
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            LOG.error("Failed to read koncept definitions from file: {}", filePath, e);
            return new YamlKonceptDefinitionSource(Map.of());
        }
    }

    @SuppressWarnings("unchecked")
    private static YamlKonceptDefinitionSource parse(Reader reader) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(reader);
        if (root == null) {
            return new YamlKonceptDefinitionSource(Map.of());
        }

        Map<String, KonceptDefinition> defs = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String identifier = entry.getKey();
            if (!(entry.getValue() instanceof Map)) {
                LOG.warn("Skipping malformed koncept entry: {}", identifier);
                continue;
            }

            Map<String, Object> fields = (Map<String, Object>) entry.getValue();
            KonceptDefinition def = KonceptDefinition.builder()
                    .identifier(identifier)
                    .label(stringField(fields, "label"))
                    .definition(stringField(fields, "definition"))
                    .axiom(stringField(fields, "axiom"))
                    .sctid(stringField(fields, "sctid"))
                    .iri(stringField(fields, "iri"))
                    .build();

            defs.put(identifier, def);
        }

        LOG.debug("Loaded {} koncept definitions", defs.size());
        return new YamlKonceptDefinitionSource(defs);
    }

    private static String stringField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().strip() : null;
    }
}
