/// Reusable infrastructure for ingesting external content into IKE
/// documentation corpus projects.
///
/// Source-specific parsers (FHIR NPM packages, FDA IFU PDFs, ISO
/// standards, etc.) live in each corpus project's own ingest module
/// and consume this library for the cross-cutting concerns:
///
/// - {@link network.ike.docs.ingest.TarballDownloader} — HTTP fetch
///   and TGZ extraction
/// - {@link network.ike.docs.ingest.TopicFragmentWriter} — emit
///   AsciiDoc topic fragments with IKE-INGEST §"External Source
///   Ingestion" provenance attributes
/// - {@link network.ike.docs.ingest.RegistryShardWriter} — emit IKE
///   topic-registry shard YAML per IKE-TOPIC-REGISTRY
/// - {@link network.ike.docs.ingest.IncludesFileWriter} — emit
///   `_includes.adoc` assembly-composition manifests
/// - {@link network.ike.docs.ingest.IngestUtil} — pure helpers
///   (safe slug, YAML escape, truncate, strip markup)
///
/// See IKE-Network/ike-issues#550 for the design proposal that
/// factored this module out of `hl7-ig-corpus-example/ingest/`.
module network.ike.docs.ingest {
    exports network.ike.docs.ingest;

    requires org.apache.commons.compress;
    requires java.net.http;
}
