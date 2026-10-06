package network.ike.docs.ingest;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void render_writesAttributesInOrderAfterKeywords() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("summary", "Describes the profile.");
        attrs.put("related", "ext-a, ext-b");
        attrs.put("notes", "Generated.");
        attrs.put("scope-note", "   ");
        TopicFragment f = new TopicFragment("ext-x", "X", "reference", "review",
                List.of("k1", "k2", "k3"), null, "body\n", attrs);

        String out = writer.render(f);

        assertThat(out).contains(":topic-keywords: k1, k2, k3\n"
                + ":topic-summary: Describes the profile.\n"
                + ":topic-related: ext-a, ext-b\n"
                + ":topic-notes: Generated.\n\n[[ext-x]]");
        assertThat(out).doesNotContain(":topic-scope-note:");
    }

    @Test
    void render_wrapsLongAttributeValuesWithContinuations() {
        String longSummary = ("word ".repeat(60)).strip() + "\nwith a newline";
        TopicFragment f = new TopicFragment("id", "T", "concept", "draft",
                List.of("k"), null, "body", Map.of("summary", longSummary));

        String header = writer.render(f).split("\n\n")[0];
        List<String> lines = header.lines()
                .dropWhile(l -> !l.startsWith(":topic-summary:")).toList();

        assertThat(lines).hasSizeGreaterThan(2);
        assertThat(lines.subList(0, lines.size() - 1)).allMatch(l -> l.endsWith(" \\"));
        assertThat(lines.subList(1, lines.size())).allMatch(l -> l.startsWith("  ") && !l.startsWith("   "));
        assertThat(lines).allMatch(l -> l.length() <= TopicFragmentWriter.WRAP + 2);
        String joined = String.join(" ", lines.stream()
                .map(l -> l.replace(":topic-summary:", "").replaceAll(" \\\\$", "").strip()).toList());
        assertThat(joined).isEqualTo(longSummary.replace("\n", " "));
    }

    @Test
    void attributes_rejectReservedAndMalformedKeys() {
        assertThatThrownBy(() -> new TopicFragment("id", "T", "concept", "draft",
                List.of(), null, "b", Map.of("status", "review")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("status");
        assertThatThrownBy(() -> new TopicFragment("id", "T", "concept", "draft",
                List.of(), null, "b", Map.of("topic-summary", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TopicFragment("id", "T", "concept", "draft",
                List.of(), null, "b", Map.of("Summary", "x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void attributes_defaultToEmptyAndAreImmutable() {
        TopicFragment f = new TopicFragment("id", "T", "concept", "draft", List.of(), null, "b");
        assertThat(f.attributes()).isEmpty();
        Map<String, String> source = new LinkedHashMap<>(Map.of("notes", "n"));
        TopicFragment g = new TopicFragment("id", "T", "concept", "draft", List.of(), null, "b", source);
        source.put("summary", "late");
        assertThat(g.attributes()).containsOnlyKeys("notes");
        assertThatThrownBy(() -> g.attributes().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
