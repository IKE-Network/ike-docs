package network.ike.docs.plugin.packages;

import network.ike.docs.plugin.registry.TopicHeader;
import network.ike.docs.plugin.registry.TopicRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The ownership lint of the packages layout ({@code dev-adoc-packages},
 * IKE-Network/ike-issues#1341), the check that makes the layout a
 * guarantee and not a habit. It fails the build with every finding listed:
 *
 * <ul>
 *   <li>the root holds nothing but dotted-group directories and
 *       {@code META-INF}: a file at the root, an {@code images} directory
 *       at the root, or a directory that is not a group is a finding;</li>
 *   <li>every package begins with the module's own group, unless
 *       {@code ike.adoc.packages} declares another prefix on purpose;</li>
 *   <li>a topic's directory equals the domain prefix of its
 *       {@code :topic-id:}, so that a person who knows a topic's id and its
 *       owner can write its include line without looking;</li>
 *   <li>images are inside packages: an {@code images} directory directly
 *       under a group that is not itself a package is a finding.</li>
 * </ul>
 *
 * <p>An include or image line that still uses the {@code {topics}} alias
 * is reported as a warning naming the file and line, so the alias can be
 * retired once the last one is gone; it does not fail the build.
 *
 * @since 136
 */
public final class PackageLint {

    /** The include attribute of the packages layout. */
    public static final String ATTRIBUTE = "{adoc}";

    /** The migration alias the lint reports. */
    public static final String ALIAS = "{topics}";

    /**
     * What the lint found.
     *
     * @param errors   findings that fail the build, in the order found
     * @param warnings findings that do not, in the order found
     */
    public record Report(List<String> errors, List<String> warnings) {

        /**
         * Whether the build passes.
         *
         * @return true when there is no error
         */
        public boolean clean() {
            return errors.isEmpty();
        }
    }

    private PackageLint() {
    }

    /**
     * Lint a root.
     *
     * @param root     the AsciiDoc root
     * @param groupId  the module's group id, which every package begins with
     * @param declared package names or group prefixes
     *                 {@code ike.adoc.packages} declares on purpose; a package
     *                 passes the ownership rule when it equals one or begins
     *                 with one followed by {@code /} or {@code .}
     * @return the report
     * @throws IOException if the tree cannot be read
     */
    public static Report lint(Path root, String groupId, List<String> declared) throws IOException {
        Path real = root.toRealPath();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        lintRoot(real, errors);
        List<AdocPackages.Package> packages = AdocPackages.packages(real, null);
        for (AdocPackages.Package p : packages) {
            if (!owned(p.name(), groupId, declared)) {
                errors.add("package " + p.name() + " does not begin with the module's group " + groupId
                        + "; move it to a library of its own, or declare the prefix in ike.adoc.packages on purpose");
            }
            for (TopicHeader topic : p.topics()) {
                String domain = TopicRegistry.domainOf(topic.id());
                if (!domain.equals(p.domain())) {
                    errors.add(topic.file() + ": topic " + topic.id() + " is in package " + p.name()
                            + " but its id's domain is " + domain + "; move it to the " + domain
                            + " package or rename it " + p.domain() + "-...");
                }
            }
        }
        for (Path group : AdocPackages.groupDirectories(real)) {
            if (AdocPackages.isDomainPackage(group)) {
                continue;
            }
            Path images = group.resolve(AdocPackages.IMAGES);
            if (Files.isDirectory(images)) {
                errors.add(real.relativize(images).toString().replace('\\', '/')
                        + ": images outside a package; move them into the " + AdocPackages.IMAGES
                        + " directory of the package that uses them");
            }
        }
        for (TopicHeader header : AdocPackages.headers(real, real)) {
            lintAlias(real, header.file(), warnings);
        }
        return new Report(List.copyOf(errors), List.copyOf(warnings));
    }

    /** The root holds nothing but dotted-group directories and META-INF; hidden entries are the editor's. */
    private static void lintRoot(Path real, List<String> errors) throws IOException {
        for (Path entry : AdocPackages.children(real)) {
            String name = entry.getFileName().toString();
            if (name.startsWith(".") || TopicRegistry.SKIPPED_DIRS.contains(name)) {
                continue;
            }
            if (Files.isDirectory(entry)) {
                if (name.equals(AdocPackages.META_INF) || AdocPackages.GROUP.matcher(name).matches()) {
                    continue;
                }
                if (name.equals(AdocPackages.IMAGES)) {
                    errors.add(name + "/: images at the root; move them into the " + AdocPackages.IMAGES
                            + " directory of the package that uses them");
                } else {
                    errors.add(name + "/: not a dotted group directory; the root holds only <group>/<domain>"
                            + " packages and " + AdocPackages.META_INF);
                }
            } else if (name.endsWith(".adoc")) {
                errors.add(name + ": a file at the root; a topic belongs in <group>/<domain>/, the preview"
                        + " index in the library's own package");
            } else {
                errors.add(name + ": a file at the root; the root holds only <group>/<domain> packages and "
                        + AdocPackages.META_INF);
            }
        }
    }

    /**
     * Whether a package passes the ownership rule.
     *
     * @param name     the package name
     * @param groupId  the module's group id
     * @param declared the prefixes {@code ike.adoc.packages} declares
     * @return true when the package begins with the group or a declared prefix
     */
    public static boolean owned(String name, String groupId, List<String> declared) {
        if (begins(name, groupId)) {
            return true;
        }
        for (String prefix : declared) {
            if (begins(name, prefix.strip())) {
                return true;
            }
        }
        return false;
    }

    private static boolean begins(String name, String prefix) {
        return !prefix.isEmpty() && (name.equals(prefix) || name.startsWith(prefix + "/")
                || name.startsWith(prefix + "."));
    }

    /** An include or image line still using the {topics} alias is a warning naming the line. */
    private static void lintAlias(Path real, String file, List<String> warnings) throws IOException {
        List<String> lines = Files.readString(real.resolve(file), StandardCharsets.UTF_8).lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("//") || !line.contains(ALIAS)) {
                continue;
            }
            if (line.startsWith("include::") || line.startsWith("image::") || line.contains("image:")) {
                warnings.add(file + ":" + (i + 1) + ": still uses " + ALIAS + ", replace it with " + ATTRIBUTE
                        + ": " + line.strip());
            }
        }
    }
}
