package network.ike.docs.plugin;

import network.ike.docs.plugin.packages.AdocLayout;
import network.ike.docs.plugin.packages.PackageLint;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lint a topic library laid out as packages ({@code dev-adoc-packages},
 * IKE-Network/ike-issues#1341) and fail the build with every finding
 * listed: the root holds only dotted-group directories and
 * {@code META-INF}; every package begins with the module's own group
 * unless the POM property {@code ike.adoc.packages} declares another
 * prefix on purpose; a topic's directory equals the domain prefix of its
 * {@code :topic-id:}; images are inside packages, not at the root. An
 * include or image line that still uses the {@code {topics}} alias is a
 * warning naming the file and line. The rules are {@link PackageLint}'s;
 * this goal is the wiring.
 *
 * <p>Gated by the POM property {@code ike.adoc.layout=packages}: a library
 * without it is skipped, so the layout before packages keeps building
 * unchanged. Runs at {@code validate}. Skip with
 * {@code -Dike.skip.lint-packages=true}.
 *
 * @since 136
 */
@Mojo(name = "lint-packages", defaultPhase = "validate")
public class LintPackagesMojo implements org.apache.maven.api.plugin.Mojo {

    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.plugin.Log log;

    /**
     * Access the Maven logger.
     *
     * @return the logger
     */
    protected org.apache.maven.api.plugin.Log getLog() { return log; }

    /** The layout the library declares; only {@code packages} is linted. */
    @Parameter(property = AdocLayout.PROPERTY)
    String layout;

    /**
     * Package names or group prefixes the library ships on purpose beyond
     * its own group, comma-separated; a package passes the ownership rule
     * when it equals one or begins with one.
     */
    @Parameter(property = "ike.adoc.packages")
    String packages;

    /** The AsciiDoc root the packages are under. */
    @Parameter(property = "ike.adoc.root", defaultValue = "${project.basedir}/src/docs/asciidoc")
    File root;

    /** Skip the goal. */
    @Parameter(property = "ike.skip.lint-packages", defaultValue = "false")
    boolean skip;

    /** The project this goal runs in, for its group. */
    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.Project project;

    /** Creates this goal instance. */
    public LintPackagesMojo() {}

    @Override
    public void execute() throws MojoException {
        if (skip) {
            getLog().debug("idoc:lint-packages — skipped");
            return;
        }
        AdocLayout adocLayout;
        try {
            adocLayout = AdocLayout.of(layout);
        } catch (IllegalArgumentException e) {
            throw new MojoException(e.getMessage(), e);
        }
        if (adocLayout != AdocLayout.PACKAGES) {
            getLog().debug("idoc:lint-packages — " + AdocLayout.PROPERTY + " is not packages, skipping");
            return;
        }
        Path rootPath = root.toPath();
        if (!Files.isDirectory(rootPath)) {
            getLog().info("idoc:lint-packages — no " + root + ", skipping");
            return;
        }
        List<String> declared = new ArrayList<>();
        if (packages != null) {
            for (String p : packages.split(",")) {
                if (!p.isBlank()) {
                    declared.add(p.strip());
                }
            }
        }
        PackageLint.Report report;
        try {
            report = PackageLint.lint(rootPath, project.getGroupId(), declared);
        } catch (IOException e) {
            throw new MojoException("Cannot lint " + rootPath + ": " + e.getMessage(), e);
        }
        for (String warning : report.warnings()) {
            getLog().warn("lint-packages: " + warning);
        }
        for (String error : report.errors()) {
            getLog().error("lint-packages: " + error);
        }
        int errors = report.errors().size();
        if (errors > 0) {
            throw new MojoException("idoc:lint-packages — " + errors + " finding" + (errors == 1 ? "" : "s")
                    + " in " + rootPath + " (listed above); the packages layout is <group>/<domain>/<topic>.adoc"
                    + " with the group as one dotted segment and the domain equal to the topic id's prefix");
        }
        int warnings = report.warnings().size();
        getLog().info("idoc:lint-packages — " + project.getGroupId() + " clean"
                + (warnings == 0 ? "" : ", " + warnings + " line" + (warnings == 1 ? "" : "s")
                + " still using " + PackageLint.ALIAS));
    }
}
