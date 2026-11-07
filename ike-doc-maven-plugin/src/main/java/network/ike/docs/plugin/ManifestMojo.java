package network.ike.docs.plugin;

import network.ike.docs.plugin.packages.AdocLayout;
import network.ike.docs.plugin.packages.AdocPackages;
import network.ike.docs.plugin.packages.PackageManifest;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generate the package manifest of a topic library laid out as packages
 * ({@code dev-adoc-packages}, IKE-Network/ike-issues#1341):
 * {@code target/adoc-meta/META-INF/ike/<groupId>/<artifactId>/manifest.yaml},
 * from the POM and the tree, never kept by hand. The manifest records the
 * artifact's coordinates, the packages it ships, each as
 * {@code {name: <group>/<domain>, kind: topics|library}}, and the path of
 * its registry. A package's kind is {@code library} when the POM property
 * {@code ike.adoc.library-package} names it, or when it holds an
 * {@code index.adoc} and no topic. The parent POM adds the
 * {@code target/adoc-meta} directory to the adoc zip, so the manifest
 * ships beside the registry under {@code META-INF}, keyed by coordinates
 * where it cannot collide in a union.
 *
 * <p>Gated by the POM property {@code ike.adoc.layout=packages}: a library
 * without it is skipped, so the layout before packages keeps building
 * unchanged. Runs at {@code generate-resources}. Skip with
 * {@code -Dike.skip.manifest=true}.
 *
 * @since 136
 */
@Mojo(name = "manifest", defaultPhase = "generate-resources")
public class ManifestMojo implements org.apache.maven.api.plugin.Mojo {

    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.plugin.Log log;

    /**
     * Access the Maven logger.
     *
     * @return the logger
     */
    protected org.apache.maven.api.plugin.Log getLog() { return log; }

    /** The layout the library declares; only {@code packages} is manifested. */
    @Parameter(property = AdocLayout.PROPERTY)
    String layout;

    /** The package that is the library's own (the preview index, shared images), of kind {@code library}. */
    @Parameter(property = "ike.adoc.library-package")
    String libraryPackage;

    /** The AsciiDoc root the packages are under. */
    @Parameter(property = "ike.adoc.root", defaultValue = "${project.basedir}/src/docs/asciidoc")
    File root;

    /** The directory the metadata tree is written under; the parent POM zips it with the sources. */
    @Parameter(property = "ike.adoc.meta-directory", defaultValue = "${project.build.directory}/adoc-meta")
    File metaDirectory;

    /** Skip the goal. */
    @Parameter(property = "ike.skip.manifest", defaultValue = "false")
    boolean skip;

    /** The project this goal runs in, for its coordinates. */
    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.Project project;

    /** Creates this goal instance. */
    public ManifestMojo() {}

    @Override
    public void execute() throws MojoException {
        if (skip) {
            getLog().debug("idoc:manifest — skipped");
            return;
        }
        AdocLayout adocLayout;
        try {
            adocLayout = AdocLayout.of(layout);
        } catch (IllegalArgumentException e) {
            throw new MojoException(e.getMessage(), e);
        }
        if (adocLayout != AdocLayout.PACKAGES) {
            getLog().debug("idoc:manifest — " + AdocLayout.PROPERTY + " is not packages, skipping");
            return;
        }
        Path rootPath = root.toPath();
        if (!Files.isDirectory(rootPath)) {
            getLog().info("idoc:manifest — no " + root + ", skipping");
            return;
        }
        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();
        Path out = metaDirectory.toPath().resolve(AdocPackages.metaDirectory(groupId, artifactId))
                .resolve(PackageManifest.FILE_NAME);
        List<AdocPackages.Package> packages;
        try {
            packages = AdocPackages.packages(rootPath, blankToNull(libraryPackage));
            Files.createDirectories(out.getParent());
            Files.writeString(out, PackageManifest.yaml(PackageManifest.model(packages, groupId, artifactId)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoException("Cannot write the manifest to " + out + ": " + e.getMessage(), e);
        }
        for (AdocPackages.Package p : packages) {
            getLog().info("manifest: " + p.name() + " (" + p.kind().label() + ", " + p.topics().size()
                    + " topic" + (p.topics().size() == 1 ? "" : "s") + ")");
        }
        getLog().info("idoc:manifest — " + groupId + ":" + artifactId + ", " + packages.size() + " package"
                + (packages.size() == 1 ? "" : "s") + ", written to " + shown(project.getBasedir(), out));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String shown(Path base, Path out) {
        return out.toAbsolutePath().startsWith(base.toAbsolutePath())
                ? base.toAbsolutePath().relativize(out.toAbsolutePath()).toString()
                : out.toString();
    }
}
