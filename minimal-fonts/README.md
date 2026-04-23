# IKE Minimal Fonts

Lightweight Noto font package containing only the essential fonts needed for IKE documentation.

## Why This Exists

The full `jasper-noto-fonts` artifact is **1.36 GB** with thousands of font files for hundreds
of languages and writing systems. For IKE documentation, we only need **13 font files (~4 MB)**
for English text, mathematical operators, and symbols.

This project downloads those 13 fonts directly from the official
[notofonts](https://github.com/notofonts) GitHub releases and packages them as a minimal
Maven artifact. No transitive dependencies.

## Fonts Included

### Serif (Body Text) — notofonts/latin-greek-cyrillic NotoSerif v2.015
- NotoSerif-Regular.ttf
- NotoSerif-Italic.ttf
- NotoSerif-Bold.ttf
- NotoSerif-BoldItalic.ttf

### Sans-Serif (Headings) — notofonts/latin-greek-cyrillic NotoSans v2.015
- NotoSans-Regular.ttf
- NotoSans-Italic.ttf
- NotoSans-Bold.ttf
- NotoSans-BoldItalic.ttf

### Monospace (Code) — notofonts/latin-greek-cyrillic NotoSansMono v2.014
- NotoSansMono-Regular.ttf
- NotoSansMono-Bold.ttf

### Mathematical Operators (Fallback) — notofonts/math v3.000
- NotoSansMath-Regular.ttf — covers ⊑ ⊓ ∃ ∀ ≡ and full Unicode Mathematical Operators block (U+2200–U+22FF)

### Symbols (Fallback) — notofonts/symbols
- NotoSansSymbols-Regular.ttf (v2.003) — circled/boxed letters, alchemical, astronomical, technical symbols
- NotoSansSymbols2-Regular.ttf (v2.008) — arrows, dingbats, geometric shapes, Braille, chess, transport

> **Note:** Maven downloads all fonts automatically from GitHub on first run.
> Subsequent runs skip the download if `src/main/resources/*.ttf` already exist.

## Installation

```bash
# Build and install to local Maven repository
mvn clean install
```

This creates `ike-minimal-fonts-1.0.0.jar` (~4 MB) in your local Maven repository.

## Usage in Projects

Replace the jasper-noto-fonts dependency with ike-minimal-fonts:

```xml
<!-- BEFORE: 1.36 GB download -->
<dependency>
    <groupId>io.github.ramezakasheh</groupId>
    <artifactId>jasper-noto-fonts</artifactId>
    <version>1.0.4</version>
</dependency>

<!-- AFTER: ~4 MB local artifact, zero transitive deps -->
<dependency>
    <groupId>network.ike</groupId>
    <artifactId>ike-minimal-fonts</artifactId>
    <version>1.0.0</version>
</dependency>
```

Font paths remain the same — fonts are flattened to the root of the JAR.

## Benefits

- **340x smaller**: 4 MB vs 1.36 GB
- **Zero dependencies**: No Maven transitive downloads
- **Reproducible**: Pinned to specific notofonts release versions
- **Much faster builds**: No massive downloads after first run
- **DL axiom support**: Mathematical operators ⊑ ⊓ ∃ render correctly in PDF
- **Robust symbol coverage**: Arrows, dingbats, geometric shapes, Braille, technical symbols

## Publishing (Optional)

To share with your team, deploy to your Maven repository:

```bash
mvn deploy
```

Update your `<distributionManagement>` section to point to your Nexus/Artifactory instance.

## License

This package repackages fonts from Google's Noto project.

- Text fonts: https://github.com/notofonts/latin-greek-cyrillic
- Math font: https://github.com/notofonts/math (v3.000)
- Symbol fonts: https://github.com/notofonts/symbols
- License: SIL Open Font License 1.1
