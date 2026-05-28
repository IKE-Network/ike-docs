package network.ike.docs.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TopicFragmentWriterTest {

    private final TopicFragmentWriter writer = new TopicFragmentWriter();

    @Test
    void render_includesStandardHeader() {
        TopicFragment f = new TopicFragment(
                "ext-standards-us-core-profiles-us-core-patient",
                "US Core Patient Profile",
                "reference",
                "review",
                List.of("us-core", "structuredefinition", "us-core-patient"),
                ProvenanceAttributes.externalFairUse(
                        "HL7 International. US Core IG, version 8.0.1. http://hl7.org/fhir/us/core."),
                "Body content here.\n");

        String out = writer.render(f);

        assertThat(out)
                .contains(":topic-id: ext-standards-us-core-profiles-us-core-patient")
                .contains(":topic-type: reference")
                .contains(":topic-status: review")
                .contains(":topic-provenance: external")
                .contains(":topic-citation: HL7 International. US Core IG, version 8.0.1.")
                .contains(":topic-license: Fair use summary of copyrighted work — not for redistribution.")
                .contains(":topic-keywords: us-core, structuredefinition, us-core-patient")
                .contains("[[ext-standards-us-core-profiles-us-core-patient]]")
                .contains("= US Core Patient Profile")
                .contains("Body content here.");
    }

    @Test
    void render_omitsProvenanceWhenNull() {
        TopicFragment f = new TopicFragment(
                "internal-corpus-overview", "Internal Topic",
                "concept", "draft",
                List.of("internal"),
                null,
                "body\n");

        String out = writer.render(f);

        assertThat(out).doesNotContain(":topic-provenance:");
        assertThat(out).doesNotContain(":topic-citation:");
        assertThat(out).doesNotContain(":topic-license:");
    }

    @Test
    void render_omitsKeywordsWhenEmpty() {
        TopicFragment f = new TopicFragment(
                "id", "Title", "concept", "draft", List.of(), null, "body");
        assertThat(writer.render(f)).doesNotContain(":topic-keywords:");
    }

    @Test
    void render_flattensTitleNewlines() {
        TopicFragment f = new TopicFragment(
                "id", "Multi\nline\ntitle", "concept", "draft",
                List.of(), null, "body");
        String out = writer.render(f);

        assertThat(out).contains("// Topic: Multi line title");
        assertThat(out).contains("= Multi line title");
    }
}
