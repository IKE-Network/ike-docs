package network.ike.docs.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Serialize {@link RegistryShard} value objects to YAML files per
/// IKE-TOPIC-REGISTRY.
///
/// The emitted YAML uses two-space indentation and emits only the
/// fields a registry consumer needs. Fields whose value is empty
/// (empty string or empty list) are skipped to keep the file readable.
public final class RegistryShardWriter {

    /// Write the shard to {@code target}, creating parent directories
    /// as needed.
    ///
    /// @param target the file path to write
    /// @param shard the shard content
    /// @throws IOException on I/O failure
    public void write(Path target, RegistryShard shard) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, render(shard));
    }

    /// Render the shard as a YAML string without writing it.
    public String render(RegistryShard shard) {
        StringBuilder sb = new StringBuilder(8192);

        sb.append("# ").append(shard.shardId()).append(" — IKE topic-registry shard\n");
        sb.append("\n");
        sb.append("  - id: ").append(shard.shardId()).append("\n");
        sb.append("    title: \"").append(IngestUtil.yamlEscape(shard.title())).append("\"\n");

        if (shard.description() != null && !shard.description().isBlank()) {
            sb.append("    description: >\n");
            for (String line : shard.description().split("\n")) {
                sb.append("      ").append(IngestUtil.yamlEscape(line)).append("\n");
            }
        }

        sb.append("    topics:\n");

        for (RegistryEntry e : shard.entries()) {
            sb.append("\n");
            sb.append("      - id: ").append(e.id()).append("\n");
            sb.append("        file: ").append(e.file()).append("\n");
            sb.append("        title: \"").append(IngestUtil.yamlEscape(e.title())).append("\"\n");
            sb.append("        type: ").append(e.type()).append("\n");

            if (e.keywords() != null && !e.keywords().isEmpty()) {
                sb.append("        keywords: [").append(String.join(", ", e.keywords())).append("]\n");
            }
            sb.append("        status: ").append(e.status()).append("\n");

            if (e.provenance() != null && !e.provenance().isEmpty()) {
                sb.append("        provenance: ").append(e.provenance()).append("\n");
            }
            if (e.canonical() != null && !e.canonical().isEmpty()) {
                sb.append("        canonical: ").append(e.canonical()).append("\n");
            }
            if (e.resourceType() != null && !e.resourceType().isEmpty()) {
                sb.append("        resource-type: ").append(e.resourceType()).append("\n");
            }

            sb.append("        dependencies: [")
              .append(e.dependencies() == null ? "" : String.join(", ", e.dependencies()))
              .append("]\n");
            sb.append("        related: [")
              .append(e.related() == null ? "" : String.join(", ", e.related()))
              .append("]\n");

            if (e.summary() != null && !e.summary().isBlank()) {
                sb.append("        summary: >\n");
                sb.append("          ").append(IngestUtil.yamlEscape(IngestUtil.truncate(e.summary(), 400))).append("\n");
            }
        }

        return sb.toString();
    }
}
