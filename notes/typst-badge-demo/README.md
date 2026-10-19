# Koncept badges in Typst — demo and findings

Context: Typst is the settled print target for IKE documents
(`IKE-Network/ike-issues#1307`; design record
`ike-lab-documents/…/dev/document-surface-pages.adoc`). Before the Typst
writer library is built beside `koncept-asciidoc-extension`, this demo
checks that Koncept component badges — identicon + small-caps label on a
pill, kind sigil, stamp pentagon — render reliably in Typst, and which of
three ways of getting them there the writer should use.

Demo run on Graphlet, 2026-10-19. Nothing here is committed.

## Recommendation

**Draw badges natively in Typst from the appearance spec; ship the
identicon as a PNG file beside the document and place it with
`image(…, scaling: "pixelated")`. Do not embed the renderer's SVG, and
do not outline SVG text to paths.**

The writer library should emit a call to a shared `koncept.typ` template
function (one per badge: label, kind, identicon path, inactive flag) and
write `IdenticonRenderer.png(idString, 4)` to a content-addressed file,
exactly as the DocBook/Prawn path already does with `pngFile`. The pill,
pads, corner radius, colours, label face and the stamp pentagon are the
`KonceptAppearance` / `StampSigilGeometry` constants transcribed once into
that template (as `KonceptChipStyles` does for HTML). See `demo.typ`,
functions `native-badge` and `stamp-pentagon`, for a working transcription.

Two things KEC must know:

1. **Typst never warns about a font it cannot find inside an SVG.** The
   spec's family string `Alegreya Sans SC Medium` (what AWT/JavaFX use,
   `KonceptAppearance.smallCapsFamilyName`) does *not* resolve inside an
   SVG: it falls back silently to Libertinus Serif, Typst's embedded
   default, and so does the generic `sans-serif` the current
   `KonceptSvgRenderer` emits. Only the typographic family name
   `Alegreya Sans SC` (with weight 500) resolves. Native Typst text does
   warn (`unknown font family`), SVG text does not. Any SVG route therefore
   has a silent-fallback failure mode; the native route does not.
2. **An embedded SVG badge cannot adapt to its container.** In a table
   column narrower than the badge, Typst's default `fit: "cover"` crops it
   from both ends (the stamp loses its pentagon, a long label loses its
   first and last words); `fit: "contain"` shrinks it to illegibility;
   `fit: "stretch"` distorts it. A native badge wraps its label inside the
   pill instead. In a justified paragraph all three behave alike — a badge
   is one unbreakable word — but the SVG badge, sized to the spec
   (1.45 em tall, sunk 0.33 em), also pushes the lines that carry it apart,
   while the native box uses `outset` and leaves the leading untouched.

## What was built

```
notes/typst-badge-demo/
├── BadgeSvgs.java      runner: calls KonceptSvgRenderer / IdenticonRenderer for 6 badges
├── badges/             its output: *.text.svg, *.chip.svg, *.identicon.png, *.cells.json, badges.json
├── demo.typ            the Typst document: embeddings (a)–(d), see below
├── fonts/              Noto Sans/Serif/Sans Mono, Alegreya Sans SC Medium, IKE Koncept Glyphs
├── measure/            one-badge documents used to measure size per approach; the font-name and fit probes
├── out/                demo.pdf, demo-144-{p}.png, demo-288-{p}.png, demo-{p}.svg, warnings-*.txt
└── run.sh              regenerates everything
```

Six badges: a SNOMED concept (Heart Failure), a short one (mmHg), a long
one (Mild Non-Proliferative Diabetic Retinopathy), an IKE pattern
(Tinkar's Description pattern, kind P), a description (Heart failure (en),
kind D), and a stamp (Active · 2024-09-30 14:30 · KEC). Identicons are
`IdenticonRenderer.png` at Komet's display module size, 128 × 128,
byte-identical to Komet's.

Three badge sources per place:

