package network.ike.docs.koncept;

import java.util.List;
import java.util.Optional;

/**
 * A definition source that chains multiple sources in priority order.
 * <p>
 * Lookup queries each source in order and returns the first match.
 * Sources listed earlier take priority, so a project-local file
 * listed first will override definitions from the pipeline classpath.
 */
public class CompositeKonceptDefinitionSource implements KonceptDefinitionSource {

    private final List<KonceptDefinitionSource> sources;

    /**
     * Creates a composite source that queries the given sources in order.
     *
     * @param sources ordered list of definition sources (earlier entries take priority)
     */
    public CompositeKonceptDefinitionSource(List<KonceptDefinitionSource> sources) {
        this.sources = List.copyOf(sources);
    }

    @Override
    public Optional<KonceptDefinition> lookup(String identifier) {
        for (KonceptDefinitionSource source : sources) {
            Optional<KonceptDefinition> result = source.lookup(identifier);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
