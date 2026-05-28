package network.ike.docs.ingest;

import java.util.List;

/// A per-source IKE topic-registry shard, per IKE-TOPIC-REGISTRY.
///
/// Each shard catalogs every topic emitted from one upstream source.
/// The shard file is typically named
/// `topic-registry/{shardId}.yaml` and is referenced from the corpus's
/// root `topic-registry.yaml`.
///
/// @param shardId       short identifier — kebab-case, used as the
///                      top-level YAML key (e.g.
///                      `ext-standards-us-core`)
/// @param title         human-readable title for the shard
/// @param description   prose describing what the shard catalogs;
///                      typically includes the source citation
/// @param entries       the topic entries in the shard, in stable
///                      sort order
public record RegistryShard(
        String shardId,
        String title,
        String description,
        List<RegistryEntry> entries) {
}
