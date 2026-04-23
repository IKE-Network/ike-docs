# IKE Minimal Fonts — Claude Standards

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

Downloads 13 Noto TTF fonts from GitHub releases and packages them
as a ZIP artifact. No fonts are committed to git — `mvn install`
is fully self-sufficient. Downloads are cached after first fetch.

- **Artifact**: `network.ike.docs:minimal-fonts` (type: zip)
- **Packaging**: POM + assembly (produces ZIP containing TTF files at root)
- **License**: SIL Open Font License 1.1

## Key Conventions

- Font files are downloaded at `generate-resources` phase, not committed
- Cached in `~/.m2/repository/.cache/download-maven-plugin/`
- 13 TTFs: NotoSerif (4), NotoSans (4), NotoSansMono (2),
  NotoSansMath (1), NotoSansSymbols (1), NotoSansSymbols2 (1)
- This is a standalone module — does not inherit from `ike-parent`

## Build

```bash
mvn install
```
