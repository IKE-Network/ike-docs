package network.ike.docs.plugin.registry;

import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A real git repository in a temporary directory for the registry tests:
 * the {@code git} command line, as the goal itself runs it, with no stub
 * of any kind. Tests that need it are skipped where {@code git} is not
 * on the path.
 */
final class TempGit {

    final Path dir;

    private TempGit(Path dir) {
        this.dir = dir;
    }

    /** Initialize a repository at a directory, with an identity so commits need no global config. */
    static TempGit init(Path dir) throws IOException {
        Files.createDirectories(dir);
        TempGit git = new TempGit(dir);
        try {
            git.run("init", "-q", "-b", "main");
        } catch (IOException e) {
            Assumptions.abort("git is not available: " + e.getMessage());
        }
        git.run("config", "user.name", "Registry Test");
        git.run("config", "user.email", "registry@example.invalid");
        git.run("config", "commit.gpgsign", "false");
        return git;
    }

    /** Write a file under the repository, creating directories. */
    Path write(String relative, String content) throws IOException {
        Path file = dir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /** Stage everything and commit. */
    void commitAll(String message) throws IOException {
        run("add", "-A", ".");
        run("commit", "-q", "-m", message);
    }

    /** The blob id git computes for a file, as {@code git hash-object} reports it. */
    String hashObject(String relative) throws IOException {
        return run("hash-object", relative).strip();
    }

    /** Run git in the repository and return its standard output. */
    String run(String... args) throws IOException {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true);
        Process process = builder.start();
        String out;
        try {
            out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) {
                throw new IOException("git " + String.join(" ", args) + " failed: " + out);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        return out;
    }
}
