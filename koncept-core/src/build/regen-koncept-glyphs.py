#!/usr/bin/env python3
"""Regenerates the bundled IKE Koncept Glyphs face (IKE-Network/ike-issues#953).

The face is a ~12 KB merged subset of three Noto donors — the same notofonts
releases the sibling minimal-fonts module pins for the PDF fallback chain — so
every IKE surface renders the Koncept glyph vocabulary from one spec-owned file
instead of whatever the ambient OS font fallback answers with (the #953 flip:
Apple SD Gothic Neo ships U+22CE/U+22CF swapped).

Donors (all SIL OFL 1.1, no Reserved Font Names), hinted statics:
  NotoSansMath-Regular     notofonts/math                 NotoSansMath-v3.000
  NotoSansSymbols2-Regular notofonts/symbols              NotoSansSymbols2-v2.008
  NotoSansSymbols-Regular  notofonts/symbols              NotoSansSymbols-v2.003
  NotoSans-Regular         notofonts/latin-greek-cyrillic NotoSans-v2.015 (punctuation)

Codepoints are assigned to donors in that priority order (first donor whose
cmap covers a codepoint wins), so adding a glyph to INVENTORY is normally the
only edit this script ever needs.

The authoritative inventory lives in KonceptGlyphs.java; KonceptGlyphsTest
fails the koncept-core build if the checked-in TTF ever lags it. This script's
INVENTORY is the build-side mirror of that list.

Usage:
  pip install fonttools
  python3 regen-koncept-glyphs.py [--donors-dir <minimal-fonts>/target/downloads]

Without --donors-dir the pinned release zips are downloaded to a temp dir.
Donor TTFs are sha256-verified either way. Output (overwritten in place):
  ../main/resources/network/ike/docs/konceptcore/IKEKonceptGlyphs-Regular.ttf
"""

import argparse
import hashlib
import io
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

try:
    from fontTools import subset
    from fontTools.ttLib import TTFont
except ImportError:
    sys.exit("fontTools is required: pip install fonttools")

FAMILY = "IKE Koncept Glyphs"
PS_NAME = "IKEKonceptGlyphs-Regular"
VERSION = "1.000"

# The glyph vocabulary (mirror of KonceptGlyphs.java — the gate compares the two).
INVENTORY = [
    # layout
    0x0020,  # SPACE (cluster strings carry separators)
    0x00A0,  # NO-BREAK SPACE (adoc chip line glue)
    # logical-status cluster (KonceptStatus)
    0x2261,  # ≡ defined copula
    0x2291,  # ⊑ primitive copula
    0x22A4,  # ⊤ root
    0x22CE,  # ⋎ multi-parent fork
    0x22CF,  # ⋏ fork sibling (docs/tests pair it with ⋎)
    # DL / axiom operators
    0x2203,  # ∃ existential
    0x2293,  # ⊓ conjunction
    0x2294,  # ⊔ disjunction
    0x2192,  # → role arrow
    0x21D2,  # ⇒ double arrow (ClauseView)
    0x279E,  # ➞ heavy arrow (ClauseView)
    0x2264,  # ≤ concrete domain
    0x2265,  # ≥ concrete domain
    0x03C0,  # π projection (ClauseView)
    0x03C3,  # σ selection (ClauseView)
    0x24A1,  # ⒡ feature marker (ClauseView)
    0x2B20,  # ⬠ STAMP pentagon stand-in (text channels)
    # chrome
    0x2026,  # … ellipsis
    0x00B7,  # · middle dot
    0x2022,  # • bullet
    0x2212,  # − minus
    0x221E,  # ∞ infinity
    0x25B2,  # ▲
    0x25BC,  # ▼
    0x25B6,  # ▶
    0x25C0,  # ◀
    0x25B8,  # ▸ tree disclosure
    0x25BE,  # ▾ tree disclosure
    0x25CC,  # ◌ dotted circle
    0x25CF,  # ● filled circle
    0x2630,  # ☰ handle
    0x26A0,  # ⚠ warning
    0x2713,  # ✓ check
    0x2715,  # ✕ dismiss
    0x2318,  # ⌘ command
    0x2B1B,  # ⬛ matrix cell
    0x2B1C,  # ⬜ matrix cell
]

