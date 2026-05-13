# IKE Docs

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Documentation](https://img.shields.io/badge/docs-ike.network%2Fike--docs-blue)](https://ike.network/ike-docs/)
[![IKE Network](https://img.shields.io/badge/IKE-Network-green)](https://ike.network/)

Documentation plumbing for the IKE Network. Provides the
`ike-doc-maven-plugin` (`idoc:*` render and packaging goals), the
Koncept AsciiDoc extension, DocBook XSL + fonts, shared doc
resources, and a semantic linebreak reformatter.

Split from the archived `ike-pipeline` repo. See
[`IKE-Network/ike-issues#216`](https://github.com/IKE-Network/ike-issues/issues/216)
for the architectural rationale.

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| [`ike-doc-resources`](ike-doc-resources) | `network.ike.docs:ike-doc-resources` | Shared build resources (themes, assembly descriptors, renderer configs) |
| [`minimal-fonts`](minimal-fonts) | `network.ike.docs:minimal-fonts` | Noto font subset for PDF rendering (ZIP) |
| [`docbook-xsl`](docbook-xsl) | `network.ike.docs:docbook-xsl` | DocBook XSL 1.79.2 + IKE FO customization |
| [`koncept-asciidoc-extension`](koncept-asciidoc-extension) | `network.ike.docs:koncept-asciidoc-extension` | AsciidoctorJ `k:Name[]` inline macro + glossary |
| [`ike-doc-maven-plugin`](ike-doc-maven-plugin) | `network.ike.docs:ike-doc-maven-plugin` | `idoc:*` goals: AsciiDoc rendering, multi-renderer PDF wrappers, doc packaging utilities |
| [`semantic-linebreak`](semantic-linebreak) | `network.ike.docs:semantic-linebreak` | AsciiDoc one-sentence-per-line reformatter |

## Build

```bash
mvn clean install
```

Requires Java 25 and a Maven 4 wrapper (`./mvnw`). The `ike-build-standards`
artifact (from `network.ike.tooling`) is fetched automatically at
`validate` and unpacks Claude standards into `.claude/standards/`
for each module.

## Release Position

```
ike-tooling  →  ike-docs  →  ike-platform  →  { doc-example, example-project, ike-example-its }  →  ike-example-ws
```

`ike-docs` releases **before** `ike-platform` because `ike-platform`'s
`ike-parent` declares `ike-doc-maven-plugin` and the other ike-docs
artifacts (themes, fonts, DocBook XSL, koncept extension) in its
`<pluginManagement>` and `<dependencyManagement>` at
`${ike-docs.version}`. Those artifacts must be resolvable from
Nexus when downstream reactors load.

The cascade ordering is structurally upstream-first; it is not
driven by extension-realm timing. Earlier revisions of these docs
cited `<extensions>true</extensions>` and literal-version pinning
as the reason — that constraint was eliminated in
[`IKE-Network/ike-issues#321`](https://github.com/IKE-Network/ike-issues/issues/321)
when `ike-doc-maven-plugin` retired its `<packaging>ike-doc</packaging>`
custom type in favor of a classifier-canonical doc shape. See
[`ike-doc-maven-plugin/src/site/asciidoc/index.adoc`](ike-doc-maven-plugin/src/site/asciidoc/index.adoc)
for the full design rationale.

## Links

- **Documentation:** [`https://ike.network/ike-docs/`](https://ike.network/ike-docs/)
- **Issues:** [`IKE-Network/ike-issues`](https://github.com/IKE-Network/ike-issues) (cross-project tracker)
- **Source:** [`IKE-Network/ike-docs`](https://github.com/IKE-Network/ike-docs)

## License

Apache License 2.0. See the [project pom.xml](pom.xml) or
[apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0).
