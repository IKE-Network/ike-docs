// Koncept badge rendering demo for the Typst print target (IKE-Network/ike-issues#1307).
//
// Compile from this directory (fonts/ holds Noto Sans/Serif, Alegreya Sans SC Medium):
//   typst compile --root . --font-path fonts demo.typ out/demo.pdf
//
// Four ways of putting a badge on the page are compared:
//   (a) inline in a justified paragraph,
//   (b) inside a table cell,
//   (c) in a glossary-like term list,
//   (d) side by side at several sizes — the renderer's SVG vs. a native Typst drawing.
// Each place shows three badge sources:
//   text.svg  — what KonceptSvgRenderer.render() emits today (no identicon; <text> in "sans-serif")
//   chip.svg  — the identicon-bearing chip as one SVG (pill + PNG identicon data URI + <text> label)
//   native    — drawn in Typst from the spec values: box + image(identicon.png, scaling: "pixelated")
//               (or a 32×32 rect grid from cells.json) + text in Alegreya Sans SC

#set page(width: 148mm, height: 210mm, margin: (x: 12mm, y: 14mm), numbering: "1")
#set text(font: "Noto Serif", size: 10.5pt, lang: "en", hyphenate: true)
#set par(justify: true, leading: 0.65em)
#show heading: set text(font: "Noto Sans", weight: "bold")
#show heading.where(level: 1): set text(size: 14pt)
#show heading.where(level: 2): set text(size: 11pt)
#show raw: set text(font: "Noto Sans Mono", size: 0.85em)

// ---------------------------------------------------------------------------
// The shared appearance spec (KonceptAppearance.defaults()), in em at the
// label-relative ratio KonceptChipStyles uses (reference px / 12).
// ---------------------------------------------------------------------------
#let spec = (
  pill-fill: rgb("#e9eff6"),
  pill-fill-stamp: rgb("#ecebe8"),
  label-color: rgb("#2a5a8a"),
  label-color-inactive: rgb("#b00020"),
  stamp-text-color: rgb("#5a5750"),
  stamp-color: rgb("#888780"),
  corner-radius: 0.5em, // 6/12
  identicon-size: 0.9em, // the medium-tuned inline edge (KonceptChipStyles.identiconStyle)
  icon-label-gap: 0.33em, // 4/12
  pad-top: 0.08em, // 1/12
  pad-right: 0.5em, // 6/12
  pad-bottom: 0.08em,
  pad-left: 0.33em, // 4/12
  small-caps-family: "Alegreya Sans SC",
  kind-colors: (DESCRIPTION: rgb("#b8860b"), SEMANTIC: rgb("#3b8c2f"), PATTERN: rgb("#7a4fb5")),
)

#let badges = json("badges/badges.json")
#let badge(name) = badges.find(b => b.name == name)

// ---------------------------------------------------------------------------
// (1) Embedding the renderer's SVG. The SVG is 22 px (text.svg) / 20 px (chip.svg)
// tall; we size it relative to the surrounding text and sink it so the pill is
// centred on the x-height, as vertical-align:middle does in HTML.
// ---------------------------------------------------------------------------
#let svg-badge(name, variant: "chip", height: 1.45em, sink: 0.33em) = box(
  baseline: sink,
  image("badges/" + name + "." + variant + ".svg", height: height),
)

// ---------------------------------------------------------------------------
// (2) Native Typst drawing from the spec.
// ---------------------------------------------------------------------------
#let identicon-png(name, size) = image(
  "badges/" + name + ".identicon.png", width: size, height: size, scaling: "pixelated",
)

#let identicon-grid(name, size) = {
  let cells = json("badges/" + name + ".cells.json")
  let n = cells.len()
  box(width: size, height: size, clip: true, radius: 0.1 * size,
    grid(
      columns: (1fr,) * n, rows: (1fr,) * n, gutter: 0pt,
      ..cells.flatten().map(c => rect(width: 100%, height: 100%, fill: rgb(c), stroke: none)),
    ),
  )
}

