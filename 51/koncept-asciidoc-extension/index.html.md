---
date_published: 2026-05-20
date_modified: 2026-05-20
canonical_url: https://github.com/IKE-Network/ike-docs/index.html
---

# Koncept AsciiDoc Extension

An AsciidoctorJ extension providing the `k:ConceptName[]` inline macro for formally identified terminology concepts. Renders SVG badges for HTML and PDF output, DocBook phrases for XSL-FO backends, and plain text for Prawn. Automatically generates a Referenced Koncepts glossary section with definitions, description logic axioms, SNOMED CT identifiers, and OWL IRIs.

## [#usage](#usage)Usage

In any AsciiDoc document processed by the IKE pipeline:

```
The patient presents with k:Hypertension[] and k:Type2Diabetes[].
```

Each `k:Name[]` reference renders as a styled badge linking to the auto-generated glossary at the end of the document.

## [#koncept-definitions](#koncept-definitions)Koncept Definitions

Definitions are sourced from YAML files (`koncepts.yml`) bundled with the extension JAR. Each entry includes:

- Natural language definition
- Description logic axioms (when available)
- SNOMED CT concept identifier
- OWL IRI for formal ontology alignment

## [#registration](#registration)Registration

The inline macro is registered via Java SPI (`META-INF/services`) and works with all AsciiDoc backends. The glossary postprocessor is registered per-execution in the `asciidoctor-maven-plugin` configuration (not via SPI, due to incompatibility with the Prawn PDF backend).

## [#backend-rendering](#backend-rendering)Backend Rendering

| Backend | Rendering |
| --- | --- |
| HTML5 | SVG badge with brand colors and hyperlink to glossary |
| Prawn PDF | Styled text with color coding |
| DocBook/FO | DocBook phrase elements for FOP and XEP rendering |
