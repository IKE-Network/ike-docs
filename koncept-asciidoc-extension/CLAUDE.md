# Koncept AsciiDoc Extension — Claude Standards

## Initial Setup — ALWAYS DO THIS FIRST

Run `mvn validate` before any other work. This unpacks current build
standards into `.claude/standards/`. Do not proceed without this step.

If `mvn validate` fails because `ike-build-standards` is not in the
local repository, install it first:

```bash
mvn install -f ../ike-build-standards/pom.xml
```

After validate completes, read and follow these files in `.claude/standards/`:

- MAVEN.md — Maven 4 build standards (always read)
- IKE-MAVEN.md — IKE-specific Maven conventions (always read)

Read these additional files when working on Java code:

- JAVA.md — Java standards
- IKE-JAVA.md — IKE-specific Java patterns

## Module Overview

AsciidoctorJ SPI extension providing the `k:Name[]` inline macro for
formally identified terminology concepts. Renders SVG badges for HTML/PDF
and DocBook phrases for XSL-FO. Auto-generates a glossary section with
definitions, axioms, and SNOMED CT identifiers.

- **Artifact**: `network.ike.docs:koncept-asciidoc-extension`
- **Packaging**: JAR
- **Java**: 25 with preview (inherited from parent)
- **Registration**: SPI via `META-INF/services`

## Key Conventions

- InlineMacro is registered via SPI (works with all backends)
- Postprocessor is registered per-execution in asciidoctor-maven-plugin
  config (NOT via SPI — crashes Prawn backend)
- Koncept definitions live in `src/main/resources/koncepts.yml`
- SVG badges rendered for HTML; DocBook phrases for FOP/XEP; plain
  text for Prawn
- Java version: 25 with preview (inherited from parent)

## Build

```bash
mvn install
```
