---
date_published: 2026-06-23
date_modified: 2026-06-23
canonical_url: https://ike.network/ike-docs/getting-started.html
---

# Getting Started

## [#prerequisites](#prerequisites)Prerequisites

Java 21+ JRuby 10 requires Java 21 or later. The pipeline targets Java 25 for tool modules but any JDK 21+ will work for document generation. Maven 4+ The reactor uses POM model version 4.1.0 and Maven 4 subproject semantics. Download from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)[1].  

## [#for-document-authors](#for-document-authors)For Document Authors

If you are writing documentation using the IKE pipeline, your project inherits from `network.ike.platform:ike-parent`. The same parent serves both doc-only modules and hybrid Java-plus-docs modules — packaging type is what differentiates them (see “Hybrid Java-plus-docs” below).

### [#minimal-pom-xml--doc-only-project](#minimal-pom-xml--doc-only-project)Minimal `pom.xml` — doc-only project

```
<project xmlns="http://maven.apache.org/POM/4.1.0" ...>
    <modelVersion>4.1.0</modelVersion>

    <parent>
        <groupId>network.ike.platform</groupId>
        <artifactId>ike-parent</artifactId>
        <version>26</version>
        <relativePath/>          <!-- force repo lookup; see ike-issues#323 -->
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-document</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>   <!-- doc-only: pom packaging post-#321 -->

    <name>My IKE Document</name>
</project>
```

The AsciiDoc source payload is auto-attached as a `<classifier>adoc</classifier><type>zip</type>` artifact when `src/docs/asciidoc/` exists. Renderer outputs (PDF, HTML) attach as additional classifiers when their toggles are enabled.

`<relativePath/>` (empty self-closing element) forces Maven to resolve `ike-parent` from the local repository / Nexus rather than traversing the filesystem. Required when your project lives inside a workspace aggregator that itself inherits the same parent (see `IKE-Network/ike-issues#323`).

### [#document-structure](#document-structure)Document structure

Place your AsciiDoc content in the standard location:

```
my-document/
├── pom.xml
└── src/
    └── docs/
        └── asciidoc/
            ├── index.adoc          (1)
            ├── chapters/
            │   ├── introduction.adoc
            │   └── methodology.adoc
            └── images/
                └── architecture.png
```

The master document. Override `pdf.source.document` in your POM if named differently.  

### [#build-html](#build-html)Build HTML

```
mvn clean verify
```

HTML output lands in `target/generated-docs/html/`. The HTML profile is active by default.

### [#build-pdf](#build-pdf)Build PDF

```
mvn clean verify -Dike.pdf.prawn
```

The Prawn renderer is free and built-in — no external tool installation required. PDF output lands in `target/generated-docs/pdf-prawn/`.

### [#multiple-renderers](#multiple-renderers)Multiple renderers

Profiles are composable. Activate several at once:

```
mvn clean verify -Dike.pdf.prawn -Dike.pdf.prince -Dike.html.single
```

## [#hybrid-java-plus-docs](#hybrid-java-plus-docs)Hybrid Java-plus-docs

Java modules that also ship documentation inherit from the same `ike-parent`. The differentiator from a doc-only module is the packaging type — hybrids use the default `jar` (or `maven-plugin`, etc.) and let the path-conditional doc-pipeline profile attach the `adoc` classifier on top of the primary jar.

### [#minimal-pom-xml](#minimal-pom-xml)Minimal `pom.xml`

```
<project xmlns="http://maven.apache.org/POM/4.1.0" ...>
    <modelVersion>4.1.0</modelVersion>

    <parent>
        <groupId>network.ike.platform</groupId>
        <artifactId>ike-parent</artifactId>
        <version>26</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <name>My IKE Project</name>
</project>
```

`ike-parent` provides Java 25 compiler config, test framework versions, JPMS `--add-opens` for surefire/failsafe, signing, javadoc, and the rest of the Java toolchain — there is no separate `java-parent` (the previously-distinct java-parent was retired in `IKE-Network/ike-issues#216`).

Java source goes in `src/main/java/` as usual. Documentation goes in `src/docs/asciidoc/`. Both compile and render in a single `mvn clean verify`. The doc-pipeline profile auto-activates on `src/docs/asciidoc/` existence — no per-module declaration needed.

## [#using-koncept-markup](#using-koncept-markup)Using Koncept Markup

Reference clinical concepts with the `k:Name[]` inline macro:

```
The patient presented with k:HeartFailure[] and k:AorticStenosis[].
```

This renders as clickable SVG badges that link to an auto-generated glossary. Supply definitions via a `koncepts.yml` file:

```
HeartFailure:
  label: Heart Failure
  definition: >
    A clinical syndrome characterized by the heart's inability
    to pump sufficient blood to meet metabolic demands.
  axiom: "≡ ClinicalSyndrome ⊓ ∃hasPathology.(InsufficientCardiacOutput)"
  sctid: "84114007"
```

See the [Koncept Reference](koncept-reference.html)[2] for full details.

## [#using-semantic-linebreaks](#using-semantic-linebreaks)Using Semantic Linebreaks

Format your AsciiDoc source with one sentence per line for cleaner git diffs:

```
java -jar semantic-linebreak/target/semantic-linebreak-1.0.0-SNAPSHOT.jar \
    src/docs/asciidoc/index.adoc
```

See [Semantic Lines](semantic-lines.html)[3] for the specification and rationale.

## [#available-renderers](#available-renderers)Available Renderers

| Renderer | Cost | Activation | Notes |
| --- | --- | --- | --- |
| HTML5 | Free | Default (always on) | Browsable output with sidebar TOC |
| Prawn PDF | Free | `-Dike.pdf.prawn` | Built-in JRuby/Prawn backend |
| Prince XML | $495+ | `-Dike.pdf.prince` | Best CSS Paged Media, PDF/UA-1 |
| Antenna House | $560+ | `-Dike.pdf.ah` | CSS + XSL-FO dual engine |
| WeasyPrint | Free | `-Dike.pdf.weasyprint` | Python, open source |
| RenderX XEP | Free personal | `-Dike.pdf.xep` | XSL-FO via DocBook pipeline |
| Apache FOP | Free | `-Dike.pdf.fop` | Pure Java XSL-FO, Apache 2.0 |
