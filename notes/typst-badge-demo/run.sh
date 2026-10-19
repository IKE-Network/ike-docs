#!/usr/bin/env bash
# Regenerates the badge assets with the renderers that ship in
# koncept-asciidoc-extension, then compiles demo.typ to PDF, PNG and SVG.
#
#   ./run.sh            # everything
#   ./run.sh badges     # only the Java step
#   ./run.sh typst      # only the Typst step
#
# Needs: a JDK (27 here), the toucan jar in ~/.m2, and `typst` on PATH.
set -euo pipefail
cd "$(dirname "$0")"
ROOT=../..                                  # the ike-docs reactor root
TOUCAN=~/.m2/repository/com/sparrowwallet/toucan/0.9.0/toucan-0.9.0.jar
CLASSES=${TMPDIR:-/tmp}/typst-badge-demo-classes
STEP=${1:-all}

if [[ $STEP == all || $STEP == badges ]]; then
  mkdir -p "$CLASSES"
  javac --enable-preview --release 27 -nowarn -d "$CLASSES" -cp "$TOUCAN" \
    "$ROOT"/koncept-core/src/main/java/network/ike/docs/konceptcore/*.java \
    "$ROOT"/koncept-asciidoc-extension/src/main/java/network/ike/docs/koncept/KonceptSvgRenderer.java \
    "$ROOT"/koncept-asciidoc-extension/src/main/java/network/ike/docs/koncept/IdenticonRenderer.java \
    "$ROOT"/koncept-asciidoc-extension/src/main/java/network/ike/docs/koncept/SnomedUuids.java
  cp -r "$ROOT"/koncept-core/src/main/resources/* "$CLASSES"/
  java --enable-preview -cp "$CLASSES:$TOUCAN" BadgeSvgs.java badges
fi

if [[ $STEP == all || $STEP == typst ]]; then
  mkdir -p out
  typst --version | tee out/typst-version.txt
  typst fonts --font-path fonts --ignore-system-fonts > out/fonts-seen.txt
  # Every warning Typst prints is kept beside the output it belongs to.
  typst compile --root . --font-path fonts demo.typ out/demo.pdf            2> out/warnings-pdf.txt  || true
  typst compile --root . --font-path fonts --format png --ppi 144 demo.typ out/demo-144-{p}.png 2> out/warnings-png144.txt || true
  typst compile --root . --font-path fonts --format png --ppi 288 demo.typ out/demo-288-{p}.png 2> out/warnings-png288.txt || true
  typst compile --root . --font-path fonts --format svg demo.typ out/demo-{p}.svg 2> out/warnings-svg.txt || true
  echo "--- warnings (pdf) ---"; cat out/warnings-pdf.txt
  ls -la out
fi
