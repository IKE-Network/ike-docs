---
date_published: 2026-06-18
date_modified: 2026-06-18
canonical_url: https://github.com/IKE-Network/ike-docs/index.html
---

# Semantic Linebreak

A CLI tool that reformats AsciiDoc prose to use [semantic linefeeds](https://rhodesmill.org/brandon/2012/one-sentence-per-line/)[1] — one logical unit per line. The result is source text that produces cleaner `git diff` output, makes sentence-level review practical, and simplifies reordering and editing prose.

## [#why-semantic-linebreaks](#why-semantic-linebreaks)Why Semantic Linebreaks?

AsciiDoc is a plain-text markup language stored in version control. When prose is reflowed to an arbitrary column width, a single inserted word can cause every subsequent line in the paragraph to shift, producing a diff that obscures the actual change.

Semantic linebreaks solve this by placing line breaks at *logical* boundaries — sentences, clauses, and punctuation-delimited phrases. Each line captures one idea, so a change to one sentence appears as a clean one-line diff.

```
This document describes the Analysis Normal Form used
by HL7 CIMI. It was balloted in September 2019 as an
informative specification.
```

```
This document describes the Analysis Normal Form used by HL7 CIMI.
It was balloted in September 2019 as an informative specification.
```

When a reviewer later changes "September 2019" to "September 2020", only the second line shows up in the diff.

## [#how-it-works](#how-it-works)How It Works

The tool uses AsciidoctorJ to parse the document into an abstract syntax tree (AST). It identifies *paragraph* blocks that contain prose and reformats only those blocks. Delimited blocks — listings, diagrams, tables, passthroughs, and all other structural elements — are never touched.

Within each paragraph the tool:

1. Joins existing lines into a single string (respecting AsciiDoc hard line breaks ` +`)
2. Applies semantic breaking rules at logical boundaries
3. Soft-wraps any remaining long lines at word boundaries
4. Merges very short lines to prevent orphans

## [#breaking-rules](#breaking-rules)Breaking Rules

Rules are applied in priority order. The first matching rule at each position wins.

### [#own-line-macros](#own-line-macros)Own-line macros

AsciiDoc index terms and footnotes are placed on their own line. Content inside their brackets is never broken.

`` Placed on its own line. Comma inside brackets is syntax, not a clause boundary. `term` Same treatment as `indexterm`. `[[1](#footnotedef-1)]` Placed on its own line. Content inside the brackets *is* soft-wrapped because footnotes contain prose. `` Triple-paren hidden index entries are placed on their own line — they are invisible in rendered output. `visible term` Double-paren visible terms stay *inline* in the surrounding prose because they render as visible text.  

### [#sentence-boundaries](#sentence-boundaries)Sentence boundaries

A period, question mark, or exclamation point followed by a space and an uppercase letter triggers a line break.

Abbreviations are recognized and not treated as sentence ends. The tool knows about titles (Dr., Prof.), Latin abbreviations (e.g., i.e.), reference prefixes (Fig., Vol.), and single-letter initials (J. Smith).

Closing quotes after sentence punctuation (`."` `?"` `!"`) are also recognized.

### [#sub-sentence-boundaries](#sub-sentence-boundaries)Sub-sentence boundaries

When `--sentences-only` is *not* active (the default), the tool also breaks at:

Em-dash (Unicode or AsciiDoc `--`) Breaks after the dash. "The specification — originally from 2019 — was revised." becomes two lines. Semicolon Breaks after semicolons that separate independent clauses. Colon Breaks after colons, with guards to avoid breaking URLs (`https://`), times (`10:30`), and AsciiDoc definition lists (`Term::`). Comma + conjunction Breaks after a comma followed by a coordinating conjunction (and, but, or, yet, so, nor). Simple comma Breaks at any comma boundary. This is gated by `--clause-threshold` (default: 0, meaning all commas).  

## [#post-processing](#post-processing)Post-Processing

### [#soft-wrap](#soft-wrap)Soft wrap

After semantic breaking, any line longer than `--max-line-length` (default: 64) is soft-wrapped at the last word boundary before the limit.

Guards prevent wrapping inside AsciiDoc macro brackets where content is structured syntax (``, `…​`). Footnote brackets *are* wrappable because their content is prose.

An orphan guard (`--min-remainder`, default: 15) skips a wrap if the remainder after the break would be too short, preventing single-word runoff lines.

With the defaults of 64 + 15 = 79, no output line exceeds 80 characters unless a single word is longer than the limit.

### [#short-line-merge](#short-line-merge)Short-line merge

Lines shorter than `--min-line-length` (default: 10) are merged with the following line. This prevents orphan fragments — a single word like "A" sitting alone before a long phrase.

Own-line macros (indexterm, footnote, ``) are never merged, because they are intentionally isolated.

## [#usage](#usage)Usage

```
semantic-linebreak [options] <file.adoc ...>
semantic-linebreak [options] <directory ...>
```

The tool accepts one or more files, one or more directories, or a mix of both. When given a directory it walks recursively for `*.adoc` files, skipping `target/` directories.

AsciidoctorJ is initialized once and reused across all files, so batch mode is significantly faster than invoking per file. Files are modified in-place by default.

### [#invocation-methods](#invocation-methods)Invocation methods

From the ike-pipeline reactor (recommended for IKE projects)

```
# Entire directory tree:
mvn exec:java -pl semantic-linebreak \
  -Dexec.args="path/to/src/docs/asciidoc"

# Multiple files:
mvn exec:java -pl semantic-linebreak \
  -Dexec.args="chapter1.adoc chapter2.adoc chapter3.adoc"

# Single file:
mvn exec:java -pl semantic-linebreak \
  -Dexec.args="path/to/source.adoc"
```

Direct Java invocation (outside the reactor)

```
java -jar semantic-linebreak/target/semantic-linebreak-*.jar \
  path/to/src/docs/asciidoc
```

Preview without modifying

```
mvn exec:java -pl semantic-linebreak \
  -Dexec.args="-n path/to/source.adoc"
```

### [#options](#options)Options

| Option | Description |
| --- | --- |
| `-o, --output <file>` | Write to a file (single-file mode only; default: in-place) |
| `-n, --dry-run` | Print result to stdout without modifying files |
| `-v, --verbose` | Show which paragraphs are reformatted |
| `--sentences-only` | Break only at sentence boundaries (`.` `?` `!`) |
| `--clause-break` | Break on simple commas (default: on) |
| `--clause-threshold <n>` | Minimum line length before a comma break applies (default: 0) |
| `--max-line-length <n>` | Soft-wrap long lines at this column (default: 64) |
| `--min-remainder <n>` | Skip wrap if remainder is shorter than this (default: 15) |
| `--min-line-length <n>` | Merge lines shorter than this with the next line (default: 10) |
| `--no-wrap` | Disable soft wrapping entirely |
| `-h, --help` | Show usage summary |

## [#design-decisions](#design-decisions)Design Decisions

AST-aware reformatting Many text-reformatting tools operate on raw lines. This tool parses the full AsciiDoc AST via AsciidoctorJ so it can distinguish prose paragraphs from code listings, table cells, diagram blocks, and attribute declarations. Only paragraph blocks are reformatted. Bracket-depth tracking AsciiDoc uses square brackets for macro arguments (``). Commas and punctuation inside brackets are syntax, not prose boundaries. The tool tracks bracket nesting depth and skips all breaking rules inside `[…]`. Structured vs. prose brackets Not all bracket content is equal. `` contains structured syntax where wrapping would break the markup. `[[2](#footnotedef-2)]` contains prose that may be a full paragraph. The tool distinguishes these: index term brackets are never wrapped, footnote brackets are soft-wrapped at word boundaries. Invisible vs. visible index entries AsciiDoc has two shorthand index forms. `` is invisible in rendered output — it belongs on its own line. `visible term` renders as visible inline text and stays in the prose flow. Diff-optimized defaults The default parameters (max line length 64, min remainder 15, min line length 10) are chosen so that output lines stay under 80 characters, short orphan fragments are merged, and every line captures a meaningful unit of prose. The goal is not typographic line length but version-control-friendly source. No opinion on prose quality The tool reformats line boundaries. It does not flag overly long sentences, suggest restructuring, or critique writing style. Prose quality is a separate concern addressed by different tools.