NOTO_MATH = "https://github.com/notofonts/math/releases/download"
NOTO_SYMBOLS = "https://github.com/notofonts/symbols/releases/download"
NOTO_LGC = "https://github.com/notofonts/latin-greek-cyrillic/releases/download"

# (label, release zip URL, TTF path inside the zip, donors-dir path, sha256 of the TTF)
# Unhinted statics — the variant minimal-fonts itself packages. Hint programs cannot be
# transplanted across fonts, and neither Prism nor our raster pipeline executes them.
DONORS = [
    ("NotoSansMath",
     f"{NOTO_MATH}/NotoSansMath-v3.000/NotoSansMath-v3.000.zip",
     "NotoSansMath/unhinted/ttf/NotoSansMath-Regular.ttf",
     "NotoSansMath/unhinted/ttf/NotoSansMath-Regular.ttf",
     "b127e84699212b6b2ef50aff58e0ebebeec04ffe6db1b9eb9e209c8c3d97b4aa"),
    ("NotoSansSymbols2",
     f"{NOTO_SYMBOLS}/NotoSansSymbols2-v2.008/NotoSansSymbols2-v2.008.zip",
     "NotoSansSymbols2/unhinted/ttf/NotoSansSymbols2-Regular.ttf",
     "NotoSansSymbols2/unhinted/ttf/NotoSansSymbols2-Regular.ttf",
     "c4a0a80f0041ce4be81e2478faad22776d23edb98ae3f0d19bd37044820ecf9d"),
    ("NotoSansSymbols",
     f"{NOTO_SYMBOLS}/NotoSansSymbols-v2.003/NotoSansSymbols-v2.003.zip",
     "NotoSansSymbols/unhinted/ttf/NotoSansSymbols-Regular.ttf",
     "NotoSansSymbols/unhinted/ttf/NotoSansSymbols-Regular.ttf",
     "6eea9cb4cd39269ea9f95ba5c2735f80ae74049dfc9e1a7c932a5cfc8f0c3030"),
    ("NotoSans",
     f"{NOTO_LGC}/NotoSans-v2.015/NotoSans-v2.015.zip",
     "NotoSans/unhinted/ttf/NotoSans-Regular.ttf",
     "NotoSans/unhinted/ttf/NotoSans-Regular.ttf",
     "f3961a9cde016d41a4879aecda1474d3a36d6bf54fa0e4643de029cc2248b0e8"),
]

COPYRIGHT = (
    "IKE Koncept Glyphs: a merged subset of Noto Sans Math (Copyright 2022 The Noto "
    "Project Authors, https://github.com/notofonts/math), Noto Sans Symbols and Noto "
    "Sans Symbols 2 (Copyright 2022 The Noto Project Authors, "
    "https://github.com/notofonts/symbols), and Noto Sans (Copyright 2022 The Noto "
    "Project Authors, https://github.com/notofonts/latin-greek-cyrillic). Licensed "
    "under the SIL Open Font License, Version 1.1."
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def acquire_donor(label, url, zip_member, local_rel, digest, donors_dir, tmp: Path) -> Path:
    if donors_dir:
        candidate = donors_dir / local_rel
        if candidate.is_file():
            if sha256(candidate) != digest:
                sys.exit(f"{label}: {candidate} does not match the pinned sha256")
            return candidate
        print(f"{label}: not under --donors-dir, downloading the pinned release")
    print(f"{label}: downloading {url}")
    with urllib.request.urlopen(url) as response:
        payload = response.read()
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        data = archive.read(zip_member)
    out = tmp / Path(zip_member).name
    out.write_bytes(data)
    if sha256(out) != digest:
        sys.exit(f"{label}: downloaded TTF does not match the pinned sha256")
    return out


def subset_to(path: Path, codepoints, tmp: Path) -> Path:
    options = subset.Options()
    options.glyph_names = False
    options.name_IDs = []          # donor names are replaced wholesale after the merge
    options.notdef_outline = True
    options.hinting = False
    # Isolated symbols need no shaping/positioning, and cross-font table merging has no
    # sound semantics for layout tables — drop them outright.
    options.drop_tables += ["MATH", "GSUB", "GPOS", "GDEF"]
    font = subset.load_font(str(path), options)
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=codepoints)
    subsetter.subset(font)
    out = tmp / f"subset-{path.stem}.ttf"
    font.save(str(out))
    return out


