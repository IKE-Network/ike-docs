---
date_published: 2026-05-10
date_modified: 2026-05-10
canonical_url: https://github.com/IKE-Network/ike-docs/index.html
---

# IKE Doc Maven Plugin

The `ike-doc-maven-plugin` provides the `idoc:*` goal prefix — AsciiDoc rendering, multi-renderer PDF wrappers (Prince, Apache FOP, RenderX XEP, Antenna House, WeasyPrint), DocBook XSL patching, breadcrumb injection for site navigation, and renderer log scanning.

| Coordinate | Value |
| --- | --- |
| Group ID | `network.ike.docs` |
| Artifact ID | `ike-doc-maven-plugin` |
| Goal prefix | `idoc:` |
| Packaging | `maven-plugin` |
| Java version | 25 |

For workspace-spanning goals (`ws:*` prefix), see the [ws plugin](https://ike.network/ike-platform/ike-workspace-maven-plugin/)[1] in `ike-platform`. For single-repo release orchestration (`ike:*`), see the [ike plugin](https://ike.network/ike-tooling/ike-maven-plugin/)[2] in `ike-tooling`. The three plugins are deliberately separate — `idoc:*` is doc rendering, `ike:*` is single-repo release, `ws:*` fans out across a workspace’s checked-out subprojects.

## [#plugin-shape](#plugin-shape)Plugin shape

This is a **regular Maven plugin** — no `<extensions>true</extensions>`, no custom `<packaging>` type registered via `META-INF/plexus/components.xml`, no participation in the build extension realm. Goals look up at execution time, the plugin’s `<version>` interpolates from `${ike-docs.version}` like any other managed plugin, and consumers inherit the version through ordinary `<pluginManagement>` indirection.

This was not always the case. Earlier revisions registered the `<packaging>ike-doc</packaging>` custom type here, which forced this plugin to be loaded as a build extension and consequently forced its `<version>` to be a literal string everywhere it was declared. That constraint was retired alongside the migration to a classifier-canonical doc shape (see [Design rationale](#design-rationale) below).

What this means for plugin consumers: nothing changes about how you **invoke** `idoc:*` goals. What changed is that the plugin no longer forces literal-version pins in your POM, and no longer contributes `<packaging>ike-doc</packaging>` to your reactor. Doc modules now use `<packaging>pom</packaging>` (or `<packaging>jar</packaging>` for hybrid Java-plus-docs modules) plus an `adoc` classifier attachment; activation is path-conditional on `src/docs/asciidoc/` existence in `ike-parent’s `doc-pipeline` profile.

## [#goals-at-a-glance](#goals-at-a-glance)Goals at a glance

| Goal | Purpose |
| --- | --- |
| `idoc:asciidoc` | Render AsciiDoc sources via AsciidoctorJ — the heart of the doc pipeline. Handles IKE-specific renderer profiles (Prawn / Prince / AH / WeasyPrint / XEP / FOP / DocBook / single-page HTML). |
| `idoc:render-pdf` | Wrapper around the five external PDF renderers (Prince, AH, FOP, WeasyPrint, XEP). Selects the active renderer per `-Dike.pdf.<name>=true` toggles set by the `doc-pipeline` profile. |
| `idoc:adocstudio` | Generate Adoc Studio sidecar projects for assembly modules. |
| `idoc:patch-docbook` | Patch the stock DocBook XSL 1.79.2 stylesheets to suppress two known Saxon warnings. Applied during the `docbook-xsl` artifact build. |
| `idoc:fix-svg` | Remove bare `<rect/>` elements that Mermaid emits as artifacts in generated HTML — they cause rendering glitches in some browsers. |
| `idoc:inject-breadcrumb` | Inject navigation breadcrumbs and theme overrides into JaCoCo HTML reports so they fit visually with the rest of the Maven site. |
| `idoc:copy-default-pdf` | Promote one renderer’s PDF as the default. With multiple renderers producing output in `pdf-prince/`, `pdf-fop/`, etc., this picks the first available and copies it to a single canonical `pdf/` location. |
| `idoc:prepare-renderer-output` | Create the per-renderer output directories (`pdf-prince/`, `pdf-fop/`, etc.) before the renderers run. |
| `idoc:scan-logs` | Scan renderer log files (`renderer-*.log`) for error patterns and print a summary. Used after multi-renderer runs to flag failures fast. |
| `idoc:package-doc` | Zip `src/docs/asciidoc/` and assign as primary artifact. Originally bound to the `<package>` phase of the retired `ike-doc` lifecycle; superseded by `maven-assembly-plugin’s `adoc` classifier execution in `ike-parent’s `doc-pipeline` profile. Retained at present pending a final decision in the open question on [ike-issues#321](https://github.com/IKE-Network/ike-issues/issues/321)[3]; may be removed in a follow-up. |

## [#design-rationale](#design-rationale)Design rationale

This plugin’s shape — regular plugin, no extensions, no custom packaging — is the result of a deliberate architectural decision documented in [ike-issues#321](https://github.com/IKE-Network/ike-issues/issues/321)[3] and the `dev-classifier-canonical-doc-shape` topic in `ike-lab-documents/topics/`. Brief summary, since the issue will eventually close and this page is what surviving teams will land on:

### [#why-no-extensions](#why-no-extensions)Why no extensions

A plugin with `<extensions>true</extensions>` is loaded into the build extension realm at **project-load time** — during Maven’s “Scanning for projects” phase, before the Model Builder runs property interpolation. This means the plugin’s `<version>` in the POM cannot be a `${property}`; it must be a literal string.

For a single-repo reactor with one plugin, the constraint is manageable. For our cross-repo cascade (`ike-tooling` → `ike-docs` → `ike-platform` → consumers), it became toxic: every consumer POM had to carry the literal version of every extension plugin from upstream, the literal had to be kept in sync manually across repos, and routine version-property tooling (`ws:align-publish`, `versions:set-property`) was structurally unable to maintain it. The pain surfaced as [ike-issues#236](https://github.com/IKE-Network/ike-issues/issues/236)[4] — pre-flight release checks repeatedly catching stale literals after upstream releases that the alignment tooling could not see.

### [#why-no-packagingike-doc-packaging](#why-no-packagingike-doc-packaging)Why no `<packaging>ike-doc</packaging>`

The historical reason for `<extensions>true</extensions>` here was to register a custom `<packaging>ike-doc</packaging>` type for documentation modules — a packaging that produces a `.zip` of AsciiDoc sources as the primary artifact, with a lean lifecycle that skips compile/test phases.

Inspection revealed two structural problems with that frame:

1. **The custom packaging produced a primary artifact nobody used.** Across 43 `<packaging>ike-doc</packaging>` modules in `ike-lab-documents`, **zero** dependencies referenced `<type>ike-doc</type>`. Consumers exclusively used a parallel `<classifier>asciidoc</classifier><type>zip</type>` attachment (built by `maven-assembly-plugin` from the same source directory). Same content, two coordinates, redundant.
2. **Custom packaging cannot serve hybrid modules.** `<packaging>` is single-valued: a Java module that ships reference docs cannot be both `jar` and `ike-doc`. Hybrid modules are the norm in any non-trivial project. Every comparable docs-as-code project in the JVM ecosystem (Hibernate, Eclipse Collections, Vert.x, Quarkus, OptaPlanner, Apache projects) handles this via classifier attachments for exactly this reason. No project in the surveyed set invents a custom packaging type for documentation.

The architectural decision was therefore to retire `<packaging>ike-doc</packaging>` in favor of a classifier- canonical shape: `<classifier>adoc</classifier><type>zip</type>` attached to either a `<packaging>pom</packaging>` (doc-only) or `<packaging>jar</packaging>` (hybrid) primary. Activation is path-conditional on `src/docs/asciidoc/` existence in `ike-parent’s `doc-pipeline` profile — not packaging type — so hybrid modules become first-class without per-module ceremony.

### [#the-artifact-uniformity-argument-for-staying-maven](#the-artifact-uniformity-argument-for-staying-maven)The artifact-uniformity argument for staying Maven-canonical

A natural objection: most large JVM projects (Spring, many Apache projects, Quarkus) publish docs as static sites to gh-pages or CDN-hosted destinations rather than as Maven artifacts. Why not follow that pattern?

Examined point-by-point, those rationales do not transfer. Most of the apparent reasons (continuous publication, search-driven discovery, contributor friction, CDN economics) have Maven-snapshot-plus-post-deploy-rsync solutions and so do not argue against staying inside the Maven artifact ecosystem. What genuinely remains as a non-Maven argument — Antora as a turnkey product — does not apply here, because we have already built the equivalent rendering pipeline (`ike-parent’s render profiles, Prince/FOP/XEP chains, knowledge.design rsync deploy, the asciidoctor toolchain). Spring would have to throw away their pipeline to gain Antora’s turnkey-ness; we have ours, so the comparison does not apply.

For our constraints — small team, cross-repo doc dependencies via topic libraries, an already-built rendering pipeline, and a standing commitment to artifact uniformity — staying inside the Maven artifact ecosystem is the rigorous choice. We hold every shipped artifact, code or documentation, to the same engineering standard: signed (Bouncy Castle GPG, parallel-safe), versioned, deployed to Nexus, mirrorable, and reproducibly buildable years later. We do not want one class of output operating under different rules than another. The single-pipeline discipline is the standard we are protecting.

The classifier-canonical shape **is** fully Maven-canonical — every artifact deploys to Nexus, signed and versioned, just like every other output of the project. The choice between custom packaging and classifier is not a Maven-vs-non-Maven choice; both stay inside the Maven ecosystem. Custom packaging was buying an aesthetic type-system marker; classifier-canonical buys a single mechanism that handles doc-only and hybrid uniformly.

### [#tracking](#tracking)Tracking

- [ike-issues#321](https://github.com/IKE-Network/ike-issues/issues/321)[3] — primary tracking issue (umbrella); subsumes #220, #236, #320.
- [ike-issues#216](https://github.com/IKE-Network/ike-issues/issues/216)[5] — repo split that established the cross-repo boundary the extension realm was working around.
- Design note: `dev-classifier-canonical-doc-shape` in `ike-lab-documents/topics/` (full Socratic discovery captured for posterity).

## [#see-also](#see-also)See also

- [ike:* plugin in ike-tooling](https://ike.network/ike-tooling/ike-maven-plugin/)[2] — single-repo release orchestration.
- [ws:* plugin in ike-platform](https://ike.network/ike-platform/ike-workspace-maven-plugin/)[1] — workspace-spanning goals.
- [ike-docs reactor home](https://ike.network/ike-docs/)[6].
- [Source on GitHub](https://github.com/IKE-Network/ike-docs)[7].
- [Issue tracker](https://github.com/IKE-Network/ike-issues)[8].
