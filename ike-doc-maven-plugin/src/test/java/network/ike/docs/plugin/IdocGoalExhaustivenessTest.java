package network.ike.docs.plugin;

import org.apache.maven.api.plugin.annotations.Mojo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-reference check: {@link IdocGoal} must stay in lockstep with
 * the {@link Mojo @Mojo} declarations on this plugin's mojo classes.
 *
 * <p>Two failure modes are both guarded:
 * <ul>
 *   <li>An enum entry's {@code goalName} doesn't match its mojo class's
 *       {@code @Mojo(name = ...)}.</li>
 *   <li>A new {@code @Mojo} class is added without a matching enum
 *       entry, or vice versa.</li>
 * </ul>
 *
 * <p>Parallels {@code IkeGoalExhaustivenessTest} in ike-maven-plugin.
 * Introduced by ike-issues #215.
 */
class IdocGoalExhaustivenessTest {

    private static final Pattern MOJO_NAME = Pattern.compile(
            "@(?:[\\w.]*\\.)?Mojo\\s*\\(\\s*name\\s*=\\s*\"([a-z][a-z0-9-]*)\"");

    @Test
    void every_enum_entry_matches_its_mojo_annotation() {
        for (IdocGoal goal : IdocGoal.values()) {
            Mojo annotation = goal.mojoClass().getAnnotation(Mojo.class);
            assertThat(annotation)
                    .withFailMessage("IdocGoal.%s mojoClass %s has no @Mojo annotation",
                            goal.name(), goal.mojoClass().getSimpleName())
                    .isNotNull();
            assertThat(annotation.name())
                    .withFailMessage("IdocGoal.%s declares goalName=\"%s\""
                                    + " but @Mojo on %s says \"%s\"",
                            goal.name(), goal.goalName(),
                            goal.mojoClass().getSimpleName(), annotation.name())
                    .isEqualTo(goal.goalName());
        }
    }

    @Test
    void enum_names_cover_every_mojo_in_source() throws IOException {
        Set<String> fromSource = scanMojoNamesInSource();
        Set<String> fromEnum = Arrays.stream(IdocGoal.values())
                .map(IdocGoal::goalName)
                .collect(Collectors.toSet());

        Set<String> missingFromEnum = new HashSet<>(fromSource);
        missingFromEnum.removeAll(fromEnum);
        Set<String> missingFromSource = new HashSet<>(fromEnum);
        missingFromSource.removeAll(fromSource);

        assertThat(missingFromEnum)
                .withFailMessage("@Mojo names without an IdocGoal entry: %s",
                        missingFromEnum)
                .isEmpty();
        assertThat(missingFromSource)
                .withFailMessage("IdocGoal entries without a matching @Mojo"
                                + " in source: %s",
                        missingFromSource)
                .isEmpty();
    }

    private static Set<String> scanMojoNamesInSource() throws IOException {
        Path srcMain = Path.of("src/main/java");
        Set<String> names = new HashSet<>();
        try (Stream<Path> files = Files.walk(srcMain)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (!Files.isRegularFile(file)
                        || !file.toString().endsWith(".java")) {
                    continue;
                }
                Matcher m = MOJO_NAME.matcher(Files.readString(file));
                while (m.find()) {
                    names.add(m.group(1));
                }
            }
        }
        return names;
    }
}
