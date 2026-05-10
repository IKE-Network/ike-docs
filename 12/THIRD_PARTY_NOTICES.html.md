---
date_published: 2026-05-09
date_modified: 2026-05-09
canonical_url: https://github.com/IKE-Network/ike-docs/THIRD_PARTY_NOTICES.html
---

# IKE Docs — Third-Party Notices

This page is the curated companion to two mechanical inventories that ship alongside it:

- [Software Bill of Materials (CycloneDX, JSON)](bom.json)[1] — full transitive dependency graph with SPDX-normalized licenses and artifact hashes. Ingestible by Dependency-Track, Trivy, Snyk, GitHub’s dependency graph, etc. Also reachable as a Maven artifact with `<classifier>cyclonedx</classifier>`.
- [Maven dependencies report](dependencies.html)[2] — auto-generated HTML browse of declared dependencies, with verbatim per-license text from each dependency’s POM.

The curated content below covers what neither sees — the AsciiDoc rendering chain, embedded fonts, DocBook XSL toolchain, diagram rendering, external PDF renderers, and frontend assets bundled into rendered HTML. License identifiers below are SPDX form (`Apache-2.0`, `MIT`, `EPL-2.0`, expressions with `OR` / `AND` / `WITH`) so they’re unambiguous and grep-friendly.

For corresponding notices in the rest of the IKE platform see:

- `[ike-tooling](../ike-tooling/THIRD_PARTY_NOTICES.html)[3]` — Maven build infrastructure, plugin core, signing.
- `[ike-platform](../ike-platform/THIRD_PARTY_NOTICES.html)[4]` — Java toolchain, BOM-managed dependencies, test framework.

## [#asciidoc-rendering-toolchain](#asciidoc-rendering-toolchain)AsciiDoc rendering toolchain