// The locked StampSigilGeometry pentagon, drawn natively (unit radius = 0.92 × half-box).
#let stamp-pentagon(size) = {
  let verts = ((0.0, -1.0), (0.951, -0.309), (0.588, 0.809), (-0.588, 0.809), (-0.951, -0.309))
  let readings = (0.78, 0.48, 0.86, 0.56, 0.66)
  let r = size / 2 * 0.92
  let c = size / 2
  box(width: size, height: size, {
    place(polygon(
      stroke: (paint: spec.stamp-color, thickness: 0.064 * size, join: "round"), fill: none,
      ..verts.map(v => (c + v.at(0) * r, c + v.at(1) * r)),
    ))
    for (i, v) in verts.enumerate() {
      let d = 0.10 * r * 2
      place(dx: c + v.at(0) * r * readings.at(i) - d / 2, dy: c + v.at(1) * r * readings.at(i) - d / 2,
        circle(radius: d / 2, fill: spec.stamp-color, stroke: none))
    }
    let hub = 0.12 * r * 2
    place(dx: c - hub / 2, dy: c - hub / 2, circle(radius: hub / 2, fill: spec.stamp-color, stroke: none))
  })
}

#let native-badge(name, icon: "png", inactive: false) = {
  let b = badge(name)
  let stamp = b.kind == "STAMP"
  let fill = if stamp { spec.pill-fill-stamp } else { spec.pill-fill }
  let label-color = if stamp { spec.stamp-text-color } else if inactive { spec.label-color-inactive } else { spec.label-color }
  box(
    fill: fill,
    radius: spec.corner-radius,
    inset: (top: spec.pad-top, bottom: spec.pad-bottom, left: spec.pad-left, right: spec.pad-right),
    outset: (top: 0.1em, bottom: 0.1em),
    {
      if b.glyph != none {
        text(font: "Noto Sans", weight: "bold", fill: rgb(b.glyphColor))[#b.glyph]
        h(0.2em)
      }
      if stamp {
        box(baseline: 0.15em, stamp-pentagon(0.95em))
        h(0.25em)
      }
      box(baseline: 0.12em, if icon == "png" { identicon-png(name, spec.identicon-size) } else { identicon-grid(name, spec.identicon-size) })
      h(spec.icon-label-gap)
      if stamp {
        text(fill: label-color)[#b.label]
      } else {
        text(font: spec.small-caps-family, weight: 500, tracking: 0.02em, fill: label-color,
          if inactive { strike(b.label) } else { b.label })
      }
    },
  )
}

// A paragraph showing the same four badges three ways, with the text underlined so
// the baseline is visible against each badge.
#let specimen-par(badge-fn) = {
  let u(body) = underline(stroke: 0.3pt + red, offset: 0pt, evade: false, body)
  par[
    #u[Hypertension is a known comorbidity of] #badge-fn("heart-failure") #u[and the guideline measures it in] #badge-fn("mmhg") #u[rather than kPa. Retinal screening finds] #badge-fn("mild-npdr") #u[in a fraction of the cohort; the terminology records every name through the] #badge-fn("description-pattern") #u[so that] #badge-fn("heart-failure-en") #u[and its synonyms share one concept, each version stamped] #badge-fn("example-stamp") #u[by its author. The sentence continues long enough to justify across several lines and to let hyphenation show where it must.]
  ]
}

= Koncept badges in Typst

Typst #sys.version, fonts from `fonts/` (Noto Serif body, Noto Sans headings, Alegreya Sans SC Medium labels).
Each section shows the same six badges from three sources: the renderer's *text SVG* (today's `KonceptSvgRenderer.render`, no identicon), the *chip SVG* (identicon PNG as a data URI inside one SVG), and a *native* Typst drawing from the appearance spec.

== (a) Inline in a justified paragraph

*text.svg* — `KonceptSvgRenderer.render()` verbatim (`<text font-family="sans-serif" style="font-variant:small-caps">`):

#specimen-par(n => svg-badge(n, variant: "text"))

*chip.svg* — pill + identicon data URI + `<text font-family="Alegreya Sans SC Medium, …">`:

#specimen-par(n => svg-badge(n, variant: "chip"))

*native* — `box(fill, radius, inset)` + `image(identicon.png, scaling: "pixelated")` + `text(font: "Alegreya Sans SC", weight: 500)`:

#specimen-par(n => native-badge(n))

Scaling with the text size — native, then chip.svg, at 8 pt and 14 pt:

#text(size: 8pt)[At 8 pt: #native-badge("heart-failure") and #native-badge("heart-failure-en") versus #svg-badge("heart-failure", variant: "chip") and #svg-badge("heart-failure-en", variant: "chip").]

#text(size: 14pt)[At 14 pt: #native-badge("heart-failure") and #native-badge("heart-failure-en") versus #svg-badge("heart-failure", variant: "chip") and #svg-badge("heart-failure-en", variant: "chip").]

#pagebreak()
== (b) Inside a table cell

