package network.ike.docs.ingest;

import java.util.List;

/// One entry in an IKE topic-registry shard, per IKE-TOPIC-REGISTRY.
///
/// Optional fields use empty strings or empty lists (not null) by
/// convention; the shard writer omits attributes whose values are
/// empty.
///
/// @param id          unique topic id (matches the TopicFragment's `:topic-id:`)
/// @param file        path to the .adoc file, relative to the topics
///                    library root (e.g. `topics/ext/standards/us-core/profiles/x.adoc`)
/// @param title       human-readable title
/// @param type        IKE topic type (`concept`, `reference`, etc.)
/// @param keywords    list of keywords (mirrors the topic's
///                    `:topic-keywords:`)
/// @param status      topic status (`draft`, `review`, `published`)
/// @param provenance  topic provenance kind (`internal`, `external`,
///                    or empty for default)
/// @param canonical   the upstream canonical URL for external topics,
///                    empty if none
/// @param resourceType for FHIR-resource-typed entries, the
///                    `resourceType` value (e.g. `StructureDefinition`),
///                    empty for non-FHIR sources
/// @param dependencies topic ids this topic depends on
/// @param related     related topic ids
/// @param summary     one-paragraph summary
public record RegistryEntry(
        String id,
        String file,
        String title,
        String type,
        List<String> keywords,
        String status,
        String provenance,
        String canonical,
        String resourceType,
        List<String> dependencies,
        List<String> related,
        String summary) {
}
