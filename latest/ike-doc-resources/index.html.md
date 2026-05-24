---
date_published: 2026-05-22
date_modified: 2026-05-22
canonical_url: https://github.com/IKE-Network/ike-docs/index.html
---

# IKE Documentation Resources

Shared build resources for the IKE documentation pipeline. Packaged as a JAR artifact and unpacked at build time into each consuming project’s `target/ike-doc-resources/` directory.

## [#contents](#contents)Contents

PDF themes `ike-default-theme.yml` — Asciidoctor PDF (Prawn) theme defining IKE brand colors, fonts, margins, and heading styles. Print CSS `ike-print.css` — stylesheet for CSS-based PDF renderers (Prince, Antenna House, WeasyPrint). Assembly descriptors Maven Assembly Plugin descriptors for packaging documentation output as classified ZIP artifacts. Renderer configurations FOP user configuration (`fop-userconfig.xml`), XEP configuration, and other renderer-specific settings. SVGO configuration SVG optimizer settings for post-processing diagram output. Shared docinfo AsciiDoc `docinfo.html` fragments shared across all IKE documents.  

## [#distribution](#distribution)Distribution

Resources are unpacked during the `initialize` phase by `maven-dependency-plugin`. All paths in the pipeline resolve through `$\{ike.doc.resources.directory}`, eliminating `../` relative paths and ensuring clean isolation between modules.
