# Semantic Linebreak — Claude Standards

## Initial Setup — ALWAYS DO THIS FIRST

Run `mvn validate` before any other work. This unpacks current build
standards into `.claude/standards/`. Do not proceed without this step.

If `mvn validate` fails because `ike-build-standards` is not in the
local repository, install infrastructure modules first:

```bash
mvn install -pl ike-build-standards -f ../pom.xml
```

After validate completes, read and follow these files in `.claude/standards/`:

- MAVEN.md — Maven 4 build standards (always read)
- IKE-MAVEN.md — IKE-specific Maven conventions (always read)
- JAVA.md — Java 25 standards (always read)
- IKE-JAVA.md — IKE-specific Java patterns

## Module Overview

CLI tool that reformats AsciiDoc prose paragraphs to use semantic
linefeeds (one sentence per line). Uses AsciidoctorJ to parse the
document AST, ensuring only paragraph blocks are reformatted while
listings, diagrams, tables, and other block types are preserved.

- **Artifact**: `network.ike.docs:semantic-linebreak`
- **Packaging**: JAR (executable)
- **Parent**: `ike-parent`

## Key Build Commands

```bash
# Build:
mvn clean install -pl semantic-linebreak

# Run (dry-run mode):
mvn exec:java -pl semantic-linebreak -Dexec.args="-n path/to/file.adoc"

# Run (in-place reformat):
mvn exec:java -pl semantic-linebreak -Dexec.args="path/to/file.adoc"
```
