package network.ike.docs.plugin;

import network.ike.docs.plugin.registry.TopicHeader;
import network.ike.docs.plugin.registry.TopicRegistry;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generate the topic registry: every AsciiDoc file under the module's source
 * root, with its storage location and header metadata (the {@code :topic-*:}
 * block of a topic fragment; the title, document attributes and include count
 * of any file), written to {@code target/topic-registry.yaml} in the shape
 * {@code IKE-TOPIC-REGISTRY.md} gives the registry: topics grouped into
 * domains by the prefix of their id. The same structure is printed to the
 * build log as a tree. It applies to every module the doc pipeline renders:
 * topic libraries, assemblies, doc-only and hybrid projects.
 *
 * <p>This is the topic registry's build-time revision: the registry keeps its
 * purpose and shape and is now generated from the files on every run instead
 * of kept by hand at {@code src/docs/asciidoc/topic-registry.yaml}. It is
 * never committed and never edits a source file. Header findings (a
 * missing required attribute, an anchor that does not match the id, an
 * unknown type or status, the same id in two files) are reported in the
 * registry and the log and do not fail the build.
 *
 * <p>A module without {@code src/docs/asciidoc} is skipped with one log
 * line, so the goal can run from a reactor root. Usage:
 *
 * <pre>
 *   mvn idoc:topic-registry                                          # the module's src/docs/asciidoc
 *   mvn idoc:topic-registry -Dike.topic-registry.roots=a,b           # other roots, comma-separated
 *   mvn idoc:topic-registry -Dike.topic-registry.output=/tmp/r.yaml  # write elsewhere
 *   mvn idoc:topic-registry -Dike.topic-registry.add=topics/x/new.adoc   # add one ingested file, no rescan
 * </pre>
 *
 * <p>With {@code -Dike.topic-registry.add} the named files are parsed alone
 * and merged into the existing registry: placed in their domain, counted, and
 * checked for a duplicate id against the registry. An entry for the same file
 * is replaced, so re-ingesting is safe. Deleted files are not noticed that
 * way; a run without {@code add} re-derives everything.
 *
 * <p>The default phase is {@code validate}: the goal reads sources and
 * writes only under {@code target/}, so an execution declared without a
 * phase (as {@code ike-parent} does for {@code lint-site}) refreshes the
 * registry on every build, including a bare {@code mvn validate}.
 *
 * <p>Skip with {@code -Dike.skip.topic-registry=true}.
 *
 * @since 109
 */
@Mojo(name = "topic-registry", defaultPhase = "validate")
public class TopicRegistryMojo implements org.apache.maven.api.plugin.Mojo {

    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.plugin.Log log;

    /**
     * Access the Maven logger.
     *
     * @return the logger
     */
    protected org.apache.maven.api.plugin.Log getLog() { return log; }

    /**
     * Directories to scan, comma-separated. Defaults to the module's
     * {@code src/docs/asciidoc}; when that default directory does not exist
     * the goal is skipped, while a named directory that does not exist
     * fails the build.
     */
    @Parameter(property = "ike.topic-registry.roots",
               defaultValue = "${project.basedir}/src/docs/asciidoc")
    String roots;

    /** Where the generated registry is written. */
    @Parameter(property = "ike.topic-registry.output",
               defaultValue = "${project.build.directory}/topic-registry.yaml")
    File output;

    /** The most {@code .adoc} files one root may hold before the scan fails. */
    @Parameter(property = "ike.topic-registry.maxFiles", defaultValue = "5000")
    int maxFiles;

    /**
     * Files to add to the existing registry without rescanning, comma-separated,
     * relative to the module base directory or absolute. Each must be an
     * {@code .adoc} file under one of the registry's roots. When no generated
     * registry exists yet, the full scan runs instead.
     */
    @Parameter(property = "ike.topic-registry.add")
    String add;

    /** Skip the goal. */
    @Parameter(property = "ike.skip.topic-registry", defaultValue = "false")
    boolean skip;

    /** The project this goal runs in; registry paths are shown relative to its base directory. */
    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.Project project;

    /** Creates this goal instance. */
    public TopicRegistryMojo() {}

