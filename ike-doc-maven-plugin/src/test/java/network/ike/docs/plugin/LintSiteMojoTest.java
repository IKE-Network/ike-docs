package network.ike.docs.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LintSiteMojo#lint(String, String, String, String)}.
 *
 * <p>Pure-function tests — no I/O. The execute() method's
 * file-reading + MojoException wrapping is thin and exercised by
 * an end-to-end build at integration time.
 */
class LintSiteMojoTest {

    private static final String DEFAULT_GROUP = "org.sentrysoftware.maven";
    private static final String DEFAULT_ARTIFACT = "sentry-maven-skin";

    // ── expectedBodyClassFor ─────────────────────────────────────

    @Test
    void expectedBodyClassFor_ikeNetwork_returnsGreen() {
        assertThat(LintSiteMojo.expectedBodyClassFor("ike-network"))
                .isEqualTo("sentry-green");
    }

    @Test
    void expectedBodyClassFor_knowledgeDesign_returnsPurple() {
        assertThat(LintSiteMojo.expectedBodyClassFor("knowledge-design"))
                .isEqualTo("sentry-purple");
    }

    @Test
    void expectedBodyClassFor_ikeNetworkExample_returnsOrange() {
        assertThat(LintSiteMojo.expectedBodyClassFor("ike-network-example"))
                .isEqualTo("sentry-orange");
    }

    @Test
    void expectedBodyClassFor_unknown_defaultsToGreen() {
        // ike-network is the primary deployment, default to its
        // expected class.
        assertThat(LintSiteMojo.expectedBodyClassFor("nonsense"))
                .isEqualTo("sentry-green");
        assertThat(LintSiteMojo.expectedBodyClassFor(null))
                .isEqualTo("sentry-green");
    }

    // ── lint: bodyClass check ────────────────────────────────────

    @Test
    void lint_correctGreenBodyClass_noProblems() {
        String site = wellFormedSite("sentry-green",
                "ike-tooling", DEFAULT_GROUP, DEFAULT_ARTIFACT);
        assertThat(LintSiteMojo.lint(site, "ike-network",
                DEFAULT_GROUP, DEFAULT_ARTIFACT))
                .isEmpty();
    }

    @Test
    void lint_correctPurpleBodyClass_noProblems() {
        String site = wellFormedSite("sentry-purple",
                "knowledge.design", DEFAULT_GROUP, DEFAULT_ARTIFACT);
        assertThat(LintSiteMojo.lint(site, "knowledge-design",
                DEFAULT_GROUP, DEFAULT_ARTIFACT))
                .isEmpty();
    }

    @Test
    void lint_correctOrangeBodyClass_noProblems() {
        String site = wellFormedSite("sentry-orange",
                "doc-example", DEFAULT_GROUP, DEFAULT_ARTIFACT);
        assertThat(LintSiteMojo.lint(site, "ike-network-example",
                DEFAULT_GROUP, DEFAULT_ARTIFACT))
                .isEmpty();
    }

