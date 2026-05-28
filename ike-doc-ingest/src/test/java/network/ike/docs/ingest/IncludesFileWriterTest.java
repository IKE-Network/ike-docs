package network.ike.docs.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncludesFileWriterTest {

    private final IncludesFileWriter writer = new IncludesFileWriter();

    @Test
    void render_emitsHeaderAndSections() {
        IncludesFile f = new IncludesFile(
                "// Auto-generated.\n// Do not hand-edit.",
                List.of(
                        new IncludesFile.IncludesSection("Profiles",
                                List.of("profiles/us-core-patient.adoc",
                                        "profiles/us-core-observation.adoc")),
                        new IncludesFile.IncludesSection("Value Sets",
                                List.of("valuesets/condition-code.adoc"))));

        String out = writer.render(f);

        assertThat(out)
                .startsWith("// Auto-generated.\n// Do not hand-edit.\n")
                .contains("== Profiles\n\n")
                .contains("include::profiles/us-core-patient.adoc[leveloffset=+1]")
                .contains("include::profiles/us-core-observation.adoc[leveloffset=+1]")
                .contains("== Value Sets\n\n")
                .contains("include::valuesets/condition-code.adoc[leveloffset=+1]");
    }

    @Test
    void render_skipsEmptySections() {
        IncludesFile f = new IncludesFile(
                "",
                List.of(
                        new IncludesFile.IncludesSection("Empty", List.of()),
                        new IncludesFile.IncludesSection("Present",
                                List.of("a.adoc"))));

        String out = writer.render(f);

        assertThat(out).doesNotContain("== Empty");
        assertThat(out).contains("== Present");
    }
}
