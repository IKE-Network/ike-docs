package network.ike.docs.plugin;

import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lint {@code src/site/site.xml} for IKE Network theme + breadcrumb
 * conventions (ike-issues#319).
 *
 * <p>The shared site assets ({@code site.css}, {@code ike-logo.svg})
 * are consolidated in {@code ike-doc-resources} (#318). The
 * per-project {@code site.xml} legitimately stays local — breadcrumbs
 * and menus genuinely vary — but it carries a small set of
 * conventions that have drifted in the past:
 *
 * <ul>
 *   <li><b>{@code <bodyClass>}</b> must be {@code sentry-green}
 *       (Forest theme) for ike.network foundation consumers,
 *       {@code sentry-orange} (Sunset) for ike.network worked-example
 *       projects, and {@code sentry-purple} (Horizon) for
 *       knowledge.design. The example-vs-foundation split visually
 *       reinforces that examples are not published to Maven Central
 *       and exist to be read and copied rather than depended on (per
 *       #545/#546). The 5 ike-docs submodules shipped with
 *       {@code sentry-purple} after {@code ike-platform} and
 *       {@code ike-tooling} switched to {@code sentry-green}
 *       (per #307), producing a purple banner at
 *       https://ike.network/ike-docs/ike-doc-resources/.</li>
 *   <li><b>Breadcrumb leading item</b> — those same 5 files carried a
 *       stale {@code <item name="IKE Pipeline" href="../index.html"/>}
 *       from before the ike-pipeline → ike-docs/ike-platform repo
 *       split (#216).</li>
 *   <li><b>{@code <skin>} GAV</b> — drift here is silent until you
 *       upgrade.</li>
 * </ul>
 *
 * <p>The ike.network ↔ knowledge.design boundary means a single
 * global enforcement isn't right; the expected {@code bodyClass} is
 * per-deployment. Set the deployment target via the
 * {@code ike.site.deployment} property (defaults to {@code ike-network}).
 *
 * <p>When {@code src/site/site.xml} doesn't exist, this goal is a
 * silent no-op — modules without a custom site descriptor inherit
 * from the parent and don't need linting.
 *
 * <p>Skip with {@code -Dike.skip.lint-site=true}.
 *
 * @see <a href="https://github.com/IKE-Network/ike-issues/issues/319">ike-issues#319</a>
 */
@Mojo(name = "lint-site", defaultPhase = "validate")
public class LintSiteMojo implements org.apache.maven.api.plugin.Mojo {

    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.plugin.Log log;

    /**
     * Access the Maven logger.
     *
     * @return the logger
     */
    protected org.apache.maven.api.plugin.Log getLog() { return log; }

    /**
     * Path to the project's site descriptor. Default is the
     * conventional location; override for non-standard layouts.
     */
    @Parameter(property = "ike.lint-site.path",
               defaultValue = "${project.basedir}/src/site/site.xml")
    File siteXml;

    /**
     * Deployment target — controls the expected {@code <bodyClass>}.
     *
     * <ul>
     *   <li>{@code ike-network} (default) → expects {@code sentry-green}
     *       (foundation libraries published to Maven Central)</li>
     *   <li>{@code ike-network-example} → expects {@code sentry-orange}
     *       (worked-example projects under ike.network, deliberately
     *       not published to Maven Central; read-and-copy rather than
     *       depend on)</li>
     *   <li>{@code knowledge-design} → expects {@code sentry-purple}</li>
     * </ul>
     *
     * Override in worked-example projects with
     * {@code -Dike.site.deployment=ike-network-example} (or set in the
     * POM's {@code <properties>}), and in knowledge.design projects with
     * {@code -Dike.site.deployment=knowledge-design}.
     */
    @Parameter(property = "ike.site.deployment",
               defaultValue = "ike-network")
    String deployment;

    /**
     * Expected {@code <skin>} groupId. Drift here causes silent skin
     * changes when the project's pluginManagement-inherited version
     * is older than the locally-declared one.
     */
    @Parameter(property = "ike.lint-site.skin-group",
               defaultValue = "org.sentrysoftware.maven")
    String expectedSkinGroup;

    /**
     * Expected {@code <skin>} artifactId.
     */
    @Parameter(property = "ike.lint-site.skin-artifact",
               defaultValue = "sentry-maven-skin")
    String expectedSkinArtifact;

    /**
     * Skip linting entirely. Use sparingly — the conventions checked
     * here have all caused production-site drift at least once.
     */
    @Parameter(property = "ike.skip.lint-site", defaultValue = "false")
    boolean skip;

    /** Creates this goal instance. */
    public LintSiteMojo() {}

    @Override
    public void execute() throws MojoException {
        if (skip) {
            getLog().info("idoc:lint-site skipped "
                    + "(-Dike.skip.lint-site=true)");
            return;
        }

        if (!siteXml.isFile()) {
            getLog().debug("idoc:lint-site: no site.xml at "
                    + siteXml + " — module inherits from parent. Skipping.");
            return;
        }

        String content;
        try {
            content = Files.readString(siteXml.toPath(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoException(
                    "Could not read " + siteXml + ": " + e.getMessage(),
                    e);
        }

        List<String> problems = lint(content, deployment,
                expectedSkinGroup, expectedSkinArtifact);

        if (problems.isEmpty()) {
            getLog().debug("idoc:lint-site: " + siteXml
                    + " — all checks passed.");
            return;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("idoc:lint-site found ").append(problems.size())
                .append(" problem(s) in ").append(siteXml).append(":\n");
        for (String p : problems) {
            msg.append("  ✗ ").append(p).append("\n");
        }
        msg.append("\n  Fix the site.xml and re-run, or skip with "
                + "-Dike.skip.lint-site=true (not recommended — these "
                + "conventions have all caused production drift).");
        throw new MojoException(msg.toString());
    }

    /**
     * Apply all site.xml lint rules to the given content. Pure
     * function for testability — does no I/O.
     *
     * @param siteXmlContent     raw site.xml as a string
     * @param deployment         deployment target
     *                           ({@code ike-network} or
     *                           {@code knowledge-design})
     * @param expectedSkinGroup  expected {@code <skin>} groupId
     * @param expectedSkinArtifact expected {@code <skin>} artifactId
     * @return list of problem messages; empty if all checks passed
     */
    public static List<String> lint(String siteXmlContent,
                                     String deployment,
                                     String expectedSkinGroup,
                                     String expectedSkinArtifact) {
        List<String> problems = new ArrayList<>();

        // ── Check 1: <bodyClass> ─────────────────────────────────
        String expectedBodyClass = expectedBodyClassFor(deployment);
        Matcher bodyClassMatcher = Pattern.compile(
                "<bodyClass>\\s*([^<]+?)\\s*</bodyClass>")
                .matcher(siteXmlContent);
        if (bodyClassMatcher.find()) {
            String actual = bodyClassMatcher.group(1).trim();
            if (!actual.equals(expectedBodyClass)) {
                problems.add("<bodyClass>: expected '" + expectedBodyClass
                        + "' (deployment=" + deployment + "), found '"
                        + actual + "'");
            }
        } else {
            problems.add("<bodyClass>: missing — expected '"
                    + expectedBodyClass + "' (deployment="
                    + deployment + ")");
        }

        // ── Check 2: breadcrumb deny-list ─────────────────────────
        // Stale labels left over from repo splits or rebrands. The
        // pattern matches <item name="X" ...> inside the <body> /
        // <breadcrumbs> region but we don't need fine-grained
        // anchoring — any item with one of these names is wrong
        // regardless of location.
        for (String denyName : STALE_BREADCRUMB_NAMES) {
            Pattern denyPattern = Pattern.compile(
                    "<item\\s+[^>]*name=[\"']"
                            + Pattern.quote(denyName) + "[\"']");
            if (denyPattern.matcher(siteXmlContent).find()) {
                problems.add("breadcrumb: stale name '" + denyName
                        + "' (left over from a pre-split layout; "
                        + "see #216 for the ike-pipeline → "
                        + "ike-docs/ike-platform rename)");
            }
        }

        // ── Check 3: <skin> GAV ──────────────────────────────────
        Matcher skinGroupMatcher = Pattern.compile(
                "<skin>\\s*<groupId>\\s*([^<]+?)\\s*</groupId>")
                .matcher(siteXmlContent);
        Matcher skinArtifactMatcher = Pattern.compile(
                "<skin>\\s*<groupId>[^<]+</groupId>\\s*"
                        + "<artifactId>\\s*([^<]+?)\\s*</artifactId>")
                .matcher(siteXmlContent);
        if (!skinGroupMatcher.find()
                || !skinGroupMatcher.group(1).trim().equals(expectedSkinGroup)) {
            String actual = skinGroupMatcher.reset().find()
                    ? skinGroupMatcher.group(1).trim() : "(missing)";
            problems.add("<skin>/<groupId>: expected '" + expectedSkinGroup
                    + "', found '" + actual + "'");
        }
        if (!skinArtifactMatcher.find()
                || !skinArtifactMatcher.group(1).trim().equals(expectedSkinArtifact)) {
            String actual = skinArtifactMatcher.reset().find()
                    ? skinArtifactMatcher.group(1).trim() : "(missing)";
            problems.add("<skin>/<artifactId>: expected '"
                    + expectedSkinArtifact + "', found '" + actual + "'");
        }

        return problems;
    }

    /**
     * Return the expected {@code <bodyClass>} for a deployment target.
     *
     * @param deployment one of {@code ike-network},
     *                   {@code ike-network-example}, or
     *                   {@code knowledge-design}
     * @return the expected {@code bodyClass} string
     */
    public static String expectedBodyClassFor(String deployment) {
        if ("knowledge-design".equals(deployment)) {
            return "sentry-purple";
        }
        if ("ike-network-example".equals(deployment)) {
            return "sentry-orange";
        }
        // Default + explicit ike-network → Forest theme.
        return "sentry-green";
    }

    /**
     * Breadcrumb names that are known-stale and should never appear
     * in a current IKE site descriptor. Extend this list when a
     * rebrand or split retires a label.
     */
    static final List<String> STALE_BREADCRUMB_NAMES = List.of(
            "IKE Pipeline"  // #216 ike-pipeline → ike-docs/ike-platform
    );
}
