package network.ike.docs.plugin;

import network.ike.docs.koncept.KonceptInlineMacro;
import network.ike.docs.plugin.diff.AdocDiffMarker;
import network.ike.docs.plugin.diff.ChangeManifest;
import network.ike.docs.plugin.diff.ChangeStatus;
import network.ike.docs.plugin.diff.GitSource;
import network.ike.docs.plugin.diff.PacketAssembler;
import network.ike.docs.plugin.diff.RegistryDelta;
import network.ike.docs.plugin.diff.RegistryIndex;
import network.ike.docs.plugin.diff.StampRegistry;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generate and render a doc-diff <em>review packet</em>: every changed
 * AsciiDoc fragment between two refs (or a ref and the working tree),
 * marked inline with {@code [.diff-ins]}/{@code [.diff-del]} roles and
 * composed into a book with a cover summary, Record of Changes, Change
 * Glossary, Change Index, registry delta, and assembly scaffolding
 * diffs (ike-issues#648, #649, #650; design note
 * {@code dev-doc-diff-pipeline} in ike-lab-documents).
 *
 * <p>The goal works at the Maven subproject level and adapts to the
 * module it runs in:
 *
 * <ul>
 *   <li><b>Topics-library module</b> (its source root holds
 *       {@code topic-registry.yaml}): the corpus packet — every changed
 *       fragment, the full registry delta including each assembly's
 *       membership changes, and the module's own scaffolding.</li>
 *   <li><b>Assembly module</b>: the packet is the <em>projection</em>
 *       of the corpus diff onto this assembly — changed topics
 *       intersected with the assembly's flattened {@code topic-refs}
 *       (assembly id defaults to the artifactId; topic ids resolve to
 *       files through the per-domain registries), plus the module's own
 *       master-file scaffolding diff and a membership delta for just
 *       this assembly. Topics deleted in range are reported by the
 *       membership delta rather than projected.</li>
 *   <li><b>Other modules</b>: no source directory → skipped silently;
 *       no changes → skipped with a note. Invoking the goal from the
 *       aggregator therefore produces a packet for every subproject
 *       able to generate one.</li>
 * </ul>
 *
 * <p>Typical invocations:
 *
 * <pre>
 *   mvn idoc:diff                                  # HEAD vs working tree
 *   mvn idoc:diff -Dike.diff.from=v1.2.3           # tag vs working tree
 *   mvn idoc:diff -Dike.diff.from=A -Dike.diff.to=B  # any two commits
 * </pre>
 *
 * <p>Change entities come from {@code changes.yaml} — looked up first
 * beside the module pom, then at the repository work-tree root. For
 * commit-to-commit comparisons without one, they are derived by
 * grouping the range's commits on their {@code Refs:}/{@code Fixes:}
 * trailers (ike-issues#652); a working-tree comparison without one gets
 * a single synthetic "uncommitted changes" entity.
 *
 * <p>Both sides' content always renders through the <em>current</em>
 * toolchain — the diff is of knowledge, never of the renderer. Deleted
 * fragments are listed on the cover and in the registry delta rather
 * than rendered struck-through (v1 decision per #649). Diagram blocks
 * inside marked topics render as source listings in this goal's output;
 * full diagram fidelity comes from the regular pipeline render of a
 * packet wired as a doc module.
 *
 * <p>Outputs under {@code target/doc-diff/}: {@code asciidoc/} (the
 * generated packet sources), {@code html/}, and {@code pdf/} (prawn).
 * Skip rendering with {@code -Dike.diff.render=false}.
 *
 * @see <a href="https://github.com/IKE-Network/ike-issues/issues/648">ike-issues#648</a>
 */
@Mojo(name = "diff")
public class DocDiffMojo implements org.apache.maven.api.plugin.Mojo {

    private static final String REGISTRY_SUFFIX = "src/docs/asciidoc/topic-registry.yaml";

    @org.apache.maven.api.di.Inject
    private org.apache.maven.api.plugin.Log log;

    /**
     * Access the Maven logger.
     *
     * @return the logger
     */
    protected org.apache.maven.api.plugin.Log getLog() { return log; }

    /** The from side: any committish. */
    @Parameter(property = "ike.diff.from", defaultValue = "HEAD")
    String from;

    /** The to side: any committish, or {@code WORKTREE} for the working tree. */
    @Parameter(property = "ike.diff.to", defaultValue = GitSource.WORKTREE)
    String to;

    /** The module's AsciiDoc source root. */
    @Parameter(property = "ike.diff.sourceDirectory",
               defaultValue = "${project.basedir}/src/docs/asciidoc")
    File sourceDirectory;

    /**
     * Assembly id used for projection when this module is an assembly;
     * by IKE naming convention this is the artifactId.
     */
    @Parameter(property = "ike.diff.assemblyId", defaultValue = "${project.artifactId}")
    String assemblyId;

    /**
     * Optional comma-separated topic filter for a tighter packet —
     * topic ids (resolved through the registry) or path suffixes. When
     * set, only matching topics are marked; scaffolding and membership
     * sections are unaffected. Meant for share-scoped excerpts; for a
     * scope you reuse, register a small assembly instead and let the
     * projection do this permanently.
     */
    @Parameter(property = "ike.diff.topics")
    String topics;

    /** Authored change manifest; optional (see class javadoc). */
    @Parameter(property = "ike.diff.changesFile",
               defaultValue = "${project.basedir}/changes.yaml")
    File changesFile;

    /** Output root for generated sources and renders. */
    @Parameter(property = "ike.diff.outputDirectory",
               defaultValue = "${project.build.directory}/doc-diff")
    File outputDirectory;

    /**
     * Prawn theme for the PDF render — by default the unpacked
     * ike-doc-resources theme, which carries the diff roles.
     */
    @Parameter(property = "ike.diff.pdfTheme",
               defaultValue = "${project.build.directory}/ike-doc-resources/theme/ike-default-theme.yml")
    File pdfTheme;

    /** Optional fonts directory passed to the PDF render when present. */
    @Parameter(property = "ike.diff.pdfFontsDir",
               defaultValue = "${project.build.directory}/fonts")
    File pdfFontsDir;

    /** Render HTML and PDF after generating sources. */
    @Parameter(property = "ike.diff.render", defaultValue = "true")
    boolean render;

    /**
     * Kroki diagram server URL — same property the pipeline render
     * uses, so one override controls both goals.
     */
    @Parameter(property = "ike.asciidoc.diagramServerUrl",
               defaultValue = "https://kroki.komet.sh")
    String diagramServerUrl;

    /** Title of the generated packet document. */
    @Parameter(property = "ike.diff.title",
               defaultValue = "Documentation Review Packet")
    String title;

    /**
     * Stamp each change boundary with its provenance coordinate as a
     * reusable endnote (ike-issues#656); auto-suppressed when the whole
     * packet carries a single stamp, which is then stated on the cover.
     */
    @Parameter(property = "ike.diff.stamps", defaultValue = "true")
    boolean stamps;

    @Override
    public void execute() throws MojoException {
        if (sourceDirectory == null || !sourceDirectory.isDirectory()) {
            getLog().info("idoc:diff: no src/docs/asciidoc in this module — skipping");
            return;
        }
        try (GitSource git = GitSource.open(sourceDirectory.toPath().getParent())) {
            Path workTree = git.workTree();
            Path srcAbs = sourceDirectory.toPath().toAbsolutePath().normalize();
            if (!srcAbs.startsWith(workTree)) {
                throw new MojoException("sourceDirectory is outside the git work tree: " + srcAbs);
            }
            String srcRel = workTree.relativize(srcAbs).toString().replace(File.separatorChar, '/');

            boolean corpus = git.read(to, srcRel + "/topic-registry.yaml") != null;
            String registryRoot = corpus ? srcRel : discoverRegistryRoot(git);

            List<GitSource.Change> own = git.changes(from, to, srcRel + "/", ".adoc");
            String topicsPrefix = (corpus ? srcRel : registryRoot) == null
                    ? srcRel + "/topics/"
                    : (corpus ? srcRel : registryRoot) + "/topics/";

            List<GitSource.Change> markCandidates;
            List<GitSource.Change> scaffoldCandidates;
            String membershipDelta = null;
            String packetTitle = title;

            if (corpus) {
                markCandidates = own.stream()
                        .filter(c -> c.displayPath().startsWith(topicsPrefix)).toList();
                scaffoldCandidates = own.stream()
                        .filter(c -> !c.displayPath().startsWith(topicsPrefix)).toList();
            } else if (registryRoot != null) {
                RegistryIndex index = RegistryIndex.load(git, to, registryRoot);
                List<String> refs = index.assemblyRefs(assemblyId);
                if (refs == null) {
                    getLog().warn("idoc:diff: assembly '" + assemblyId
                            + "' is not registered in " + registryRoot
                            + "/topic-registry/assemblies.yaml — scaffolding-only packet");
                    markCandidates = List.of();
                } else {
                    Set<String> included = new LinkedHashSet<>();
                    for (String ref : refs) {
                        String file = index.topicFile(ref);
                        if (file != null) {
                            included.add(registryRoot + "/" + file);
                        }
                    }
                    markCandidates = git.changes(from, to, topicsPrefix, ".adoc").stream()
                            .filter(c -> c.status() != ChangeStatus.DELETED)
                            .filter(c -> included.contains(c.displayPath()))
                            .toList();
                    membershipDelta = RegistryIndex.membershipDelta(
                            git, from, to, registryRoot, assemblyId);
                    packetTitle = title + " — " + assemblyId;
                }
                scaffoldCandidates = own;
            } else {
                markCandidates = own.stream()
                        .filter(c -> c.displayPath().startsWith(topicsPrefix)).toList();
                scaffoldCandidates = own.stream()
                        .filter(c -> !c.displayPath().startsWith(topicsPrefix)).toList();
            }

            if (topics != null && !topics.isBlank()) {
                RegistryIndex index = registryRoot == null
                        ? null : RegistryIndex.load(git, to, registryRoot);
                Set<String> wanted = new LinkedHashSet<>();
                for (String token : topics.split(",")) {
                    String t = token.strip();
                    String file = index == null ? null : index.topicFile(t);
                    wanted.add(file != null ? registryRoot + "/" + file : t);
                }
                markCandidates = markCandidates.stream()
                        .filter(c -> wanted.stream().anyMatch(
                                w -> c.displayPath().equals(w) || c.displayPath().endsWith(w)))
                        .toList();
                getLog().info("idoc:diff topic filter active: " + markCandidates.size()
                        + " of the changed topics match " + topics);
            }

            Path diffDir = outputDirectory.toPath().resolve("asciidoc/_diff");
            Files.createDirectories(diffDir);

            ChangeManifest manifest = resolveManifest(git, markCandidates, own);

            // Stamp registry (#656): one stamp per range commit plus one
            // for uncommitted content; suppressed when only one exists.
            String stampToEnd = GitSource.WORKTREE.equals(to) ? "HEAD" : to;
            List<GitSource.CommitMeta> rangeCommits = git.commitsBetween(from, stampToEnd);
            Set<String> rangeIds = new LinkedHashSet<>();
            rangeCommits.forEach(c -> rangeIds.add(c.id()));
            boolean hasUncommitted = GitSource.WORKTREE.equals(to)
                    && !git.changes("HEAD", GitSource.WORKTREE, "", ".adoc").isEmpty();
            StampRegistry stampRegistry = new StampRegistry(rangeCommits, git.branch(),
                    git.userName(), java.time.LocalDate.now().toString(),
                    from + ".." + (GitSource.WORKTREE.equals(to) ? "worktree" : to));
            boolean stampsOn = stamps && stampRegistry.multipleStampsPossible(hasUncommitted);
            List<PacketAssembler.TopicEntry> topics = new ArrayList<>();
            List<String> deleted = new ArrayList<>();
            List<PacketAssembler.ScaffoldEntry> scaffolds = new ArrayList<>();
            List<List<String>> anchorsByChange = new ArrayList<>();
            manifest.changes().forEach(c -> anchorsByChange.add(new ArrayList<>()));

            for (GitSource.Change c : markCandidates) {
                if (c.status() == ChangeStatus.DELETED) {
                    deleted.add(c.displayPath());
                    continue;
                }
                List<String> newLines = lines(git.read(to, c.newPath()));
                List<String> oldLines = c.status() == ChangeStatus.ADDED
                        ? List.of() : lines(git.read(from, c.oldPath()));
                AdocDiffMarker.StampSource stampSource = null;
                String addedStampRef = null;
                if (stampsOn) {
                    String domain = domainOf(c.displayPath(), topicsPrefix);
                    List<String> lineIds = git.blameInRange(to, c.newPath(), rangeIds);
                    // blameInRange speaks precisely: a commit id, UNCOMMITTED,
                    // or null (out of range / indexing gap) — null falls to the
                    // coarse range stamp, never to uncommitted.
                    boolean rangeEmpty = rangeCommits.isEmpty();
                    stampSource = new AdocDiffMarker.StampSource() {
                        @Override
                        public String activeRef(int newLineIndex) {
                            String id = newLineIndex >= 0 && newLineIndex < lineIds.size()
                                    ? lineIds.get(newLineIndex) : null;
                            return stampRegistry.footnote(id,
                                    StampRegistry.Status.ACTIVE, domain);
                        }

                        @Override
                        public String inactiveRef() {
                            // A pure deletion is attributable only coarsely:
                            // to the range when one exists, else to the
                            // working tree.
                            return stampRegistry.footnote(
                                    rangeEmpty ? GitSource.UNCOMMITTED : null,
                                    StampRegistry.Status.INACTIVE, domain);
                        }
                    };
                    if (c.status() == ChangeStatus.ADDED) {
                        addedStampRef = stampRegistry.footnote(
                                lineIds.isEmpty() ? null : lineIds.get(0),
                                StampRegistry.Status.ACTIVE, domain);
                    }
                }
                AdocDiffMarker.MarkResult result = c.status() == ChangeStatus.ADDED
                        ? AdocDiffMarker.markAdded(newLines, from, addedStampRef)
                        : AdocDiffMarker.mark(oldLines, newLines, stampSource);
                String outName = c.displayPath().substring(topicsPrefix.length()).replace('/', '-');
                List<String> owners = ownerTitles(manifest, c.displayPath(), anchorsByChange, result);
                List<String> marked = PacketAssembler.injectChangeTerms(result.lines(), owners);
                if (!oldLines.isEmpty()) {
                    marked = AdocDiffMarker.withDiagramHistory(oldLines, marked);
                }
                Files.writeString(diffDir.resolve(outName),
                        String.join("\n", marked) + "\n", StandardCharsets.UTF_8);
                topics.add(new PacketAssembler.TopicEntry(c, outName, result));
                getLog().info("idoc:diff marked " + c.displayPath() + " (+" + result.insWords()
                        + "/-" + result.delWords() + ", " + result.notes().size() + " notes)");
            }

            for (GitSource.Change c : scaffoldCandidates) {
                if (c.status() == ChangeStatus.DELETED) {
                    deleted.add(c.displayPath());
                    continue;
                }
                List<String> oldLines = c.oldPath() == null
                        ? List.of() : lines(git.read(from, c.oldPath()));
                scaffolds.add(new PacketAssembler.ScaffoldEntry(c.displayPath(),
                        PacketAssembler.unifiedDiff(c.displayPath(), oldLines,
                                lines(git.read(to, c.newPath())))));
            }

            boolean hasRegistry;
            if (corpus) {
                hasRegistry = writeRegistryDelta(git, srcRel, diffDir);
            } else if (membershipDelta != null && !membershipDelta.isEmpty()) {
                Files.writeString(diffDir.resolve("registry-delta.adoc"),
                        membershipDelta, StandardCharsets.UTF_8);
                hasRegistry = true;
            } else {
                hasRegistry = false;
            }

            if (topics.isEmpty() && deleted.isEmpty() && scaffolds.isEmpty() && !hasRegistry) {
                getLog().info("idoc:diff: no documentation changes between " + from + " and "
                        + (GitSource.WORKTREE.equals(to) ? "the working tree" : to)
                        + " for this module — skipping packet");
                return;
            }

            String singleStampLine = (stamps && !stampsOn)
                    ? stampRegistry.singleStampLine("(packet)") : null;
            String master = PacketAssembler.masterDoc(packetTitle, from,
                    GitSource.WORKTREE.equals(to) ? "working tree" : to,
                    topics, deleted, manifest, anchorsByChange, hasRegistry, scaffolds,
                    singleStampLine, stampRegistry.usedStamps());
            Path masterFile = outputDirectory.toPath().resolve("asciidoc/review-packet.adoc");
            Files.writeString(masterFile, master, StandardCharsets.UTF_8);
            getLog().info("idoc:diff packet: " + topics.size() + " marked topics, "
                    + deleted.size() + " deleted, " + scaffolds.size()
                    + " scaffolding diffs, " + manifest.changes().size() + " change entities"
                    + (corpus ? " [corpus]" : membershipDelta != null
                            ? " [assembly " + assemblyId + "]" : ""));

            if (render) {
                renderPacket(masterFile);
            }
        } catch (IOException e) {
            throw new MojoException("idoc:diff failed: " + e.getMessage(), e);
        }
    }

    /**
     * The topic's domain — the STAMP module analogue — from its path
     * under the topics prefix.
     *
     * @param displayPath  the repository-relative topic path
     * @param topicsPrefix the topics tree prefix
     * @return the domain directory name, or {@code "(doc)"} when the
     *         path has no domain segment
     */
    private static String domainOf(String displayPath, String topicsPrefix) {
        if (!displayPath.startsWith(topicsPrefix)) {
            return "(doc)";
        }
        String rest = displayPath.substring(topicsPrefix.length());
        int slash = rest.indexOf('/');
        return slash > 0 ? rest.substring(0, slash) : "(doc)";
    }

    /**
     * Locate the topic-registry source root anywhere in the repository
     * on the to side, so an assembly module can resolve its membership.
     *
     * @param git repository access
     * @return the registry's source root (repository-relative), or
     *         {@code null} when the repository has no registry
     * @throws IOException on repository access failure
     */
    private String discoverRegistryRoot(GitSource git) throws IOException {
        String found = git.findPath(to, REGISTRY_SUFFIX);
        if (found == null) {
            return null;
        }
        return found.substring(0, found.length() - "/topic-registry.yaml".length());
    }

    private ChangeManifest resolveManifest(GitSource git, List<GitSource.Change> marked,
                                           List<GitSource.Change> own) throws IOException {
        if (changesFile != null && changesFile.isFile()) {
            return ChangeManifest.load(changesFile.toPath());
        }
        Path repoManifest = git.workTree().resolve("changes.yaml");
        if (Files.isRegularFile(repoManifest)) {
            return ChangeManifest.load(repoManifest);
        }
        if (!GitSource.WORKTREE.equals(to)) {
            return ChangeManifest.derive(git.commitsBetween(from, to));
        }
        // Working-tree review without an authored manifest: derive entities
        // from any committed part of the range (from..HEAD), then append one
        // synthetic entity for the uncommitted remainder (HEAD..WORKTREE).
        List<GitSource.CommitMeta> committed = git.commitsBetween(from, "HEAD");
        Set<String> uncommitted = new LinkedHashSet<>();
        git.changes("HEAD", GitSource.WORKTREE, "", ".adoc")
                .forEach(c -> uncommitted.add(c.displayPath()));
        if (committed.isEmpty() && uncommitted.isEmpty()) {
            Set<String> files = new LinkedHashSet<>();
            marked.forEach(c -> files.add(c.displayPath()));
            own.forEach(c -> files.add(c.displayPath()));
            uncommitted.addAll(files);
        }
        Path yaml = Files.createTempFile("doc-diff-synthetic", ".yaml");
        try {
            StringBuilder sb = new StringBuilder("changes:\n");
            if (!uncommitted.isEmpty()) {
                sb.append("  - id: chg-uncommitted\n")
                        .append("    title: \"Uncommitted changes\"\n")
                        .append("    description: >\n")
                        .append("      Working-tree changes not yet committed on top of HEAD.\n")
                        .append("    files:\n");
                uncommitted.forEach(f -> sb.append("      - ").append(f).append('\n'));
            }
            Files.writeString(yaml, sb.toString(), StandardCharsets.UTF_8);
            ChangeManifest synthetic = ChangeManifest.load(yaml);
            if (committed.isEmpty()) {
                return synthetic;
            }
            List<ChangeManifest.ChangeEntity> merged =
                    new ArrayList<>(ChangeManifest.derive(committed).changes());
            merged.addAll(synthetic.changes());
            return ChangeManifest.of(merged);
        } finally {
            Files.deleteIfExists(yaml);
        }
    }

    private List<String> ownerTitles(ChangeManifest manifest, String path,
                                     List<List<String>> anchorsByChange,
                                     AdocDiffMarker.MarkResult result) {
        List<String> owners = new ArrayList<>();
        String anchor = PacketAssembler.anchorOf(result.lines());
        List<ChangeManifest.ChangeEntity> changes = manifest.changes();
        for (int i = 0; i < changes.size(); i++) {
            if (changes.get(i).files().contains(path)) {
                owners.add(changes.get(i).title());
                if (anchor != null) {
                    anchorsByChange.get(i).add(anchor);
                }
            }
        }
        return owners;
    }

    private boolean writeRegistryDelta(GitSource git, String srcRel, Path diffDir)
            throws IOException {
        String root = srcRel + "/topic-registry.yaml";
        if (git.read(from, root) == null && git.read(to, root) == null) {
            return false;
        }
        String delta = new RegistryDelta(git, from, to)
                .render(root, srcRel + "/topic-registry");
        if (delta.isEmpty()) {
            return false;
        }
        Files.writeString(diffDir.resolve("registry-delta.adoc"), delta, StandardCharsets.UTF_8);
        return true;
    }

    private void renderPacket(Path masterFile) {
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.registerLogHandler(new AsciidocMojo.MavenLogHandler(getLog()));
            asciidoctor.requireLibrary("asciidoctor-diagram");
            asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);

            Path htmlDir = outputDirectory.toPath().resolve("html");
            convert(asciidoctor, masterFile, "html5", htmlDir, baseAttributes().build());

            AttributesBuilder pdfAttrs = baseAttributes();
            Path theme = resolvePdfTheme();
            if (theme != null) {
                pdfAttrs.attribute("pdf-theme", theme.toAbsolutePath().toString());
            }
            if (pdfFontsDir != null && pdfFontsDir.isDirectory()) {
                pdfAttrs.attribute("pdf-fontsdir", pdfFontsDir.getAbsolutePath());
            }
            Path pdfDir = outputDirectory.toPath().resolve("pdf");
            convert(asciidoctor, masterFile, "pdf", pdfDir, pdfAttrs.build());
        }
    }

    /**
     * Resolve the prawn theme for this run, making the goal
     * self-contained (ike-issues#649): an explicit/unpacked theme wins;
     * otherwise the branded theme is extracted from the plugin's own
     * classpath when the pipeline fonts are present; otherwise a
     * bundled fallback theme (stock typography, IKE diff roles) keeps a
     * bare {@code mvn idoc:diff} fully styled on a fresh checkout.
     *
     * @return the theme file to use, or {@code null} when even the
     *         fallback cannot be provisioned
     */
    private Path resolvePdfTheme() {
        if (pdfTheme != null && pdfTheme.isFile()) {
            return pdfTheme.toPath();
        }
        boolean fontsAvailable = pdfFontsDir != null && pdfFontsDir.isDirectory();
        String resource = fontsAvailable
                ? "/theme/ike-default-theme.yml"
                : "/diff/diff-fallback-theme.yml";
        try {
            Path dir = outputDirectory.toPath().resolve(".theme");
            Files.createDirectories(dir);
            Path target = dir.resolve("ike-default-theme.yml");
            try (InputStream in = DocDiffMojo.class.getResourceAsStream(resource)) {
                if (in == null) {
                    getLog().warn("idoc:diff: theme resource " + resource
                            + " not on plugin classpath — PDF renders unthemed");
                    return null;
                }
                Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            getLog().info("idoc:diff theme: " + (fontsAvailable
                    ? "branded (self-provisioned from ike-doc-resources)"
                    : "fallback (stock typography + diff roles; run after a "
                            + "pipeline build for branded output)"));
            return target;
        } catch (IOException e) {
            getLog().warn("idoc:diff: theme provisioning failed — PDF renders unthemed: "
                    + e.getMessage());
            return null;
        }
    }

    private AttributesBuilder baseAttributes() {
        return Attributes.builder()
                .attribute("toc", "left")
                .attribute("toclevels", "1")
                .attribute("icons", "font")
                .attribute("source-highlighter", "rouge")
                .attribute("allow-uri-read", "true")
                // Diagram rendering — same recipe as the pipeline render
                // (kroki server, inline SVG, no PlantUML preprocessing).
                .attribute("diagram-server-url", diagramServerUrl)
                .attribute("diagram-server-type", "kroki_io")
                .attribute("diagram-format", "svg")
                .attribute("diagram-svg-type", "inline")
                .attribute("plantuml-preprocess", "false")
                .attribute("preprocess", "false");
    }

    private void convert(Asciidoctor asciidoctor, Path masterFile, String backend,
                         Path outDir, Attributes attrs) {
        try {
            Files.createDirectories(outDir);
            Options options = Options.builder()
                    .backend(backend)
                    .safe(SafeMode.UNSAFE)
                    .baseDir(masterFile.getParent().toFile())
                    .toDir(outDir.toFile())
                    .mkDirs(true)
                    .standalone(true)
                    .attributes(attrs)
                    .build();
            asciidoctor.convertFile(masterFile.toFile(), options);
            getLog().info("idoc:diff rendered " + backend + " -> " + outDir);
        } catch (RuntimeException | IOException e) {
            getLog().error("idoc:diff " + backend + " render failed — packet sources remain at "
                    + masterFile.getParent() + ": " + e.getMessage());
        }
    }

    private static List<String> lines(String text) {
        return text == null ? List.of() : Arrays.asList(text.split("\n", -1));
    }
}