| Component | License | Role |
| --- | --- | --- |
| [AsciidoctorJ](https://asciidoctor.org/)[5] | `MIT` | The Java entry-point for the Asciidoctor renderer. `ike-doc-maven-plugin` invokes it via the official Maven plugin to convert `.adoc` source to HTML, DocBook, and XHTML. |
| [AsciidoctorJ PDF](https://github.com/asciidoctor/asciidoctorj-pdf)[6] | `MIT` | Direct PDF rendering of AsciiDoc. The `prawn` profile in `ike-parent` activates this renderer; output is post-processed for page-break and font handling. |
| [AsciidoctorJ Diagram](https://github.com/asciidoctor/asciidoctorj-diagram)[7] | `MIT` | Inline diagram rendering. Used together with the Kroki extension to support PlantUML, Graphviz, Mermaid, and similar formats inside AsciiDoc source. |
| [Asciidoctor Maven Plugin](https://github.com/asciidoctor/asciidoctor-maven-plugin)[8] | `MIT` | The Maven plugin wrapper that drives AsciidoctorJ from the build. |
| [Asciidoctor Parser Doxia Module](https://github.com/asciidoctor/asciidoctor-doxia)[9] | `MIT` | Lets `maven-site-plugin` natively render `src/site/asciidoc/*.adoc` alongside the auto-generated reports. (Replaced an earlier pre-render step in #310.) |
| [JRuby](https://www.jruby.org/)[10] | `EPL-2.0 OR GPL-2.0-only OR LGPL-2.1-only` | The embedded Ruby runtime that Asciidoctor itself runs on. Pulled in transitively by AsciidoctorJ. |
| [Koncept AsciiDoc Extension](https://github.com/koncept-asciidoc/koncept-asciidoc-extension)[11] | `MIT` | In-house extension (vendored in this reactor as `koncept-asciidoc-extension`) that adds Koncept macros to AsciiDoc source. |

## [#diagram-rendering](#diagram-rendering)Diagram rendering

| Component | License | Role |
| --- | --- | --- |
| [Kroki](https://kroki.io/)[12] (PlantUML, Graphviz, Mermaid, BlockDiag, etc. backends) | `MIT` (Kroki orchestrator; per-backend licenses noted below) | External HTTP service that renders diagram blocks. The IKE Network runs a private Kroki instance at `kroki.komet.sh`; documents that contain diagram blocks call it at render time. Each backend has its own license — PlantUML (GPL 3.0), Graphviz (CPL 1.0), Mermaid (MIT), etc. Diagrams render server-side; the rendered SVG/PNG is embedded in the output. |

## [#external-pdf-renderers-runtime-not-bundled](#external-pdf-renderers-runtime-not-bundled)External PDF renderers (runtime, not bundled)

Several PDF renderer profiles in `ike-parent` shell out to external binaries the user installs locally. The IKE build does not bundle or redistribute these binaries; consumers install them directly.

| Component | License | Role |
| --- | --- | --- |
| [Prince XML](https://www.princexml.com/)[13] | Commercial (out of SPDX scope); free for personal/non-commercial use | High-quality CSS-Paged-Media renderer used by the `pdf-prince` profile. |
| [Antenna House Formatter](https://www.antennahouse.com/)[14] | Commercial (out of SPDX scope) | XSL-FO renderer used by the `pdf-ah` profile. |
| [WeasyPrint](https://weasyprint.org/)[15] | `BSD-3-Clause` | CSS-Paged-Media renderer (Python) used by the `pdf-weasy` profile. |
| [Apache FOP](https://xmlgraphics.apache.org/fop/)[16] | `Apache-2.0` | XSL-FO renderer used by the `pdf-fop` profile. |
| [RenderX XEP](http://www.renderx.com/)[17] | Commercial (out of SPDX scope); free personal license available | XSL-FO renderer used by the `pdf-xep` profile. |
| [AsciidoctorJ PDF (Prawn)](https://github.com/asciidoctor/asciidoctorj-pdf)[6] | `MIT` | The default profile (`pdf` / `prawn`); listed above. Included here for completeness — this one IS bundled and runs in-process. |

## [#docbook-toolchain](#docbook-toolchain)DocBook toolchain

| Component | License | Role |
| --- | --- | --- |
| [DocBook XSL Stylesheets 1.0](https://github.com/docbook/xslt10-stylesheets)[18] | `MIT` | XSL transformation rules from DocBook → HTML/XSL-FO/etc. Wrapped by the `ike-docs:docbook-xsl` module which repackages a curated subset for IKE consumers. |
| [Saxon-HE](https://www.saxonica.com/products/products.xml)[19] | `MPL-2.0` | XSLT 2.0/3.0 processor used to apply DocBook XSL transformations. |

## [#fonts-embedded-in-rendered-pdfs-and-bundled-in-cla](#fonts-embedded-in-rendered-pdfs-and-bundled-in-cla)Fonts (embedded in rendered PDFs and bundled in classified artifacts)

| Component | License | Role |
| --- | --- | --- |
| [Noto Serif, Noto Sans, Noto Sans Mono, Noto Math, Noto Sans Symbols](https://fonts.google.com/noto)[20] | `OFL-1.1` | Default body, sans-serif, monospace, math, and symbol typefaces. Distributed via `ike-docs:minimal-fonts` as a classified artifact consumed by all PDF and printable HTML renderers. |

The full SIL Open Font License 1.1 text is included in the `minimal-fonts` artifact and reproduced in any rendered PDF that embeds these fonts.

## [#frontend-assets-bundled-in-rendered-html](#frontend-assets-bundled-in-rendered-html)Frontend assets (bundled in rendered HTML)

| Component | License | Role |
| --- | --- | --- |
| [Font Awesome](https://fontawesome.com/)[21] (free tier — icons, fonts, JS) | `CC-BY-4.0 AND OFL-1.1 AND MIT` (icons under CC-BY-4.0; fonts under OFL-1.1; JS under MIT — different files, different licenses) | Icon set used in rendered HTML. Sentry skin pulls icons through the `<i class="fa-…​">` API. |
| [Prism](https://prismjs.com/)[22] | `MIT` | Syntax highlighter for code blocks in rendered HTML. Embedded in the AsciiDoc pipeline output. |

## [#verification](#verification)Verification

- `mvn package` produces `target/bom.json` and `target/bom.xml` (CycloneDX 1.6 format) with SPDX-normalized licenses. AsciidoctorJ family, JRuby, Saxon-HE, and the rest of the declared deps appear there with canonical SPDX identifiers.
- `mvn site` populates `target/site/licenses.html`, `target/site/dependencies.html`, `target/site/plugins.html`, and `target/site/plugin-management.html` from declared `<dependencies>`, `<build><plugins>`, and `<build><pluginManagement>`.
- The same `mvn site` run copies `bom.json` and `bom.xml` into `target/site/` so they’re reachable from the published Maven Site.
- This curated document complements those mechanical inventories for the runtime renderers, fonts, frontend assets, and external services that aren’t visible to `licenses.html` or the SBOM (e.g., the Sentry skin, Kroki, the Noto font texts inside `minimal-fonts`).

## [#related](#related)Related

- [ike-docs site index](index.html)[23]
- [ike-issues#331](https://github.com/IKE-Network/ike-issues/issues/331)[24] — comprehensive third-party attribution (closed; this page’s curated content).
- [ike-issues#333](https://github.com/IKE-Network/ike-issues/issues/333)[25] — SBOM via CycloneDX + SPDX normalization (the mechanical inventory linked at the top of this page).
