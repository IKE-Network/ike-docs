package network.ike.docs.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryShardWriterTest {

    private final RegistryShardWriter writer = new RegistryShardWriter();

    @Test
    void render_emitsShardHeaderAndEntries() {
        RegistryEntry e = new RegistryEntry(
                "ext-standards-us-core-profiles-us-core-patient",
                "topics/ext/standards/us-core/profiles/us-core-patient.adoc",
                "US Core Patient Profile",
                "reference",
                List.of("us-core", "structuredefinition", "us-core-patient"),
                "review",
                "external",
                "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient",
                "StructureDefinition",
                List.of(),
                List.of(),
                "Profile for US Core Patient.");

        RegistryShard shard = new RegistryShard(
                "ext-standards-us-core",
                "US Core",
                "HL7 US Core IG, version 8.0.1.",
                List.of(e));

        String out = writer.render(shard);

        assertThat(out)
                .contains("- id: ext-standards-us-core")
                .contains("title: \"US Core\"")
                .contains("description: >")
                .contains("topics:")
                .contains("- id: ext-standards-us-core-profiles-us-core-patient")
                .contains("file: topics/ext/standards/us-core/profiles/us-core-patient.adoc")
                .contains("type: reference")
                .contains("keywords: [us-core, structuredefinition, us-core-patient]")
                .contains("status: review")
                .contains("provenance: external")
                .contains("canonical: http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient")
                .contains("resource-type: StructureDefinition")
                .contains("summary: >")
                .contains("Profile for US Core Patient.");
    }

    @Test
    void render_omitsEmptyOptionalFields() {
        RegistryEntry e = new RegistryEntry(
                "id", "file.adoc", "Title",
                "concept", List.of(), "draft",
                "", "", "",
                List.of(), List.of(), "");
        RegistryShard shard = new RegistryShard("test", "Test", "", List.of(e));

        String out = writer.render(shard);

        assertThat(out).doesNotContain("provenance:");
        assertThat(out).doesNotContain("canonical:");
        assertThat(out).doesNotContain("resource-type:");
        assertThat(out).doesNotContain("summary:");
        assertThat(out).doesNotContain("keywords:");
    }
}
