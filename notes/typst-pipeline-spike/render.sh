#!/usr/bin/env bash
# Renders one AsciiDoc topic through the typst backend and the Typst CLI:
#   ./render.sh <topic.adoc> <name>
# Output under <name>/: <name>.typ, <name>.pdf, <name>-<page>.png, and
# warnings-*.txt / timing.txt capturing what Typst said and how long it took.
#
# Needs the ike-docs reactor built (./mvnw install on the converter and the
# koncept extension), a JDK, and `typst` on PATH. The classpath is the
# converter's test classpath as Maven resolves it (cp.txt beside this script,
# regenerated with `mvn dependency:build-classpath -Dmdep.includeScope=test`
# on typst-asciidoc-converter) plus the three modules' target/classes.
set -euo pipefail
cd "$(dirname "$0")"
ROOT=../..
TOPIC=$1
NAME=$2
FONTS=${FONTS:-$ROOT/notes/typst-badge-demo/fonts}
CP="$(cat cp.txt):$ROOT/typst-asciidoc-converter/target/classes:$ROOT/koncept-asciidoc-extension/target/classes:$ROOT/koncept-core/target/classes"

mkdir -p "$NAME"
java --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow \
  -cp "$CP" Render.java "$TOPIC" "$NAME/$NAME.typ" 2> "$NAME/converter-log.txt"

typst --version > "$NAME/typst-version.txt"
# Compile root: the ike-dev folder, so any image path under a sibling working set resolves.
{ time typst compile --root /Users/kec/ike-dev --font-path "$FONTS" --format pdf "$NAME/$NAME.typ" "$NAME/$NAME.pdf" ; } 2> "$NAME/warnings-pdf.txt" || true
{ time typst compile --root /Users/kec/ike-dev --font-path "$FONTS" --format png --ppi 110 "$NAME/$NAME.typ" "$NAME/$NAME-{p}.png" ; } 2> "$NAME/warnings-png.txt" || true
echo "--- $NAME: warnings (pdf) ---"; cat "$NAME/warnings-pdf.txt"
ls "$NAME"
