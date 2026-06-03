---
date_published: 2026-06-02
date_modified: 2026-06-02
canonical_url: https://github.com/IKE-Network/ike-docs/built-with.html
---

# Built With

Open-source software that `ike-doc-resources` 70 depends on, links against, ships within, or invokes at runtime.

Three layers of attribution ship with each release:

- [Software Bill of Materials (CycloneDX, JSON)](bom.json)[1] — full transitive dependency graph with SPDX-normalized licenses and artifact hashes. Ingestible by Dependency-Track, Trivy, Snyk, GitHub’s dependency graph.
- [Licenses (SPDX)](licenses.html)[2] — human-readable SPDX-grouped view of declared dependencies, generated from `bom.json` (#335).
- This page — curated companion covering what mechanical reports can’t see (Maven Site skin, external services, fonts inside artifacts, frontend assets in rendered HTML).

## [#curated-narrative](#curated-narrative)Curated narrative

Components covered by the project-wide supplement at `src/main/built-with/supplement.yaml`. These are the components that don’t appear in `bom.json` because they aren’t Maven artifacts (external services, fonts inside classifier ZIPs, runtime binaries, frontend assets).

### [#asciidoc-rendering-toolchain](#asciidoc-rendering-toolchain)AsciiDoc rendering toolchain

| Component | License | Role |
| --- | --- | --- |
| [AsciidoctorJ](https://asciidoctor.org/)[3] | `MIT` | The Java entry-point for the Asciidoctor renderer. ike-doc-maven-plugin invokes it via the official Maven plugin to convert .adoc source to HTML, DocBook, and XHTML. |
| [AsciidoctorJ PDF](https://github.com/asciidoctor/asciidoctorj-pdf)[4] | `MIT` | Direct PDF rendering of AsciiDoc. The prawn profile in ike-parent activates this renderer; output is post- processed for page-break and font handling. |
| [AsciidoctorJ Diagram](https://github.com/asciidoctor/asciidoctorj-diagram)[5] | `MIT` | Inline diagram rendering. Used together with the Kroki extension to support PlantUML, Graphviz, Mermaid, and similar formats inside AsciiDoc source. |
| [Asciidoctor Maven Plugin](https://github.com/asciidoctor/asciidoctor-maven-plugin)[6] | `MIT` | The Maven plugin wrapper that drives AsciidoctorJ from the build. |
| [Asciidoctor Parser Doxia Module](https://github.com/asciidoctor/asciidoctor-doxia)[7] | `MIT` | Lets maven-site-plugin natively render src/site/asciidoc/*.adoc alongside the auto-generated reports. (Replaced an earlier pre-render step in #310.) |
| [JRuby](https://www.jruby.org/)[8] | `EPL-2.0 OR GPL-2.0-only OR LGPL-2.1-only` | Embedded Ruby runtime that Asciidoctor itself runs on. Pulled in transitively by AsciidoctorJ. |
| [Koncept AsciiDoc Extension](https://github.com/koncept-asciidoc/koncept-asciidoc-extension)[9] | `MIT` | In-house extension (vendored in this reactor as koncept-asciidoc-extension) that adds Koncept macros to AsciiDoc source. |

### [#diagram-rendering](#diagram-rendering)Diagram rendering

| Component | License | Role |
| --- | --- | --- |
| [Kroki (PlantUML, Graphviz, Mermaid, BlockDiag, etc. backends)](https://kroki.io/)[10] | `MIT` | External HTTP service that renders diagram blocks. The IKE Network runs a private Kroki instance at kroki.komet.sh; documents that contain diagram blocks call it at render time. Each backend has its own license — PlantUML (GPL-3.0), Graphviz (CPL-1.0), Mermaid (MIT), etc. Diagrams render server-side; the rendered SVG/PNG is embedded in the output. |

### [#external-pdf-renderers-runtime-not-bundled](#external-pdf-renderers-runtime-not-bundled)External PDF renderers (runtime, not bundled)

| Component | License | Role |
| --- | --- | --- |
| [Prince XML](https://www.princexml.com/)[11] | `Commercial (out of SPDX scope); free for personal/non-commercial use` | High-quality CSS-Paged-Media renderer used by the pdf-prince profile. |
| [Antenna House Formatter](https://www.antennahouse.com/)[12] | `Commercial (out of SPDX scope)` | XSL-FO renderer used by the pdf-ah profile. |
| [WeasyPrint](https://weasyprint.org/)[13] | `BSD-3-Clause` | CSS-Paged-Media renderer (Python) used by the pdf-weasy profile. |
| [Apache FOP](https://xmlgraphics.apache.org/fop/)[14] | `Apache-2.0` | XSL-FO renderer used by the pdf-fop profile. |
| [RenderX XEP](http://www.renderx.com/)[15] | `Commercial (out of SPDX scope); free personal license available` | XSL-FO renderer used by the pdf-xep profile. |

### [#docbook-toolchain](#docbook-toolchain)DocBook toolchain

| Component | License | Role |
| --- | --- | --- |
| [DocBook XSL Stylesheets 1.0](https://github.com/docbook/xslt10-stylesheets)[16] | `MIT` | XSL transformation rules from DocBook → HTML/XSL-FO/etc. Wrapped by the ike-docs:docbook-xsl module which repackages a curated subset for IKE consumers. |
| [Saxon-HE](https://www.saxonica.com/products/products.xml)[17] | `MPL-2.0` | XSLT 2.0/3.0 processor used to apply DocBook XSL transformations. |

### [#fonts-embedded-in-rendered-pdfs-and-bundled-in-cla](#fonts-embedded-in-rendered-pdfs-and-bundled-in-cla)Fonts (embedded in rendered PDFs and bundled in classified artifacts)

| Component | License | Role |
| --- | --- | --- |
| [Noto Serif, Noto Sans, Noto Sans Mono, Noto Math, Noto Sans Symbols](https://fonts.google.com/noto)[18] | `OFL-1.1` | Default body, sans-serif, monospace, math, and symbol typefaces. Distributed via ike-docs:minimal-fonts as a classified artifact consumed by all PDF and printable HTML renderers. The full SIL Open Font License 1.1 text is included in the minimal-fonts artifact and reproduced in any rendered PDF that embeds these fonts. |

### [#frontend-assets-bundled-in-rendered-html](#frontend-assets-bundled-in-rendered-html)Frontend assets (bundled in rendered HTML)

| Component | License | Role |
| --- | --- | --- |
| [Font Awesome (free tier — icons, fonts, JS)](https://fontawesome.com/)[19] | `CC-BY-4.0 AND OFL-1.1 AND MIT` | Icon set used in rendered HTML. Sentry skin pulls icons through the <i class="fa-…​"> API. Different files under different licenses — icons CC-BY-4.0; fonts OFL-1.1; JS MIT. |
| [Prism](https://prismjs.com/)[20] | `MIT` | Syntax highlighter for code blocks in rendered HTML. Embedded in the AsciiDoc pipeline output. |

## [#mechanical-inventory](#mechanical-inventory)Mechanical inventory

Direct dependencies of this module, grouped by SPDX expression. Generated from `bom.json` at build time.

| SPDX Expression | Components |
| --- | --- |
| `Apache-2.0` | 3 |
| **Total** | **3** |

For full per-component detail (group, artifact, version, hashes, transitive deps), see [bom.json](bom.json)[1] or [licenses.html](licenses.html)[2].

## [#related](#related)Related

- [site index](index.html)[21]
- [ike-issues#336](https://github.com/IKE-Network/ike-issues/issues/336)[22] — the issue that introduced this page (rename of the legacy "Third-Party Notices" to friendlier "Built With").
