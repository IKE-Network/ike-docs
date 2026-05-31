---
date_published: 2026-05-30
date_modified: 2026-05-30
canonical_url: https://github.com/IKE-Network/ike-minimal-fonts/index.html
---

# Minimal Fonts

A minimal Noto font subset (~4 MB) for consistent PDF rendering across all IKE documentation. Font files are downloaded from Google’s Noto Fonts GitHub releases at build time and packaged as a JAR artifact.

## [#included-fonts](#included-fonts)Included Fonts

| Font Family | Weights |
| --- | --- |
| Noto Serif | Regular, Bold, Italic, Bold Italic |
| Noto Sans | Regular, Bold, Italic, Bold Italic |
| Noto Sans Mono | Regular, Bold |
| Noto Sans Math | Regular |
| Noto Sans Symbols | Regular |
| Noto Sans Symbols2 | Regular |

## [#why-these-fonts](#why-these-fonts)Why These Fonts?

The Noto family provides comprehensive Unicode coverage including mathematical notation, clinical terminology symbols, and international characters commonly found in health informatics documents. The subset covers Latin, Greek, and common symbol ranges without the full Noto distribution (~1 GB).

## [#build](#build)Build

No fonts are committed to Git. The `download-maven-plugin` fetches TTF files from GitHub during `generate-resources` and caches them in `~/.m2/repository/.cache/download-maven-plugin/`. Subsequent builds reuse the cache.

```
mvn install
```

## [#license](#license)License

Noto Fonts are licensed under the SIL Open Font License, Version 1.1.