| source | what it is |
|---|---|
| `text.svg` | `KonceptSvgRenderer.render()` verbatim, HTML `<a>` wrapper stripped. No identicon; `<text font-family="sans-serif" style="font-variant:small-caps">`, label width estimated at 7.2 px/char. Not outlined — plain `<text>`. |
| `chip.svg` | The identicon-bearing chip as one SVG — the SVG twin of `KonceptInlineMacro#renderHtmlIdenticon`: pill `<rect>`, kind letter, stamp pentagon (reused from `renderSigil`), identicon PNG as a `data:` URI `<image>`, `<text>` label naming `Alegreya Sans SC Medium, Alegreya Sans SC, Noto Sans, sans-serif`. |
| native | Typst `box(fill, radius, inset, outset)` + `image(identicon.png, scaling: "pixelated")` (or a 32 × 32 `grid` of `rect`s from `cells.json`) + `text(font: "Alegreya Sans SC", weight: 500, tracking: 0.02em)`; pentagon from `StampSigilGeometry` with `polygon` and `circle`. |

Four places: (a) a justified paragraph with the text underlined in red so
the baseline shows, repeated at 8 pt and 14 pt; (b) a 3-column table with
an options header, plus a prose cell; (c) a `terms` glossary list; (d) a
side-by-side grid of all sources, a 24 pt row, identicons at 4 em
(pixelated / smooth / rect grid), and a retired referent.

## How to run

```bash
cd ~/ike-dev/ike-docs/notes/typst-badge-demo
./run.sh            # javac the renderers → run BadgeSvgs → typst compile pdf/png/svg
./run.sh typst      # only the Typst step
```

`run.sh` compiles `koncept-core` and the three renderer classes with
`javac` straight from source (the offline Maven build of the module fails
on an unresolved `junit-jupiter`), needs the `toucan` jar in `~/.m2`, and
Typst on `PATH`. Every warning Typst prints lands in `out/warnings-*.txt`.
The 288 ppi PNGs are what the findings below were read from.

## Environment