    @Test
    void lint_greenOnIkeNetworkExample_reportsBodyClassDrift() {
        String site = wellFormedSite("sentry-green",
                "doc-example", DEFAULT_GROUP, DEFAULT_ARTIFACT);
        List<String> problems = LintSiteMojo.lint(site, "ike-network-example",
                DEFAULT_GROUP, DEFAULT_ARTIFACT);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0))
                .contains("<bodyClass>")
                .contains("expected 'sentry-orange'")
                .contains("found 'sentry-green'");
    }

    @Test
    void lint_purpleOnIkeNetwork_reportsBodyClassDrift() {
        String site = wellFormedSite("sentry-purple",
                "ike-tooling", DEFAULT_GROUP, DEFAULT_ARTIFACT);
        List<String> problems = LintSiteMojo.lint(site, "ike-network",
                DEFAULT_GROUP, DEFAULT_ARTIFACT);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0))
                .contains("<bodyClass>")
                .contains("expected 'sentry-green'")
                .contains("found 'sentry-purple'");
    }

    @Test
    void lint_missingBodyClass_reportsAsMissing() {
        // No <bodyClass> element at all (some legacy site.xml omit it).
        String site = """
                <site>
                  <skin>
                    <groupId>org.sentrysoftware.maven</groupId>
                    <artifactId>sentry-maven-skin</artifactId>
                    <version>7.0.00</version>
                  </skin>
                  <body><breadcrumbs/></body>
                </site>
                """;
        List<String> problems = LintSiteMojo.lint(site, "ike-network",
                DEFAULT_GROUP, DEFAULT_ARTIFACT);

        assertThat(problems)
                .anyMatch(p -> p.contains("<bodyClass>")
                        && p.contains("missing"));
    }

    // ── lint: stale breadcrumb deny-list ──────────────────────────

    @Test
    void lint_stalePipelineBreadcrumb_reported() {
        String site = """
                <site>
                  <skin>
                    <groupId>org.sentrysoftware.maven</groupId>
                    <artifactId>sentry-maven-skin</artifactId>
                  </skin>
                  <custom><bodyClass>sentry-green</bodyClass></custom>
                  <body>
                    <breadcrumbs>
                      <item name="IKE Pipeline" href="../index.html"/>
                      <item name="ike-tooling" href="index.html"/>
                    </breadcrumbs>
                  </body>
                </site>
                """;
        List<String> problems = LintSiteMojo.lint(site, "ike-network",
                DEFAULT_GROUP, DEFAULT_ARTIFACT);

        assertThat(problems)
                .anyMatch(p -> p.contains("IKE Pipeline")
                        && p.contains("#216"));
    }

    // ── lint: skin GAV ────────────────────────────────────────────

    @Test
    void lint_wrongSkinGroup_reported() {
        String site = wellFormedSite("sentry-green", "ike-tooling",
                "io.fabric8", DEFAULT_ARTIFACT);
        List<String> problems = LintSiteMojo.lint(site, "ike-network",
                DEFAULT_GROUP, DEFAULT_ARTIFACT);

        assertThat(problems)
                .anyMatch(p -> p.contains("<skin>/<groupId>")
                        && p.contains("expected 'org.sentrysoftware.maven'")
                        && p.contains("found 'io.fabric8'"));
    }

    @Test
    void lint_wrongSkinArtifact_reported() {
        String site = wellFormedSite("sentry-green", "ike-tooling",
                DEFAULT_GROUP, "maven-fluido-skin");
        List<String> problems = LintSiteMojo.lint(site, "ike-network",
                DEFAULT_GROUP, DEFAULT_ARTIFACT);

        assertThat(problems)
                .anyMatch(p -> p.contains("<skin>/<artifactId>")
                        && p.contains("expected 'sentry-maven-skin'")
                        && p.contains("found 'maven-fluido-skin'"));
    }

    @Test
    void lint_multipleProblems_allReported() {
        // Purple-on-ike-network + stale breadcrumb + wrong skin
        // groupId, all in one shot.
        String site = """
                <site>
                  <skin>
                    <groupId>io.fabric8</groupId>
                    <artifactId>sentry-maven-skin</artifactId>
                  </skin>
                  <custom><bodyClass>sentry-purple</bodyClass></custom>
                  <body>
                    <breadcrumbs>
                      <item name="IKE Pipeline" href="../"/>
                    </breadcrumbs>
                  </body>
                </site>
                """;
        List<String> problems = LintSiteMojo.lint(site, "ike-network",
                DEFAULT_GROUP, DEFAULT_ARTIFACT);

        assertThat(problems).hasSize(3);
        assertThat(problems).anyMatch(p -> p.contains("<bodyClass>"));
        assertThat(problems).anyMatch(p -> p.contains("IKE Pipeline"));
        assertThat(problems).anyMatch(p -> p.contains("<skin>/<groupId>"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Construct a minimally-valid site.xml string with the given
     * variables, sufficient to exercise the lint checks.
     */
    private static String wellFormedSite(String bodyClass,
                                          String breadcrumbName,
                                          String skinGroup,
                                          String skinArtifact) {
        return """
                <site>
                  <skin>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>7.0.00</version>
                  </skin>
                  <custom><bodyClass>%s</bodyClass></custom>
                  <body>
                    <breadcrumbs>
                      <item name="%s" href="index.html"/>
                    </breadcrumbs>
                  </body>
                </site>
                """.formatted(skinGroup, skinArtifact,
                bodyClass, breadcrumbName);
    }
}
