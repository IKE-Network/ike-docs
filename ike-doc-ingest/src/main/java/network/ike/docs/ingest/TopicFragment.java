package network.ike.docs.ingest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/// Value type for one ingested topic fragment, before serialization.
///
/// The fragment's structural content (sections, tables, etc.) is
/// carried as a pre-rendered AsciiDoc {@code body} string; corpus
/// ingesters are responsible for generating that body in whatever
/// form their source-specific semantics demand. The library handles
/// the standard header — id, type, status, keywords, provenance
/// attributes, any further `:topic-*:` attributes, the anchor block,
/// and the H1.
///
/// @param topicId    unique topic identifier across the corpus,
///                   e.g. `ext-standards-us-core-profiles-us-core-patient`
/// @param title      human-readable title rendered in the H1
/// @param type       IKE-ASCIIDOC-FRAGMENT topic type: `concept`,
///                   `reference`, `procedure`, `dialog`
/// @param status     IKE-ASCIIDOC-FRAGMENT topic status: `draft`,
///                   `review`, `published`
/// @param keywords   list of topic keywords (rendered as a comma-
///                   separated `:topic-keywords:` value)
/// @param provenance the IKE-INGEST provenance attribute triplet
///                   for externally-sourced topics; may be null for
///                   internally-authored fragments
/// @param body       the post-header AsciiDoc content — tables,
///                   sections, prose, etc. — that follows the H1
/// @param attributes further header attributes, in the order given,
///                   keyed without the `topic-` prefix: e.g.
///                   `summary`, `notes`, `related`, `dependencies`,
///                   `scope-note`, `supersedes` (IKE-TOPIC-REGISTRY
///                   § "Topic Header Attributes"). Each renders as
///                   `:topic-<key>: <value>`. Keys the record already
///                   carries (`id`, `type`, `status`, `keywords`,
///                   `provenance`, `citation`, `license`) are rejected.
///                   May be null or empty.
public record TopicFragment(
        String topicId,
        String title,
        String type,
        String status,
        List<String> keywords,
        ProvenanceAttributes provenance,
        String body,
        Map<String, String> attributes) {

    /// Keys written from the record's own components.
    static final Set<String> RESERVED =
            Set.of("id", "type", "status", "keywords", "provenance", "citation", "license");

    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");

    /// Validate and freeze the attributes, keeping their order.
    ///
    /// @throws IllegalArgumentException for a key that is reserved or
    ///         not lowercase kebab-case
    public TopicFragment {
        if (attributes == null || attributes.isEmpty()) {
            attributes = Map.of();
        } else {
            for (String key : attributes.keySet()) {
                if (key == null || !KEY.matcher(key).matches() || key.startsWith("topic-")) {
                    throw new IllegalArgumentException("attribute key must be lowercase kebab-case "
                            + "without the topic- prefix: " + key);
                }
                if (RESERVED.contains(key)) {
                    throw new IllegalArgumentException("attribute '" + key
                            + "' is set through its own TopicFragment component");
                }
            }
            attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        }
    }

    /// A fragment with no further header attributes.
    ///
    /// @param topicId    unique topic identifier
    /// @param title      human-readable title
    /// @param type       topic type
    /// @param status     topic status
    /// @param keywords   topic keywords
    /// @param provenance provenance triplet, or null
    /// @param body       the post-header AsciiDoc content
    public TopicFragment(String topicId, String title, String type, String status,
                         List<String> keywords, ProvenanceAttributes provenance, String body) {
        this(topicId, title, type, status, keywords, provenance, body, Map.of());
    }
}