def transplant(base: TTFont, donor: TTFont, prefix: str) -> None:
    """Copies every non-.notdef glyph of ``donor`` into ``base`` under prefixed names,
    remapping composite component references and extending the Unicode cmaps with the
    donor's codepoint mappings. Donor subsets are tiny and codepoint-disjoint by
    construction, which is what makes this transplant sound where a generic table
    merge is not."""
    if donor["head"].unitsPerEm != base["head"].unitsPerEm:
        sys.exit(f"{prefix}: unitsPerEm mismatch with the base font")
    base_glyf, donor_glyf = base["glyf"], donor["glyf"]
    base_hmtx, donor_hmtx = base["hmtx"], donor["hmtx"]
    renamed = {name: f"{prefix}.{name}"
               for name in donor.getGlyphOrder() if name != ".notdef"}
    seed = list(base.getGlyphOrder())
    for old, new in renamed.items():
        glyph = donor_glyf[old]
        if glyph.isComposite():
            for component in glyph.components:
                component.glyphName = renamed.get(component.glyphName, ".notdef")
        base_glyf[new] = glyph
        base_hmtx[new] = donor_hmtx[old]
    # glyf.__setitem__ may extend the live order itself — rebuild deduplicated.
    order = list(dict.fromkeys(seed + list(renamed.values())))
    base.setGlyphOrder(order)
    base_glyf.glyphOrder = order
    base["maxp"].numGlyphs = len(order)
    for cp, glyph_name in donor.getBestCmap().items():
        target = renamed.get(glyph_name)
        if target is None:
            continue
        for table in base["cmap"].tables:
            if table.isUnicode():
                table.cmap[cp] = target


def rename(font: TTFont) -> None:
    name = font["name"]
    name.names = []
    entries = {
        0: COPYRIGHT,
        1: FAMILY,
        2: "Regular",
        3: f"{FAMILY} {VERSION}; derived from Noto (notofonts) releases",
        4: f"{FAMILY} Regular",
        5: f"Version {VERSION}",
        6: PS_NAME,
        11: "https://ike.network",
        13: ("This Font Software is licensed under the SIL Open Font License, Version 1.1. "
             "See IKEKonceptGlyphs-OFL.txt alongside this file."),
        14: "https://openfontlicense.org",
    }
    for name_id, value in entries.items():
        name.setName(value, name_id, 3, 1, 0x409)
        name.setName(value, name_id, 1, 0, 0)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--donors-dir", type=Path, default=None,
                        help="minimal-fonts target/downloads dir with the pinned donors")
    args = parser.parse_args()

    script_dir = Path(__file__).resolve().parent
    output = (script_dir / "../main/resources/network/ike/docs/konceptcore/"
              "IKEKonceptGlyphs-Regular.ttf").resolve()

    with tempfile.TemporaryDirectory() as tmp_name:
        tmp = Path(tmp_name)
        remaining = set(INVENTORY)
        merged = None
        for label, url, zip_member, local_rel, digest in DONORS:
            donor_path = acquire_donor(label, url, zip_member, local_rel, digest,
                                       args.donors_dir, tmp)
            cmap = TTFont(str(donor_path)).getBestCmap()
            take = sorted(cp for cp in remaining if cp in cmap)
            if not take:
                continue
            piece = TTFont(str(subset_to(donor_path, take, tmp)))
            if merged is None:
                merged = piece
            else:
                transplant(merged, piece, label)
            remaining -= set(take)
            print(f"{label}: {len(take)} codepoints")
        if remaining:
            sys.exit("uncovered codepoints: "
                     + ", ".join(f"U+{cp:04X}" for cp in sorted(remaining)))

        rename(merged)
        output.parent.mkdir(parents=True, exist_ok=True)
        merged.save(str(output))

    check = TTFont(str(output))
    cmap = check.getBestCmap()
    missing = [f"U+{cp:04X}" for cp in INVENTORY if cp not in cmap]
    if missing:
        sys.exit("verification failed, missing: " + ", ".join(missing))
    print(f"wrote {output} ({output.stat().st_size} bytes, "
          f"{check['maxp'].numGlyphs} glyphs, {len(INVENTORY)} codepoints verified)")


if __name__ == "__main__":
    main()
