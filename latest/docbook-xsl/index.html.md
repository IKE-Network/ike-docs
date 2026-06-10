---
date_published: 2026-06-09
date_modified: 2026-06-09
canonical_url: https://github.com/IKE-Network/ike-docbook-xsl/index.html
---

# IKE DocBook XSL Stylesheets

DocBook XSL 1.79.2 stylesheets with an IKE FO customization layer. This artifact provides the complete XSL-FO pipeline for the AsciiDoc → DocBook 5 → XSL-FO → PDF rendering path used by the FOP and XEP renderers.

## [#architecture](#architecture)Architecture

The rendering pipeline for XSL-FO-based renderers:

1. AsciiDoc source is converted to DocBook 5 XML by AsciidoctorJ
2. Saxon-HE applies DocBook XSL stylesheets to produce XSL-FO
3. The IKE customization layer adjusts formatting for IKE brand styles
4. Apache FOP or RenderX XEP renders the XSL-FO to PDF

## [#customization-layer](#customization-layer)Customization Layer

The IKE FO customization imports the upstream DocBook XSL 1.79.2 stylesheets and overrides specific templates for:

- Page geometry and margins matching IKE print specifications
- Header and footer layout with IKE branding
- Font family assignments using the `minimal-fonts` Noto subset
- Table and figure formatting
- Code listing presentation

## [#batik-svg-workarounds](#batik-svg-workarounds)Batik SVG Workarounds

Apache FOP uses Batik for SVG rendering. This module includes post-processing fixes for known Batik limitations:

- Missing `width`/`height` on `<rect>` elements
- `orient="auto-start-reverse"` on `<marker>` elements
- `alignment-baseline="central"` (SVG2, unsupported by Batik)
- `fill:rgba()` function values converted to hex