| item | value |
|---|---|
| Typst | 0.15.1, installed 2026-10-19 on Graphlet with `brew install typst` (with KEC's approval). Not present on kec-studio or the MacBook. |
| Fonts | `--font-path fonts` only (no system fonts needed): Noto Serif (body), Noto Sans (headings, kind letters), Noto Sans Mono (code), Alegreya Sans SC Medium (labels — the face bundled in `koncept-core`), IKE Koncept Glyphs (present, unused). The Noto TTFs were copied from `ike-lab-documents/reasoner-assembly/target/fonts/`. `typst fonts --font-path fonts --ignore-system-fonts` lists them all; Alegreya appears as family **Alegreya Sans SC**, weight 500. |
| Warnings | None from the final compile in any of the four formats (`out/warnings-*.txt` are empty). Earlier iterations produced only `unknown font family: noto sans mono` (fixed by adding the font) and a `terms.item` deprecation (fixed). **Typst emitted no warning for any font inside an SVG**, including the two that did not resolve. |

## Findings

Approach × reliability (✔ reliable, ◐ works with a caveat, ✘ unreliable):

| | text.svg (today's renderer) | chip.svg (SVG with data-URI identicon) | native Typst (PNG identicon) | native Typst (rect-grid identicon) |
|---|---|---|---|---|
| Label font | ✘ `sans-serif` → Libertinus Serif, silently (small caps honoured) | ◐ Alegreya resolved — but only because the family list also names `Alegreya Sans SC`; the spec string alone falls back silently | ✔ Alegreya Sans SC 500 via `--font-path`; a missing font *warns* | ✔ same |
| Sits on the baseline | ◐ with `box(baseline: 0.33em)`; pill centred near the x-height | ◐ same | ✔ `box` with `inset`/`outset`; pill centred on the x-height, bottom ≈0.2 em below baseline, text itself on the baseline | ✔ same |
| Leading of lines carrying a badge | ✘ lines pushed apart (image 1.45 em tall + sink exceeds the line box) | ✘ same | ✔ unchanged (`outset` paints outside the layout box, like `box-decoration-break: clone`) | ✔ same |
| Scales with text size | ✔ (`height: 1.45em`) | ✔ | ✔ (all lengths in em) | ✔ |
| Justified paragraph | ◐ one unbreakable word; wide inter-word gaps on the line before a long badge | ◐ same | ◐ same (a `box` does not break) | ◐ same |
| Narrow table cell | ✘ cropped both ends by `fit: "cover"`; `contain` → illegible, `stretch` → distorted | ✘ same (stamp loses its pentagon) | ✔ label wraps inside the pill, hyphenated | ✔ same |
| Glossary `terms` list | ✔ | ✔ | ✔ | ✔ |
| Identicon crispness | — (none) | ◐ smoothed: `image-rendering: optimizeSpeed` / `pixelated` in the SVG is ignored, so the 32 × 32 pattern blurs at small sizes and at 24 pt | ✔ `scaling: "pixelated"` is crisp at every size tested (8 pt – 4 em) | ✔ crisp; 1024 rects |
| Clipping / overflow | pill 20–30 % too wide (7.2 px/char estimate) | my 0.55 em/char estimate is ~10 % wide | exact (Typst measures the text) | exact |
| Pentagon (stamp) | ✔ geometry renders | ✔ (via `<g transform>`) | ✔ `polygon` + `circle` from `StampSigilGeometry` | ✔ |
| Kind letter sigil | ✔ bold, coloured | ✔ | ✔ Noto Sans bold, coloured | ✔ |
| Retired (strikethrough) | not exercised | not exercised | ✔ `strike` in `#b00020` | ✔ |
| Text selectable / searchable in PDF | ✔ (SVG text becomes glyphs) | ✔ | ✔ | ✔ |
| Size added to a one-badge PDF (`measure/`) | +1.8 KB | +7.6 KB | +7.5 KB (≈5 KB of that is the one-time Alegreya subset; the PNG is 1.8 KB) | +101 KB — unusable at scale |
| Size added to a one-page SVG export | +0.7 KB | +4.2 KB | +7.5 KB | +137 KB |
| Asset per badge on disk | 0.35 KB | 3.0 KB | 1.8 KB PNG | 10 KB JSON |

Other observations:

- `scaling: "smooth"` on the PNG blurs the identicon exactly as the SVG
  route does; `"pixelated"` is the one to use. The rect grid is crisp but
  costs ~100 KB per badge in PDF and is slower to lay out — not worth it.
- Typst's SVG support (resvg) is otherwise solid: the data-URI PNG,
  `<polygon>`, `<circle>`, `<g transform>`, `rx` corners, `letter-spacing`,
  `font-weight`, and `font-variant: small-caps` (synthesised on Libertinus)
  all rendered; HTML-only attributes (`class`, `style="display:inline-block"`)
  were ignored without complaint.
- Hyphenation and justification behaved as expected around the badges
  (`hyphenate: true`, `lang: "en"`); the badge itself is never broken.
- At 24 pt the chip.svg badge, being wider than the text block, was
  cover-cropped like the table cell — the same failure, in prose.

## Why not outline the SVG text

Outlining would fix the silent font fallback but none of the other SVG
defects: the badge still cannot wrap, still disturbs leading, still blurs
its identicon, and the label would no longer be selectable or searchable
in the PDF. Since Typst resolves fonts inside SVGs from `--font-path`
anyway, outlining buys nothing the native route does not already give.

## What the writer library should carry

- A `koncept.typ` template: `koncept-badge(label, kind, identicon,
  inactive: false)` and `stamp-badge(text, identicon)` as in `demo.typ`,
  with the `KonceptAppearance` ratios (corner 0.5 em, pads 0.08/0.5/0.08/0.33 em,
  identicon 0.9 em, gap 0.33 em, tracking 0.02 em) and the
  `StampSigilGeometry` constants transcribed once. A parity test in
  `koncept-core` style (`KonceptChipStylesParityTest`) should pin the
  numbers.
- Font resolution by **typographic family**: `font: "Alegreya Sans SC",
  weight: 500` — not the spec's `smallCapsFamilyName` string, which is the
  AWT/JavaFX name. Consider adding a `typstFamilyName()` to the spec, or
  renaming the record component to make the distinction visible.
- The identicon as a content-addressed PNG beside the `.typ`
  (`IdenticonRenderer.pngFile`), referenced relative to `--root`, placed
  with `scaling: "pixelated"`.
- Fonts shipped with the document and passed with `--font-path`: Noto
  Sans / Serif / Sans Mono for the body, Alegreya Sans SC Medium and IKE
  Koncept Glyphs from `koncept-core`.