#table(
  columns: (auto, 1fr, 1fr),
  stroke: 0.4pt + luma(160),
  inset: 5pt,
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  table.header([*Option*], [*chip.svg*], [*native*]),
  [Primary], [#svg-badge("heart-failure", variant: "chip")], [#native-badge("heart-failure")],
  [Unit], [#svg-badge("mmhg", variant: "chip")], [#native-badge("mmhg")],
  [Long label], [#svg-badge("mild-npdr", variant: "chip")], [#native-badge("mild-npdr")],
  [Pattern], [#svg-badge("description-pattern", variant: "chip")], [#native-badge("description-pattern")],
  [Description], [#svg-badge("heart-failure-en", variant: "chip")], [#native-badge("heart-failure-en")],
  [Stamp], [#svg-badge("example-stamp", variant: "chip")], [#native-badge("example-stamp")],
  [In prose], [Screening finds #svg-badge("mild-npdr", variant: "chip") in a fraction of the cohort, measured in #svg-badge("mmhg", variant: "chip").], [Screening finds #native-badge("mild-npdr") in a fraction of the cohort, measured in #native-badge("mmhg").],
)

The same table with the renderer's text.svg in the middle column:

#table(
  columns: (auto, 1fr, 1fr),
  stroke: 0.4pt + luma(160),
  inset: 5pt,
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  table.header([*Option*], [*text.svg*], [*native (rect grid)*]),
  [Primary], [#svg-badge("heart-failure", variant: "text")], [#native-badge("heart-failure", icon: "grid")],
  [Long label], [#svg-badge("mild-npdr", variant: "text")], [#native-badge("mild-npdr", icon: "grid")],
  [Stamp], [#svg-badge("example-stamp", variant: "text")], [#native-badge("example-stamp", icon: "grid")],
)

== (c) Glossary-like entry list

#set terms(separator: [ — ], hanging-indent: 1.5em)
#terms(
  terms.item(native-badge("heart-failure"), [A clinical syndrome resulting from structural or functional impairment of ventricular filling or ejection of blood, characterized by dyspnea, fatigue, and fluid retention. SNOMED CT 84114007.]),
  terms.item(native-badge("mmhg"), [Millimetres of mercury, the conventional unit of blood pressure. SNOMED CT 259018001.]),
  terms.item(native-badge("mild-npdr"), [The earliest stage of diabetic retinopathy: microaneurysms only, without the venous beading or intraretinal microvascular abnormalities of later stages. SNOMED CT 312903003.]),
  terms.item(native-badge("description-pattern"), [The Tinkar pattern every description semantic conforms to: language, text, case significance, description type.]),
  terms.item(native-badge("heart-failure-en"), [The English fully specified name of the Heart Failure concept.]),
  terms.item(native-badge("example-stamp"), [Provenance — status, time, author, module, path — of one component version.]),
)

Same list with the chip SVG as the term:

#terms(
  terms.item(svg-badge("heart-failure", variant: "chip"), [A clinical syndrome resulting from structural or functional impairment of ventricular filling or ejection of blood.]),
  terms.item(svg-badge("mild-npdr", variant: "chip"), [The earliest stage of diabetic retinopathy: microaneurysms only, without venous beading.]),
  terms.item(svg-badge("example-stamp", variant: "chip"), [Provenance of one component version.]),
)

#pagebreak()
== (d) Side by side: SVG embedding vs. native drawing

#let row(name) = (
  [#raw(name)],
  [#svg-badge(name, variant: "text")],
  [#svg-badge(name, variant: "chip")],
  [#native-badge(name)],
  [#native-badge(name, icon: "grid")],
)
#table(
  columns: (auto, auto, auto, auto, auto),
  stroke: none,
  inset: 4pt,
  align: left + horizon,
  table.header([], [*text.svg*], [*chip.svg*], [*native png*], [*native grid*]),
  ..badges.map(b => row(b.name)).flatten(),
)

At 24 pt, to look for blurring and clipping — chip.svg, native png (pixelated), native rect grid:

#text(size: 24pt)[#svg-badge("heart-failure", variant: "chip") #native-badge("heart-failure") #native-badge("heart-failure", icon: "grid")]

#text(size: 24pt)[#svg-badge("example-stamp", variant: "chip") #native-badge("example-stamp")]

Identicon alone at 4 em — PNG with `scaling: "pixelated"`, PNG with `scaling: "smooth"`, rect grid:

#box(image("badges/heart-failure.identicon.png", width: 4em, scaling: "pixelated"))
#h(1em)
#box(image("badges/heart-failure.identicon.png", width: 4em, scaling: "smooth"))
#h(1em)
#identicon-grid("heart-failure", 4em)

A retired referent, native: #native-badge("heart-failure", inactive: true) — strikethrough in the inactive colour.