    @Override
    public void execute() throws MojoException {
        if (skip) {
            getLog().debug("idoc:topic-registry — skipped");
            return;
        }
        long started = System.nanoTime();
        Path base = project.getBasedir();
        if (add != null && !add.isBlank()) {
            if (Files.isRegularFile(output.toPath())) {
                addToRegistry(base, output.toPath(), started);
                return;
            }
            getLog().info("idoc:topic-registry — no generated registry at " + output
                    + " yet, running the full scan");
        }
        Path defaultRoot = base.resolve("src/docs/asciidoc");
        List<Path> rootPaths = new ArrayList<>();
        for (String r : roots.split(",")) {
            if (!r.isBlank()) {
                rootPaths.add(Path.of(r.strip()));
            }
        }
        if (rootPaths.size() == 1 && rootPaths.get(0).toAbsolutePath().normalize()
                .equals(defaultRoot.toAbsolutePath().normalize())
                && !Files.isDirectory(defaultRoot)) {
            getLog().info("idoc:topic-registry — no src/docs/asciidoc in " + base.getFileName()
                    + ", skipping");
            return;
        }

        List<TopicRegistry.Scan> scans = new ArrayList<>();
        for (Path root : rootPaths) {
            if (!Files.isDirectory(root)) {
                throw new MojoException("Registry root does not exist: " + root
                        + " (check -Dike.topic-registry.roots)");
            }
            try {
                scans.add(TopicRegistry.scan(root, maxFiles));
            } catch (IOException e) {
                throw new MojoException("Cannot scan " + root + ": " + e.getMessage(), e);
            } catch (IllegalStateException e) {
                throw new MojoException(e.getMessage(), e);
            }
        }

        String yaml = TopicRegistry.yaml(TopicRegistry.model(scans, base, Instant.now()));
        Path out = output.toPath();
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, yaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoException("Cannot write the registry to " + out + ": " + e.getMessage(), e);
        }

        int topics = 0;
        int others = 0;
        int findings = 0;
        for (TopicRegistry.Scan scan : scans) {
            for (String line : TopicRegistry.tree(scan, base)) {
                getLog().info(line);
            }
            topics += scan.topics().size();
            others += scan.others().size();
            findings += scan.findings().size();
            for (String finding : scan.findings()) {
                getLog().warn("registry: " + finding);
            }
        }
        long millis = (System.nanoTime() - started) / 1_000_000;
        getLog().info(topics + " topic" + (topics == 1 ? "" : "s") + ", " + others
                + " other file" + (others == 1 ? "" : "s") + ", " + findings
                + " finding" + (findings == 1 ? "" : "s") + "; topic registry written to "
                + shown(base, out) + " (" + millis + " ms)");
    }

    private void addToRegistry(Path base, Path out, long started) throws MojoException {
        List<Path> files = new ArrayList<>();
        for (String f : add.split(",")) {
            if (!f.isBlank()) {
                files.add(Path.of(f.strip()));
            }
        }
        Map<String, Object> model;
        try {
            model = TopicRegistry.load(out);
        } catch (IOException e) {
            throw new MojoException("Cannot read the generated registry " + out + ": "
                    + e.getMessage(), e);
        }
        List<TopicHeader> added;
        try {
            added = TopicRegistry.add(model, base, files, Instant.now());
        } catch (IllegalArgumentException e) {
            throw new MojoException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoException("Cannot read a file to add: " + e.getMessage(), e);
        }
        try {
            Files.writeString(out, TopicRegistry.yaml(model), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoException("Cannot write the registry to " + out + ": " + e.getMessage(), e);
        }
        for (TopicHeader h : added) {
            getLog().info("registry: " + (h.topic() ? "topic " + h.id() + " at " : "file ") + h.file());
        }
        @SuppressWarnings("unchecked")
        List<Object> findings = (List<Object>) model.get("findings");
        for (Object f : findings) {
            for (TopicHeader h : added) {
                if (String.valueOf(f).startsWith(h.file() + ": ")) {
                    getLog().warn("registry: " + f);
                }
            }
        }
        long millis = (System.nanoTime() - started) / 1_000_000;
        getLog().info("added " + added.size() + " file" + (added.size() == 1 ? "" : "s")
                + " to " + shown(base, out) + " without rescanning (" + millis + " ms)");
    }

    private static String shown(Path base, Path out) {
        return out.toAbsolutePath().startsWith(base.toAbsolutePath())
                ? base.toAbsolutePath().relativize(out.toAbsolutePath()).toString()
                : out.toString();
    }
}
