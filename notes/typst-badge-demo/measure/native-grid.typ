#set page(width: 80mm, height: 20mm, margin: 3mm)
#set text(font: "Noto Serif", size: 10.5pt)
#let cells = json("../badges/heart-failure.cells.json")
Lorem #box(fill: rgb("#e9eff6"), radius: 0.5em, inset: (x: 0.4em, y: 0.08em))[#box(baseline: 0.12em, width: 0.9em, height: 0.9em, clip: true, grid(columns: (1fr,)*32, rows: (1fr,)*32, gutter: 0pt, ..cells.flatten().map(c => rect(width: 100%, height: 100%, fill: rgb(c), stroke: none)))) #text(font: "Alegreya Sans SC", weight: 500, fill: rgb("#2a5a8a"))[Heart Failure]].
