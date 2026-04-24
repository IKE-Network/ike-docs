package network.ike.docs.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the pure zipDirectory function used by PackageDocMojo.
 *
 * <p>The execute() method's main-artifact wiring requires a real Maven
 * Session and ArtifactManager, so it is exercised by integration
 * testing (downstream {@code ike-doc}-packaged projects such as
 * {@code ike-lab-documents/topics}). These tests cover the self-
 * contained archiving logic.
 */
class PackageDocMojoTest {

    @Test
    void zipsFlatDirectoryEntries(@TempDir Path tmp) throws IOException {
        Path source = Files.createDirectory(tmp.resolve("src"));
        Files.writeString(source.resolve("a.adoc"), "one");
        Files.writeString(source.resolve("b.adoc"), "two");
        Path zip = tmp.resolve("out.zip");

        int count = PackageDocMojo.zipDirectory(source, zip);

        assertThat(count).isEqualTo(2);
        assertThat(entriesIn(zip))
                .containsExactlyInAnyOrder("a.adoc", "b.adoc");
    }

    @Test
    void zipsNestedDirectoryEntriesWithRelativePaths(@TempDir Path tmp)
            throws IOException {
        Path source = Files.createDirectory(tmp.resolve("src"));
        Path sub = Files.createDirectories(source.resolve("topics/dev"));
        Files.writeString(sub.resolve("note.adoc"), "nested");
        Files.writeString(source.resolve("index.adoc"), "root");
        Path zip = tmp.resolve("out.zip");

        int count = PackageDocMojo.zipDirectory(source, zip);

        assertThat(count).isEqualTo(2);
        assertThat(entriesIn(zip))
                .containsExactlyInAnyOrder(
                        "index.adoc",
                        "topics/dev/note.adoc");
    }

    @Test
    void producesEmptyZipForMissingSource(@TempDir Path tmp) throws IOException {
        Path source = tmp.resolve("does-not-exist");
        Path zip = tmp.resolve("out.zip");

        int count = PackageDocMojo.zipDirectory(source, zip);

        assertThat(count).isZero();
        assertThat(Files.exists(zip)).isTrue();
        assertThat(entriesIn(zip)).isEmpty();
    }

    @Test
    void producesEmptyZipForEmptyDirectory(@TempDir Path tmp) throws IOException {
        Path source = Files.createDirectory(tmp.resolve("empty"));
        Path zip = tmp.resolve("out.zip");

        int count = PackageDocMojo.zipDirectory(source, zip);

        assertThat(count).isZero();
        assertThat(Files.exists(zip)).isTrue();
        assertThat(entriesIn(zip)).isEmpty();
    }

    private static Set<String> entriesIn(Path zip) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zis =
                     new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry e = zis.getNextEntry(); e != null;
                 e = zis.getNextEntry()) {
                names.add(e.getName());
            }
        }
        return names;
    }
}
