package network.ike.docs.ingest;

/// Value type carrying the three IKE-INGEST §"External Source
/// Ingestion" provenance attributes that every externally-sourced
/// topic fragment must declare.
///
/// @param provenance value for `:topic-provenance:` — typically
///                   `external` for ingested content
/// @param citation   full bibliographic citation for the upstream
///                   source: publisher, title, version, URL
/// @param license    one of the standard IKE-INGEST license strings,
///                   e.g. `Fair use summary of copyrighted work —
///                   not for redistribution.` for HL7 IGs and other
///                   copyrighted standards
public record ProvenanceAttributes(
        String provenance,
        String citation,
        String license) {

    /// Convenience factory for the most common case: an external
    /// source under fair-use summary treatment (HL7 IGs, ISO standards,
    /// journal articles, etc., per IKE-INGEST §"Standards" /
    /// §"Literature" rules).
    public static ProvenanceAttributes externalFairUse(String citation) {
        return new ProvenanceAttributes(
                "external",
                citation,
                "Fair use summary of copyrighted work — not for redistribution.");
    }

    /// Convenience factory for US federal regulatory works (public
    /// domain), per IKE-INGEST §"Regulatory".
    public static ProvenanceAttributes externalFederalPublicDomain(String citation) {
        return new ProvenanceAttributes(
                "external",
                citation,
                "Public domain — US federal government work.");
    }

    /// Convenience factory for internal collaborator content, per
    /// IKE-INGEST §"Internal".
    public static ProvenanceAttributes externalInternal(String citation) {
        return new ProvenanceAttributes(
                "external",
                citation,
                "Internal use — project collaborator content.");
    }
}
