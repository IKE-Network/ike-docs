#set page(width: 80mm, height: 40mm, margin: 3mm)
#set text(font: "Noto Serif", size: 10.5pt)
#let b(..a) = box(baseline: 0.33em, image("../badges/mild-npdr.chip.svg", height: 1.45em, ..a))
#table(columns: (auto, 1fr, 1fr), stroke: 0.4pt, inset: 4pt,
  [default (cover)], [#b()], [x],
  [fit: contain], [#b(fit: "contain")], [x],
  [fit: stretch], [#b(fit: "stretch")], [x])
