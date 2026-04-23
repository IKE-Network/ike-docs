# IKE Docs

Documentation plumbing for the IKE Community. Provides the
`ike-doc-maven-plugin` (`idoc:*` goals + `ike-doc` packaging), the
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
| [`ike-doc-maven-plugin`](ike-doc-maven-plugin) | `network.ike.docs:ike-doc-maven-plugin` | `idoc:*` goals and `ike-doc` packaging (extensions=true) |
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
ike-tooling  →  ike-docs  →  ike-platform  →  { doc-example, example-project }  →  ike-example-ws
```

`ike-docs` releases **before** `ike-platform` because `ike-platform`'s
`ike-parent` pins `${ike-docs.version}` as a literal value and uses
`<extensions>true</extensions>` to activate the `ike-doc` packaging.

## License

Apache License 2.0. See [LICENSE](LICENSE).
