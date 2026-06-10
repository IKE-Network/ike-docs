package network.ike.docs.plugin.diff;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RegistryIndex} YAML-shape parsing — the
 * indented-fragment per-domain form and nested assembly sections.
 */
class RegistryIndexTest {

    @Test
    void topicsOf_readsIndentedDomainFragment() {
        Object root = RegistryIndex.parse("""
                  - id: arch
                    title: "System Architecture"
                    topics:
                      - id: arch-one
                        file: topics/arch/one.adoc
                        title: "One"
                      - id: arch-two
                        file: topics/arch/two.adoc
                        title: "Two"
                """);
        Map<String, Map<String, Object>> topics = RegistryIndex.topicsOf(root);
        assertThat(topics).containsKeys("arch-one", "arch-two");
        assertThat(topics.get("arch-two").get("file")).isEqualTo("topics/arch/two.adoc");
    }

    @Test
    void assemblyRefsOf_flattensNestedSections() {
        Object root = RegistryIndex.parse("""
                  - id: arch-guide
                    file: arch-guide.adoc
                    sections:
                      - heading: "Overview"
                        topic-refs: [arch-one]
                      - heading: "Foundations"
                        sections:
                          - heading: "Inner"
                            topic-refs: [arch-two, arch-three]
                """);
        Map<String, List<String>> refs = RegistryIndex.assemblyRefsOf(root);
        assertThat(refs.get("arch-guide"))
                .containsExactly("arch-one", "arch-two", "arch-three");
    }

    @Test
    void parse_handlesAbsentInput() {
        assertThat(RegistryIndex.parse(null)).isNull();
        assertThat(RegistryIndex.parse("  ")).isNull();
    }
}
