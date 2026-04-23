package network.ike.tools.linebreak;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.asciidoctor.Asciidoctor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reformat AsciiDoc files to use semantic linefeeds.
 *
 * <p>Line breaks are placed at logical boundaries — sentences, clauses,
 * asides, and compound-sentence joints — producing source text that is
 * easier to diff, edit, and reason about.
 *
 * <p>Usage:
 * <pre>{@code
 * mvn slb:reformat -Dfile=src/docs/asciidoc/my-doc.adoc -DdryRun=true
 * mvn slb:reformat -Dfile=src/docs/asciidoc/  # reformat all .adoc files in directory
 * }</pre>
 *
 * @since 89
 */
@Mojo(name = "reformat", threadSafe = true)
public class SemanticLineBreakMojo extends AbstractMojo {

    /** Input file or directory. Directories are searched for {@code *.adoc} files. */
    @Parameter(property = "file", required = true)
    String file;

    /** Output file path. Only valid with a single input file. */
    @Parameter(property = "outputFile")
    String outputFile;

    /** Print reformatted output to the build log without modifying files. */
    @Parameter(property = "dryRun", defaultValue = "false")
    boolean dryRun;

    /** Log which paragraphs are reformatted. */
    @Parameter(property = "verbose", defaultValue = "false")
    boolean verbose;

    /** Break only at sentence boundaries ({@code . ? !}), ignoring clauses and punctuation. */
    @Parameter(property = "sentencesOnly", defaultValue = "false")
    boolean sentencesOnly;

    /** Enable comma clause breaks. */
    @Parameter(property = "clauseBreak", defaultValue = "true")
    boolean clauseBreak;

    /** Minimum line length before comma breaks are applied. */
    @Parameter(property = "clauseBreakThreshold", defaultValue = "0")
    int clauseBreakThreshold;

    /** Soft-wrap line length limit. */
    @Parameter(property = "maxLineLength", defaultValue = "64")
    int maxLineLength;

    /** Minimum characters after a soft-wrap break point. */
    @Parameter(property = "minRemainder", defaultValue = "15")
    int minRemainder;

    /** Lines shorter than this are merged with the following line. */
    @Parameter(property = "minLineLength", defaultValue = "10")
    int minLineLength;

    /** Disable soft wrapping (equivalent to {@code maxLineLength=MAX_VALUE}). */
    @Parameter(property = "noWrap", defaultValue = "false")
    boolean noWrap;

    /** Skip execution. */
    @Parameter(property = "slb.skip", defaultValue = "false")
    boolean skip;

    /** Creates this goal instance. */
    public SemanticLineBreakMojo() {}

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().debug("slb:reformat — skipped");
            return;
        }

        // ── Configure the breaker ──────────────────────────────────────
        var breaker = new SemanticLineBreaker();
        breaker.setSentencesOnly(sentencesOnly);
        breaker.setClauseBreak(clauseBreak);
        breaker.setClauseBreakThreshold(clauseBreakThreshold);
        breaker.setMaxLineLength(noWrap ? Integer.MAX_VALUE : maxLineLength);
        breaker.setMinRemainder(minRemainder);
        breaker.setMinLineLength(minLineLength);
        breaker.setDryRun(dryRun);
        breaker.setVerbose(verbose);

        // ── Resolve input files ────────────────────────────────────────
        Path inputPath = Path.of(file);
        if (!Files.exists(inputPath)) {
            throw new MojoExecutionException(
                    "Input path does not exist: " + file);
        }

        List<Path> filesToProcess = new ArrayList<>();
        try {
            if (Files.isDirectory(inputPath)) {
                SemanticLineBreaker.collectAdocFiles(inputPath, filesToProcess);
            } else {
                filesToProcess.add(inputPath);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to resolve input files", e);
        }

        if (filesToProcess.isEmpty()) {
            getLog().info("slb:reformat — no .adoc files found in " + file);
            return;
        }

        if (outputFile != null && filesToProcess.size() > 1) {
            throw new MojoExecutionException(
                    "outputFile cannot be used with multiple input files");
        }

        // ── Process files ──────────────────────────────────────────────
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            int totalReformatted = 0;

            for (Path inPath : filesToProcess) {
                String effectiveOutput = outputFile;
                if (effectiveOutput == null && !dryRun) {
                    effectiveOutput = inPath.toString();
                }

                int result = breaker.processFile(
                        asciidoctor, inPath, effectiveOutput);
                if (result > 0) {
                    totalReformatted++;
                    getLog().info("slb:reformat — reformatted " + inPath);
                }
            }

            getLog().info("slb:reformat — " + totalReformatted + " of "
                    + filesToProcess.size() + " file(s) had changes"
                    + (dryRun ? " (dry run)" : ""));
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to process AsciiDoc files", e);
        }
    }
}
