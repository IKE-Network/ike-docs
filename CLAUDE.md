# IKE Docs — Claude Standards

## Initial Setup — ALWAYS DO THIS FIRST

Run `mvn validate` before any other work. This unpacks the current
build standards into `.claude/standards/` for each module via
`ike-build-standards` (from `network.ike.tooling`, unpacked through
`maven-dependency-plugin`). Do not proceed without this step.

If `mvn validate` fails because `ike-build-standards` is not in the
local repository, either fetch it from Nexus or install it from the
`ike-tooling` workspace:

```bash
# Via Nexus (default):
mvn dependency:resolve -Dartifact=network.ike.tooling:ike-build-standards:${ike-tooling.version}:zip

# Or locally from the ike-tooling checkout:
mvn install -pl ike-build-standards -f ../../pipeline-ws/ike-tooling/pom.xml
```

After validate completes, read and follow these files in `.claude/standards/`:

- MAVEN.md — Maven 4 build standards (always read)
- IKE-MAVEN.md — IKE-specific Maven conventions (always read)

Read these additional files when working on Java code:

- JAVA.md — Java 25 standards
- IKE-JAVA.md — IKE-specific Java patterns

Do not read other files in that directory unless specifically relevant
to a task you are performing.

## Project Overview

This is **IKE Docs** — a Maven 4 reactor that hosts the IKE
documentation plumbing. It produces the artifacts that external doc
projects (e.g., `ike-lab-documents`, `doc-example`, `example-project`)
consume through `ike-parent` (which lives in `ike-platform`).

Split from the archived `ike-pipeline` repo to resolve a fundamental
Maven `<extensions>true</extensions>` reactor-load cycle. See
`dev-ike-repo-split-architecture` in `ike-lab-documents/topics/` and
`IKE-Network/ike-issues#216`.

### Module Structure

Subprojects are built in dependency order:

| Module | Purpose | Packaging |
|---|---|---|
| `ike-doc-resources` | Shared doc build resources (themes, configs, assembly descriptors) | JAR |
| `minimal-fonts` | Noto font subset for PDF rendering | ZIP (pom) |
| `docbook-xsl` | DocBook XSL 1.79.2 + IKE FO customization | JAR |
| `koncept-asciidoc-extension` | AsciidoctorJ `k:Name[]` inline macro + glossary | JAR |
| `ike-doc-maven-plugin` | `idoc:*` goals, `ike-doc` packaging handler (extensions=true) | maven-plugin |
| `semantic-linebreak` | CLI — AsciiDoc semantic linefeed reformatter | maven-plugin |

### The extensions=true Story

`ike-doc-maven-plugin` declares the `ike-doc` custom packaging type
through `META-INF/plexus/components.xml`. Consumers of `ike-parent`
(in `ike-platform`) pick up this plugin declared with
`<extensions>true</extensions>` and can therefore use
`<packaging>ike-doc</packaging>`.

The plugin **must be released from this repo before `ike-platform`
can build**, because `ike-parent`'s extension declaration resolves
the plugin JAR from Nexus at project-load time. This is the
repository-level fix for the cycle that `ike-pipeline` could not
resolve when the plugin was a sibling reactor module.

### Dependencies on Other Repos

- `network.ike.tooling:ike-maven-plugin` — release orchestration, BOM
  generation, site deploy, AsciiDoc utilities. Declared at literal
  version `${ike-tooling.version}` in the root `<pluginManagement>`.
- `network.ike.tooling:ike-maven-plugin-support` — shared `GoalRef`,
  `AbstractGoalMojo`, etc., consumed by `ike-doc-maven-plugin`.
- `network.ike.tooling:ike-build-standards` — versioned Claude
  instruction files + build config ZIPs.

## Key Build Commands

```bash
# Full reactor:
mvn clean install

# Only the plugin and its deps:
mvn install -pl ike-doc-maven-plugin -am

# Skip tests during fast iteration:
mvn install -DskipTests
```

## Project-Specific Context

- Group ID: `network.ike.docs`
- Model version: `4.1.0` for all POMs
- Java version: 25 (as ike-doc-maven-plugin, koncept extension, and
  semantic-linebreak require Java 25 runtime)
- Version strategy: single-segment integer (starts at 1). Not semver.
- All subprojects are versionless — root version is the single source
  of truth.

## Release Cascade Position

```
ike-tooling → [ike-docs] → ike-platform → { doc-example, example-project } → ike-example-ws
```

`ike-docs` must release before `ike-platform`. `ike-platform`'s
`ike-parent` pins `${ike-docs.version}` as a literal value.

## `.mvn/jvm.config` constraints

Maven's `.mvn/jvm.config` is parsed as raw JVM arguments — one token
per line, NO comment syntax. A `#` at column 0 is passed to the JVM
as if it were a main-class name, and IntelliJ will show:

```
Error: Could not find or load main class #
Caused by: java.lang.ClassNotFoundException: #
```

Do NOT add `#`-prefixed comments to `.mvn/jvm.config`. The current
file contains exactly one argument:

- `--sun-misc-unsafe-memory-access=allow` — suppresses the JFFI
  `sun.misc.Unsafe` deprecation warnings emitted by
  JRuby/AsciidoctorJ on Java 24+.

Also do NOT set `-Denv.PATH` or PATH-related options here or in
`MAVEN_OPTS`: PATH entries containing spaces (e.g. JetBrains
Toolbox) cause the JVM launcher to bail with the same
"Could not find or load main class" error for an unrelated reason.

## Workspace Tooling

`ike-workspace-maven-plugin` (prefix `ws:`) lives in `ike-platform`.
This repo does not declare it — `ws:*` goals are used from the
workspace aggregator to orchestrate cross-repo releases, not from
inside `ike-docs` during its own build.

`ike-maven-plugin` (prefix `ike:`) is used as normal — `ike:prepare-release`,
`ike:release-status`, etc., drive the release of this repo.
