package network.ike.docs.plugin.packages;

import network.ike.docs.plugin.registry.TopicHeader;
import network.ike.docs.plugin.registry.TopicRegistry;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * The packages layout of an AsciiDoc root ({@code dev-adoc-packages},
 * IKE-Network/ike-issues#1341): the first directory under the root is the
 * owning group written as one dotted segment, {@code network.ike.lab}, and
 * the directory under that is the topic domain, so a topic is
 * {@code network.ike.lab/dev/gitflow-overview.adoc} and its package is
 * {@code network.ike.lab/dev}. A library whose group ends in its only
 * domain may use the group directory itself as the domain package: the
 * ANF kompendium's topics are {@code network.ike.kompendium.anf/determine-topic.adoc},
 * in the package {@code network.ike.kompendium.anf}. {@code META-INF} is
 * reserved for what describes the artifact; every group begins with a
 * top-level domain, so the two never meet.
 *
 * <p>This class enumerates the packages a root holds and what each holds,
 * the reading the manifest and the lint share.
 *
 * @since 136
 */
public final class AdocPackages {

    /** The reserved directory for artifact metadata, never a package. */
    public static final String META_INF = "META-INF";

    /** The directory inside a package that holds its images. */
    public static final String IMAGES = "images";

    /**
     * A group directory's name: a reverse-DNS group as one dotted segment,
     * at least two labels of lowercase letters, digits, hyphens and
     * underscores, as a Maven group id is written.
     */
    public static final Pattern GROUP = Pattern.compile("[a-z0-9_-]+(\\.[a-z0-9_-]+)+");

    /**
     * One package of a root.
     *
     * @param name      the package name, {@code <group>/<domain>}, or the
     *                  group alone when the group directory is the domain
     * @param domain    the topic domain the package's topics must carry as
     *                  the prefix of their id
     * @param directory the package directory
     * @param kind      what the package holds
     * @param files     every {@code .adoc} file under the package, parsed,
     *                  its path relative to the root, in path order
     */
    public record Package(String name, String domain, Path directory, PackageKind kind, List<TopicHeader> files) {

        /**
         * The files that declare a {@code :topic-id:}.
         *
         * @return the topics, in path order
         */
        public List<TopicHeader> topics() {
            return files.stream().filter(TopicHeader::topic).toList();
        }
    }

    private AdocPackages() {
    }

    /**
     * The metadata directory of an artifact inside the zip and the union,
     * keyed by its coordinates so that it cannot collide:
     * {@code META-INF/ike/<groupId>/<artifactId>}.
     *
     * @param groupId    the artifact's group id
     * @param artifactId the artifact's artifact id
     * @return the relative path with {@code /} separators
     */
    public static String metaDirectory(String groupId, String artifactId) {
        return META_INF + "/ike/" + groupId + "/" + artifactId;
    }

    /**
     * The group directories of a root: its immediate subdirectories whose
     * name is a dotted group.
     *
     * @param root the AsciiDoc root
     * @return the directories in name order
     * @throws IOException if the root cannot be listed
     */
    public static List<Path> groupDirectories(Path root) throws IOException {
        List<Path> groups = new ArrayList<>();
        for (Path entry : children(root)) {
            if (Files.isDirectory(entry) && GROUP.matcher(entry.getFileName().toString()).matches()) {
                groups.add(entry);
            }
        }
        return groups;
    }

    /**
     * Whether a group directory is itself the domain package: it holds an
     * {@code .adoc} file directly, as the ANF library's group directory does.
     *
     * @param group the group directory
     * @return true when the group directory holds topics directly
     * @throws IOException if the directory cannot be listed
     */
    public static boolean isDomainPackage(Path group) throws IOException {
        for (Path entry : children(group)) {
            if (Files.isRegularFile(entry) && entry.getFileName().toString().endsWith(".adoc")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enumerate the packages of a root: for each group directory, the group
     * itself when it holds topics directly, otherwise each subdirectory of
     * it as {@code <group>/<domain>}, hidden directories left out. Every
     * {@code .adoc} file under a package is parsed for its header, which
     * is what the kind and the lint's domain rule are decided by.
     *
     * @param root           the AsciiDoc root
     * @param libraryPackage the package {@code ike.adoc.library-package}
     *                       names as the library's own, or {@code null}
     * @return the packages in name order
     * @throws IOException if the tree cannot be read
     */
    public static List<Package> packages(Path root, String libraryPackage) throws IOException {
        Path real = root.toRealPath();
        TreeMap<String, Package> packages = new TreeMap<>();
        for (Path group : groupDirectories(real)) {
            String groupName = group.getFileName().toString();
            if (isDomainPackage(group)) {
                String domain = groupName.substring(groupName.lastIndexOf('.') + 1);
                packages.put(groupName, read(real, groupName, domain, group, libraryPackage));
                continue;
            }
            for (Path domainDir : children(group)) {
                String domain = domainDir.getFileName().toString();
                if (!Files.isDirectory(domainDir) || domain.startsWith(".")) {
                    continue;
                }
                String name = groupName + "/" + domain;
                packages.put(name, read(real, name, domain, domainDir, libraryPackage));
            }
        }
        return List.copyOf(packages.values());
    }

    private static Package read(Path real, String name, String domain, Path directory, String libraryPackage)
            throws IOException {
        List<TopicHeader> files = headers(real, directory);
        boolean index = Files.isRegularFile(directory.resolve("index.adoc"));
        boolean topics = files.stream().anyMatch(TopicHeader::topic);
        PackageKind kind = name.equals(libraryPackage) || (index && !topics)
                ? PackageKind.LIBRARY : PackageKind.TOPICS;
        return new Package(name, domain, directory, kind, files);
    }

    /**
     * Parse the header of every {@code .adoc} file under a directory, in
     * path order, paths relative to the root; symbolic links and the
     * directories {@link TopicRegistry#SKIPPED_DIRS} are left out.
     *
     * @param root      the AsciiDoc root paths are relative to
     * @param directory the directory to walk, under the root
     * @return the parsed headers
     * @throws IOException if a file cannot be read
     */
    public static List<TopicHeader> headers(Path root, Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(directory) && TopicRegistry.SKIPPED_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && !attrs.isSymbolicLink()
                        && file.getFileName().toString().endsWith(".adoc")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort((a, b) -> root.relativize(a).toString().compareTo(root.relativize(b).toString()));
        List<TopicHeader> headers = new ArrayList<>();
        for (Path file : files) {
            headers.add(TopicHeader.parse(file, root));
        }
        return List.copyOf(headers);
    }

    /**
     * The immediate entries of a directory in name order.
     *
     * @param directory the directory
     * @return its entries
     * @throws IOException if it cannot be listed
     */
    public static List<Path> children(Path directory) throws IOException {
        List<Path> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                entries.add(entry);
            }
        }
        entries.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        return entries;
    }
}
