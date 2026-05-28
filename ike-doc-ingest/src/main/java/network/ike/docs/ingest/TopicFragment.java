package network.ike.docs.ingest;

import java.util.List;

/// Value type for one ingested topic fragment, before serialization.
///
/// The fragment's structural content (sections, tables, etc.) is
/// carried as a pre-rendered AsciiDoc {@code body} string; corpus
/// ingesters are responsible for generating that body in whatever
/// form their source-specific semantics demand. The library handles
/// the standard header — id, type, status, keywords, provenance
/// attributes, the anchor block, and the H1.
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
public record TopicFragment(
        String topicId,
        String title,
        String type,
        String status,
        List<String> keywords,
        ProvenanceAttributes provenance,
        String body) {
}
