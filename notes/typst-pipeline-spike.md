# Typst pipeline spike — a `typst` Asciidoctor backend, end to end

Issue IKE-Network/ike-issues#1307. Design record: `dev-document-surface-pages`
(sections "Print truth: Typst from the block model, settled", "Typst in the
documentation pipeline and in Komet", "The four AsciiDoc-source disciplines").
Date: 2026-10-19. Typst 0.15.1 (Homebrew), AsciidoctorJ 3.0.1, Asciidoctor 2.0.26.
Nothing committed.

## What was built

| Piece | Where |
|---|---|
| Converter module | `typst-asciidoc-converter/` (new reactor subproject, between `koncept-asciidoc-extension` and `ike-doc-maven-plugin`) |
| Text core (escaping, string literals, labels, protected spans) | `network.ike.docs.typst.TypstText` |
| Page and furniture setup (preamble) | `network.ike.docs.typst.TypstPageSetup` |
| The converter, `@ConverterFor(value = "typst", suffix = ".typ")` | `network.ike.docs.typst.asciidoc.TypstConverter` |
| SPI registration (`ConverterRegistry`) | `TypstConverterRegistry` + `META-INF/services` |
| Unit tests, one per construct (28) | `TypstConverterTest` |
| `Backend.TYPST("typst", "typst")`, `ike.skip.typst` (default `true`), `ike.typst.root` | `ike-doc-maven-plugin` `Backend`, `AsciidocMojo` |
| `RendererType.TYPST` running `typst compile --root … --font-path … --format pdf`, with `ike.typst.version` pin | `RenderPdfMojo` |
| `ike.skip.typst` → `pdf-typst/` | `PrepareRendererOutputMojo` |
| `assembly/typst.xml` | `ike-doc-resources` |
| Root POM: `ike.skip.typst`, `typst.executable`, `typst.version=0.15.1`, `typst-to-pdf` and `zip-typst` executions, `pdf-typst` profile (`-Dike.pdf.typst`) | `pom.xml` |
| Koncept inline macro's `typst` branch (role-tagged phrase node) | `KonceptInlineMacro` |
| Rendered evidence and the headless driver | `notes/typst-pipeline-spike/` |

A new module rather than a package in `koncept-asciidoc-extension`: the
design says one Typst writer library in `ike-docs` will be consumed by
`komet-claude-plugin` as a versioned dependency the way `stamp-adoc-core` is,
and the Koncept extension must not come along with it. The
`network.ike.docs.typst` package is the seed of that library; the converter
is one of its two front ends and lives in a sub-package for now.

With every default in place nothing changes: `ike.skip.typst` is `true` in
the root POM, in the `AsciidocMojo` and `PrepareRendererOutputMojo` defaults,
and in the skip-everything profile. `-Dike.pdf.typst` turns the track on.

## Evidence

Three topics rendered through the converter and `typst compile`, PDF and PNG
(110 ppi), under `notes/typst-pipeline-spike/<name>/`, each with its `.typ`,
`converter-log.txt`, `converter-timing.txt`, `warnings-pdf.txt`,
`warnings-png.txt` and `typst-version.txt`:

| Topic | Pages | AsciidoctorJ start + convert | `typst compile` PDF | PNG (all pages) | Typst warnings | Fallback blocks | Unresolved xrefs |
|---|---|---|---|---|---|---|---|
| `dev/stamped-document-surface` (4 tables, xrefs, Koncept-free) | 6 | 1.59 s | 59 ms | 78 ms | none | 0 | 9 (all cross-topic) |
| `anf/example-blood-pressure` (15 tables, example blocks, NOTE) | 4 | 1.60 s | 54 ms | 67 ms | none | 0 | 0 |
| `ext/regulatory/fda-guidance-benefit-risk-uncertainty-2019` (3 tables, 49 footnotes) | 16 | 1.65 s | 152 ms | 213 ms | none | 0 | 0 |

No ANF topic carries a footnote (checked every `.adoc` under `anf/topics`), so
the footnote evidence is the FDA uncertainty topic from `ike-lab-documents`,
which has tables and 49 footnotes. The ANF topic was chosen for tables.

The Maven goals were also run as goals, not only through the headless driver:
a scratch project (outside the repo) bound `idoc:asciidoc` with
`skipTypst=false`, `idoc:prepare-renderer-output`, `idoc:render-pdf` with
`renderer=typst`, and `idoc:copy-default-pdf` with `defaultRenderer=typst`.
It wrote `typst/index.typ`, compiled it to `pdf-typst/index.pdf`, and copied
it to `pdf/`. With `ike.typst.version=0.14.0` the renderer refused the 0.15.1
binary with the pinned-version message.

Compile times are far below Prince's for the same material; AsciidoctorJ's
JRuby start dominates the headless run and is paid once per Maven execution.

## What works

- Document title (labelled with its `[[id]]`), `#set document(title:)`, and
  the topic header attributes (`topic-*`, author, revision) as queryable
  `#metadata(...) <ike-attributes>`.
- Sections to any depth as Typst headings with labels from the AsciiDoc ids;
  the running head queries the last level-1 heading before the page (recto)
  and shows the document title on the verso; page 1 has no head; folio in
  the footer.
- Paragraphs, block titles, strong / emphasis / monospace (as `#raw(...)`),
  super- and subscript, curly quotes, `#highlight`.
- Unordered, ordered and description lists, nested, with continuation
  paragraphs; callout lists.
- Listings and literal blocks as fenced raw blocks (fence grows past any
  backtick run inside; language from `[source,lang]`), titled listings as
  `figure(kind: raw)`.
- Admonitions (simple and compound) with a coloured rule, examples,
  sidebars, quotes with attribution, verses, thematic and page breaks.
- Tables: `cols` proportions become `columns: (25fr, 75fr)`, column
  alignment, `table.header(repeat: true, …)`, footers, `colspan` /
  `rowspan` through `table.cell`, AsciiDoc-style cells through the inner
  document, caption above the table from the block title, label from the id.
  Hyphenation and justification are off inside cells.
- Footnotes as native `#footnote[...]`, placed at the foot of the page their
  reference falls on, with `footnote:id[]` repeats as `#footnote(<fn-id>)`;
  links inside footnotes stay links.
- Cross references: `#link(<id>)[text]` when the id is in the document's
  catalog (section titles supply the text when the source gives none);
  otherwise `ike-xref-unresolved`, red with a superscript `?`, never dropped.
- Index terms as `#metadata((index: (...))) <ike-index>`, queryable for a
  generated index; visible terms keep their text.
- Bibliography anchors as labelled `#metadata("bibref")` plus the `[id]`
  text; citations link to them.
- Images as `figure(image(...))`, with the path made root-relative under
  `typst-root` (the `asciidoc` goal passes the project base directory, the
  `render-pdf` goal passes the same directory as `--root`).
- `stem` blocks and inline `stem:[...]` through mitex (`#mitex`, `#mi`); the
  import is added only when the document has math.
- Unknown blocks (passthrough, asciimath) as a red-bordered
  `ike-unconverted` block holding the source, unknown inline nodes as
  `ike-unconverted-inline`. None occurred in the three topics.
- Koncept badges: the label in a rounded box in the pill colours
  (`ike-koncept`), kind-aware (STAMP pill differs), inactive as strikethrough,
  no SVG markup leaking into the Typst. `TODO(#1307)` in both the page setup
  and `KonceptInlineMacro` points at `notes/typst-badge-demo` for the real
  badge.

## What does not work, or is not done

1. **Nothing is tuned for a book.** The page setup is one static preamble:
   A4, binding-side margins, a serif body (Noto Serif), Noto Sans headings,
   no section numbering, no table of contents, no index page, no list of
   tables, no chapter-local folios, no `SectionStart` rules. All of it is
   where the writer library's page model goes (below), none of it is in the
   AsciiDoc.
2. **Keep rules are Typst's defaults.** A block title can be orphaned from
   its table at a page foot, and a long table breaks wherever Typst breaks
   it (headers do repeat). The FDA topic shows one page with a large blank
   foot (page 12) where a table was pushed whole to the next page: the right
   outcome, by accident rather than rule.
3. **Cross-topic xrefs are unresolved by construction** when a topic is
   rendered alone (nine in `stamped-document-surface`, all to other topics).
   An assembly resolves them; the converter needs no change for that, since
   it checks the document catalog.
4. **The Koncept badge is a placeholder** and the glossary the
   `KonceptGlossaryTreeprocessor` assembles is not emitted; the design says
   the converter should emit it from the tree processor's entries with no
   postprocessor. Not started.
5. **mitex 0.2.5, the version the design cites, does not compile under Typst
   0.15.1** (`unknown variable: kai` in its symbol table, a symbol 0.15
   removed). 0.2.7 does; the import is pinned to it. Packages are fetched
   from the Typst registry on first use and cached under
   `~/Library/Caches/typst`; a CI agent needs either network on first run or
   a seeded cache. No topic in the three has math, so mitex is exercised only
   by the unit test and the smoke sample.
6. **Fonts come from the badge demo's `fonts/` directory** for the evidence
   (Noto Serif, Noto Sans, Noto Sans Mono, Alegreya Sans SC). The pipeline's
   `${fonts.output.directory}` (from `minimal-fonts`) is what the POM
   execution passes; whether its subset carries the Serif family the preamble
   names was not checked. A missing family falls back silently to the next
   in the list, then to a system font.
7. **Two artifacts are Asciidoctor's, not the converter's**, confirmed
   against the html5 backend: `(C)` in statute citations such as
   `513(a)(3)(C)` becomes `©` (the replacements substitution; the FDA topic
   has 24 of them, and Prince shows the same), and the phased-delivery list
   at the end of `stamped-document-surface` is a paragraph, not a list,
   because the `indexterm:[...]` line directly above it glues to the first
   `.` item (a list cannot interrupt a paragraph). Both are source fixes.
8. **The validator does nothing for Typst** beyond listing `*.typ`; the
   html checks (unresolved attributes, missing images) are not applied to
   `.typ`.
9. **`ike-doc-maven-plugin/src/site`** does not yet list the typst renderer.
10. The plugin's `-o` build needs `junit-jupiter:5.10.2` and Surefire's
    JUnit platform provider in the local repository; the first build of
    the new module had to go online once for those.

## Typst warnings

None. All six compiles (three PDF, three PNG) ran clean; the `warnings-*.txt`
files hold only the `time` output. The compile error seen during the spike
was mitex 0.2.5's, above.

## What the converter needed, and what the shared writer library should be

The converter's shape was decided by three facts of Asciidoctor's converter
API, and each says something about the library.

**Substituted text arrives as a flat string with inline conversions spliced
in, and later substitutions still run over it.** The footnote macro ends at
the first `]`, the replacements pass turns `'` into `’` and `--` into a dash,
all after the quotes pass has already called the converter for `*strong*`;
and Asciidoctor derives a section id from the converted title by stripping
HTML tags. So an inline conversion's output must be opaque to Asciidoctor
and look like a tag: the converter emits each piece of Typst as Base64
between two control characters inside `<…>`, as an open token and a close
token around the substituted text (`#strong[` … `]`) or as one atom when the
content is a string argument (`#raw("…")`), and the text pass that builds a
block decodes the plain text (HTML entities back to characters, then
Typst-escaped) and restores the tokens. A `;` is inserted after a token
that ends an expression when the next character would continue it
(`#strong[x].` would otherwise read `.` as a method call), and a literal
`;` in text is escaped so it cannot end one. This is specific to the
Asciidoctor front end. The library should not know about it: it should take
a tree, not strings.

**Blocks come back already converted as strings.** A compound block's
`getContent()` is the concatenation of its children's Typst. That works, but
it means the converter cannot look at a child after conversion (to decide a
keep rule, to collect the index, to number a table per chapter) without
re-walking the AST. The library should hold a **document model** between the
front end and the serializer:

- `TypstDocument` with `PageSetup` (paper, margins with a binding side,
  folio scheme, section-start rule, as-of line, running-head policy) and
  `Fonts`; the Page terminology of the design topic, so a `FolioScheme`
  chosen in Komet and a `:folio-scheme:` attribute in a topic land in the
  same object.
- Block nodes: `Heading(level, inlines, label)`, `Paragraph(inlines, label)`,
  `ListBlock(kind, items)`, `DescriptionList`, `Raw(lang, text, caption,
  label)`, `Admonition(kind, title, blocks)`, `Framed(kind, title, blocks)`
  (example, sidebar, quote with attribution), `Table(columns, header rows,
  body rows, footer rows, caption, label)` with `Cell(blocks, colspan,
  rowspan, align)`, `Figure(image or content, caption, label, kind)`,
  `Math(latex or typst, label)`, `Break(kind)`, and `Unconverted(context,
  source)` so a fallback is a node, not a string.
- Inline nodes: `Text`, `Strong`, `Emph`, `Code`, `Super`, `Sub`, `Quoted`,
  `Link(url)`, `Ref(label, inlines)`, `UnresolvedRef(id, inlines)`,
  `Footnote(inlines, label)`, `FootnoteRef(label)`, `IndexTerm(terms,
  visible)`, `Image`, `Badge(label, kind, status)`, `Math`.
- Every node carries an optional **source span** (file, line range, block
  id), which the Pages view's click-to-block mapping and the Flow view's
  page marker need; the converter can fill it from `getSourceLocation()`
  when `sourcemap` is on, the block-model writer from the block identity.

**The serializer is where all the Typst lives.** Today the preamble is a
text block in `TypstPageSetup` and the per-construct syntax is scattered
through the converter. In the library the serializer owns both: it writes
the preamble from `PageSetup`, defines the `ike-*` helper functions once, and
emits each node. The escaping rules in `TypstText` (markup escaping,
line-leading markers, string literals, label alphabet, fence sizing) move
with it unchanged; the Base64 span trick does not. Two things the spike
emits ad hoc belong to it as policy: the `#strong[...]` wrapping of header
cells and the `;` after an expression.

**Two front ends.** `TypstConverter` becomes a thin AST-to-model walk: each
`convert(node, transform)` builds a node and returns a placeholder; the
document-level call assembles the tree and calls the serializer. Komet's
block-model writer builds the same model from `StoredBlock`s. The shared
tests then assert on the model and on the serializer separately, instead of
on strings through Asciidoctor as the spike's tests do.

**Things the library needs that the spike did not reach.** Keep rules by
block kind (`block(breakable: false)` for small tables and for a title with
its table, `table.header` already repeats); table numbering per chapter and
a generated list of tables (`outline(target: figure.where(kind: table))`);
the index page from the `<ike-index>` metadata the converter already emits;
the glossary from the Koncept tree processor's entries; section numbering
and the table of contents as `PageSetup` choices; the Koncept badge drawing
from `KonceptAppearance` once the badge demo settles SVG against native; and
the pinned Typst version carried in one place (the parent's
`typst.version`, today in the root POM of `ike-docs`).

## Prior art, and what was taken from each

Pointed out by the parent session after the spike was built; read on
2026-10-19, none of the code borrowed.

- **jassielof/asciidoctor-typst** (TypeScript over `@asciidoctor/core`,
  last pushed 2026-10-14). The closest analogue: a custom `typst` inline
  converter plus an AST walker, no DocBook or HTML in between. Its construct
  mapping is the same as this converter's almost line for line
  (`#link(<id>)[…]`, `#footnote[…]<fn-id>` / `#footnote(<fn-id>)`,
  `#metadata(none)<id>` anchors, `table.header`, `figure` for titled
  blocks, `#mi` / `#mitex` from mitex, `adoc-*` template functions for what
  Typst lacks), which is some confirmation the mapping is the natural one.
  Two things were taken, both now in `TypstText`: its inline output is
  *token-protected* like ours, but shaped as an HTML-looking tag with
  open and close tokens around the text left in place, because Asciidoctor
  derives a section id from the *converted* title by stripping tags, so a
  title with `*bold*` must still yield `_using_bold`; the spike's first
  shape (one opaque span per inline node) produced ids like
  `_using_i3n0cm9uz1tib2xkxq_here`, now fixed and tested. And its escaping
  list: `;` escaped always (a literal `;` right after `#strong[x]` would
  silently end the expression), `--`, `-?` and `...` escaped so Typst's
  shorthands do not fire on text Asciidoctor already replaced. Things it
  does that this spike does not, worth a look when the writer library is
  built: a front/main/back-matter template with the Asciidoctor special
  section styles (abstract, preface, appendix, glossary, index, colophon),
  authors and revision as Typst states so `{author}` references become
  lookups, `[bibliography]` sections turned into Hayagriva YAML with `@id`
  citations (lossy: custom labels and grouping captions are dropped), raw
  blocks as one string argument so indentation can never corrupt them, a
  diagnostics channel with `--strict`, and a `[typst]` passthrough block
  (which the four disciplines forbid for us). It refuses AsciiMath
  outright; the spike marks it as unconverted, which amounts to the same.
- **ds26gte/adoc2typ** (three shell scripts, 2025-04): `asciidoctor -b
  docbook`, then `pandoc -f docbook -t typst`, then batch `vi` fix-ups for
  the admonitions pandoc drops, verse blocks, spacing round figures and
  code. Taken: nothing to borrow, but it is the baseline that shows why the
  backend route is right. DocBook loses admonition kinds, table column
  proportions, index terms and ids on the way, and every fix-up is a regex
  over generated Typst. A side-by-side comparison was not run: `pandoc` is
  not installed on this machine.
- **riscv/riscv-isa-manual#2409** (proposal to move the RISC-V ISA manual
  from AsciiDoc to Typst, 2025-12 to 2026-06, 32 comments). The pain
  points are the build chain (minutes per PDF, Ruby plus Python plus Node
  plus Docker) and static instruction tables with no link to the
  machine-readable opcodes; the answer the authors give is that a second
  migration in a decade is not worth it and that "the source documentation
  must generate from AsciiDoc". One commenter had an Asciidoctor Typst
  backend written and reports it works ("one massive `.typ`", diagrams
  converted separately, private-use-area escaping he was unsure of); an
  Antora Typst backend for accessible PDFs is being discussed on the
  Antora Zulip. Taken: the requirement that AsciiDoc stays the source and
  Typst is only ever generated is what the four disciplines already say,
  and it is the one point every party in that thread agrees on; the
  per-chapter `.typ` files and WASM-plugin diagrams they want are the
  `include` and `diagraph` shape of the design topic.

## How to reproduce

```bash
# converter + plugin, offline once the deps are cached
cd ~/ike-dev/ike-docs
./mvnw -o -pl ike-doc-resources,koncept-core,koncept-asciidoc-extension,typst-asciidoc-converter,ike-doc-maven-plugin verify
# one topic, headless (needs the modules installed and typst on PATH)
cd notes/typst-pipeline-spike
./render.sh ~/ike-dev/anf/topics/src/docs/asciidoc/topics/anf/example-blood-pressure.adoc anf-example-blood-pressure
```

`cp.txt` beside `render.sh` is the converter's resolved test classpath on
this machine (`mvn dependency:build-classpath -Dmdep.includeScope=test` on
the converter module, with `-am`); regenerate it elsewhere. In a doc project
on this plugin version, `-Dike.pdf.typst` runs the whole track as Maven
executions.
