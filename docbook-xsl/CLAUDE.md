# IKE DocBook XSL — Claude Standards

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

## Module Overview

Packages DocBook XSL 1.79.2 stylesheets together with the IKE FO
customization layer (`ike-fo.xsl`) as a single Maven JAR artifact.
Used by the XEP and FOP PDF renderer profiles.

- **Artifact**: `network.ike.docs:docbook-xsl`
- **Packaging**: JAR
- **Version scheme**: `{docbook-xsl-version}-ike.{revision}`

## Key Conventions

- DocBook XSL is downloaded from GitHub at build time, not committed
- The IKE customization layer lives in `src/main/xslt/ike-fo.xsl`
- `ike-fo.xsl` uses `<xsl:import href="../fo/docbook.xsl"/>` — this
  resolves because `custom/` sits alongside the stock `fo/` directory
- Bump the `ike.N` suffix when `ike-fo.xsl` changes
- Bump the base version when upgrading DocBook XSL
- This is a standalone module — does not inherit from `ike-parent`

## Build

```bash
mvn install
```
