package network.ike.docs.plugin.packages;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static network.ike.docs.plugin.packages.PackageManifestTest.labLibrary;
import static network.ike.docs.plugin.packages.PackageManifestTest.topic;
import static network.ike.docs.plugin.packages.PackageManifestTest.write;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PackageLint} on real trees: a clean library passes, and each rule
 * of the layout reports its own finding, all of them at once.
 */
class PackageLintTest {

    @Test
    void aCleanLibrary_passes(@TempDir Path dir) throws IOException {
        PackageLint.Report report = PackageLint.lint(labLibrary(dir), "network.ike.lab", List.of());

        assertThat(report.clean()).isTrue();
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void theRoot_holdsOnlyGroupDirectoriesAndMetaInf(@TempDir Path dir) throws IOException {
        Path root = labLibrary(dir);
        write(root, "index.adoc", "= Preview at the root\n");
        write(root, "koncepts.yml", "koncepts: []\n");
        write(root, "images/shared.svg", "<svg/>");
        write(root, "topics/dev/old.adoc", topic("dev-old"));
        Files.createDirectories(root.resolve("target/stale"));

        PackageLint.Report report = PackageLint.lint(root, "network.ike.lab", List.of());

        assertThat(report.errors()).containsExactly(
                "images/: images at the root; move them into the images directory of the package that uses them",
                "index.adoc: a file at the root; a topic belongs in <group>/<domain>/, the preview index in the"
                        + " library's own package",
                "koncepts.yml: a file at the root; the root holds only <group>/<domain> packages and META-INF",
                "topics/: not a dotted group directory; the root holds only <group>/<domain> packages and META-INF");
    }

    @Test
    void everyPackage_beginsWithTheModulesGroup_unlessDeclared(@TempDir Path dir) throws IOException {
        Path root = labLibrary(dir);
        write(root, "network.ike.examples/corpus/sample.adoc", topic("corpus-sample"));
        write(root, "network.ike.lab.extra/more/more.adoc", topic("more-more"));

        PackageLint.Report strict = PackageLint.lint(root, "network.ike.lab", List.of());
        assertThat(strict.errors()).containsExactly(
                "package network.ike.examples/corpus does not begin with the module's group network.ike.lab;"
                        + " move it to a library of its own, or declare the prefix in ike.adoc.packages on purpose");

        PackageLint.Report declared = PackageLint.lint(root, "network.ike.lab", List.of("network.ike.examples/corpus"));
        assertThat(declared.clean()).isTrue();
        assertThat(PackageLint.lint(root, "network.ike.lab", List.of(" network.ike.examples ")).clean()).isTrue();
        assertThat(PackageLint.lint(root, "network.ike.lab", List.of("network.ike.examples/other")).clean()).isFalse();
    }

    @Test
    void aTopicsDirectory_equalsItsIdsDomainPrefix(@TempDir Path dir) throws IOException {
        Path root = labLibrary(dir);
        write(root, "network.ike.lab/dev/misfiled.adoc", topic("arch-misfiled"));
        write(root, "network.ike.lab/dev/deeper/fine.adoc", topic("dev-fine"));
        Path anf = dir.resolve("anf/src/docs/asciidoc");
        write(anf, "network.ike.kompendium.anf/determine-topic.adoc", topic("anf-determine-topic"));
        write(anf, "network.ike.kompendium.anf/stray.adoc", topic("dev-stray"));

        PackageLint.Report lab = PackageLint.lint(root, "network.ike.lab", List.of());
        assertThat(lab.errors()).containsExactly(
                "network.ike.lab/dev/misfiled.adoc: topic arch-misfiled is in package network.ike.lab/dev but its"
                        + " id's domain is arch; move it to the arch package or rename it dev-...");

        PackageLint.Report kompendium = PackageLint.lint(anf, "network.ike.kompendium.anf", List.of());
        assertThat(kompendium.errors()).containsExactly(
                "network.ike.kompendium.anf/stray.adoc: topic dev-stray is in package network.ike.kompendium.anf"
                        + " but its id's domain is dev; move it to the dev package or rename it anf-...");
    }

    @Test
    void images_areInsidePackages(@TempDir Path dir) throws IOException {
        Path root = labLibrary(dir);
        write(root, "network.ike.lab/images/shared.svg", "<svg/>");

        PackageLint.Report report = PackageLint.lint(root, "network.ike.lab", List.of());

        assertThat(report.errors()).containsExactly(
                "network.ike.lab/images: images outside a package; move them into the images directory of the"
                        + " package that uses them");
    }

    @Test
    void linesStillUsingTheTopicsAlias_areWarningsNamingTheLine(@TempDir Path dir) throws IOException {
        Path root = labLibrary(dir);
        write(root, "network.ike.lab/lab/index.adoc", """
                = Preview
                // include::{topics}/topics/dev/commented.adoc[]

                include::{topics}/topics/dev/gitflow-overview.adoc[leveloffset=+1]
                include::{adoc}/network.ike.lab/arch/overview.adoc[leveloffset=+1]
                image::{topics}/images/brand/logo.svg[Logo]
                See image:{topics}/images/brand/icon.svg[] inline.
                The word {topics} in prose is not reported.
                """);

        PackageLint.Report report = PackageLint.lint(root, "network.ike.lab", List.of());

        assertThat(report.clean()).isTrue();
        assertThat(report.warnings()).containsExactly(
                "network.ike.lab/lab/index.adoc:4: still uses {topics}, replace it with {adoc}:"
                        + " include::{topics}/topics/dev/gitflow-overview.adoc[leveloffset=+1]",
                "network.ike.lab/lab/index.adoc:6: still uses {topics}, replace it with {adoc}:"
                        + " image::{topics}/images/brand/logo.svg[Logo]",
                "network.ike.lab/lab/index.adoc:7: still uses {topics}, replace it with {adoc}:"
                        + " See image:{topics}/images/brand/icon.svg[] inline.");
    }

    @Test
    void everyFinding_isListedAtOnce(@TempDir Path dir) throws IOException {
        Path root = labLibrary(dir);
        write(root, "stray.adoc", "= Stray\n");
        write(root, "other.group/x/x.adoc", topic("x-x"));
        write(root, "network.ike.lab/dev/wrong.adoc", topic("ops-wrong"));

        PackageLint.Report report = PackageLint.lint(root, "network.ike.lab", List.of());

        // the root first, then each package in name order
        assertThat(report.errors()).hasSize(3);
        assertThat(report.errors().get(0)).startsWith("stray.adoc: a file at the root");
        assertThat(report.errors().get(1)).startsWith("network.ike.lab/dev/wrong.adoc: topic ops-wrong");
        assertThat(report.errors().get(2)).startsWith("package other.group/x does not begin");
    }
}
