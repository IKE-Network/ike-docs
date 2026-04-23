package network.ike.tools.linebreak;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cursor;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Reformats AsciiDoc files to use semantic linefeeds.
 * <p>
 * Line breaks are placed at logical boundaries — sentences, clauses, asides,
 * introductions, and compound-sentence joints — producing source text that is
 * easier to diff, edit, and reason about.
 * <p>
 * Uses AsciidoctorJ to parse the document AST, identifying paragraph blocks that
 * contain prose. Only those blocks are reformatted; delimited blocks (listings,
 * diagrams, tables, passthroughs, etc.) are never touched.
 * <p>
 * Default breaking rules (in priority order):
 * <ol>
 *   <li>Sentence ends: {@code . ? !} followed by a space and uppercase letter</li>
 *   <li>Closing quote after sentence: {@code ." ?" !"} followed by space and uppercase</li>
 *   <li>Em-dash (Unicode {@code \u2014}) followed by space</li>
 *   <li>Em-dash (AsciiDoc {@code --}) surrounded by spaces</li>
 *   <li>Semicolon followed by space</li>
 *   <li>Colon followed by space (guarded against URLs, times, definition lists)</li>
 *   <li>Comma followed by coordinating conjunction ({@code and, but, or, yet, so, nor})</li>
 *   <li>Simple comma clause break (optional, threshold-gated)</li>
 * </ol>
 * <p>
 * Use {@code --sentences-only} to restrict breaking to sentence boundaries only
 * (rules 1-2 above).
 * <p>
 * Hard line breaks ({@code " +"} at end of line) are preserved.
 * Abbreviations (Dr., Mr., e.g., i.e., etc.) are recognized and not treated as sentence ends.
 */
public class SemanticLineBreaker {

    /** Creates a semantic line breaker instance. */
    public SemanticLineBreaker() {}

    // ── Abbreviations that end with a period but do NOT end a sentence ──────────

    private static final Set<String> ABBREVIATIONS = Set.of(
            // Titles and honorifics
            "Dr", "Mr", "Mrs", "Ms", "Prof", "Sr", "Jr", "St", "Rev",
            // Military / government titles
            "Gen", "Gov", "Rep", "Sen", "Sgt", "Cpl", "Pvt",
            "Capt", "Lt", "Col", "Maj", "Cmdr", "Adm", "Pres",
            // Latin abbreviations
            "vs", "cf",
            // Multi-period Latin (matched as token before final period)
            "e.g", "i.e",
            // Reference prefixes
            "Fig", "No", "Vol", "Sec", "Ref", "Ed", "Ch", "Pt",
            // Common abbreviations
            "approx", "dept", "govt", "est",
            // Multi-period geographic / time
            "U.S", "U.K", "A.M", "P.M", "a.m", "p.m"
    );

    // ── Coordinating conjunctions for comma+conjunction breaks ─────────────────

    private static final List<String> CONJUNCTIONS =
            List.of("and ", "but ", "or ", "yet ", "so ", "nor ");

    // ── AsciiDoc inline macros that should start on their own line ────────────
    // These are block-level annotations embedded in paragraph text.  The tool
    // emits a line break *before* each one so they sit on a dedicated line,
    // and never breaks *inside* their bracket arguments.

    private static final List<String> OWN_LINE_MACROS =
            List.of("indexterm:[", "indexterm2:[", "footnote:[");

    // ── AsciiDoc preprocessor directives ────────────────────────────────────────
    // These are evaluated and removed before the AST is built, so they are
    // invisible to the parser.  When a paragraph range in the source contains
    // directive lines, the tool must preserve them in-place.

    private static final Pattern PREPROCESSOR_DIRECTIVE = Pattern.compile(
            "^(ifdef::|ifndef::|ifeval::|endif::|include::).*");

    // ── Configuration ───────────────────────────────────────────────────────────

    private boolean sentencesOnly = false;
    private boolean clauseBreak = true;
    private int clauseBreakThreshold = 0;
    private int maxLineLength = 64;
    private int minRemainder = 15;
    private int minLineLength = 10;
    private boolean dryRun = false;
    private boolean verbose = false;

    // ── Configuration setters (package-private, for Mojo use) ───────────────

    /**
     * Restrict breaking to sentence boundaries only.
     *
     * @param sentencesOnly true to break only at sentence ends
     */
    void setSentencesOnly(boolean sentencesOnly) { this.sentencesOnly = sentencesOnly; }

    /**
     * Enable comma clause breaks.
     *
     * @param clauseBreak true to break at comma clauses
     */
    void setClauseBreak(boolean clauseBreak) { this.clauseBreak = clauseBreak; }

    /**
     * Set the minimum line length before comma breaks are applied.
     *
     * @param clauseBreakThreshold minimum characters before a comma break
     */
    void setClauseBreakThreshold(int clauseBreakThreshold) { this.clauseBreakThreshold = clauseBreakThreshold; }

    /**
     * Set the soft-wrap line length limit.
     *
     * @param maxLineLength maximum characters per line (use {@code Integer.MAX_VALUE} to disable)
     */
    void setMaxLineLength(int maxLineLength) { this.maxLineLength = maxLineLength; }

    /**
     * Set the minimum remainder after a soft wrap break.
     *
     * @param minRemainder minimum characters after a wrap break point
     */
    void setMinRemainder(int minRemainder) { this.minRemainder = minRemainder; }

    /**
     * Set the merge threshold for short lines.
     *
     * @param minLineLength lines shorter than this are merged with the next line
     */
    void setMinLineLength(int minLineLength) { this.minLineLength = minLineLength; }

    /**
     * Enable dry-run mode (print to stdout, don't modify files).
     *
     * @param dryRun true for dry-run mode
     */
    void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    /**
     * Enable verbose output showing which paragraphs are reformatted.
     *
     * @param verbose true for verbose output
     */
    void setVerbose(boolean verbose) { this.verbose = verbose; }

    // ── Entry point ─────────────────────────────────────────────────────────────

    /**
     * Command-line entry point for the semantic linebreak reformatter.
     *
     * @param args command-line arguments specifying input files and options
     * @throws IOException if an I/O error occurs while reading or writing files
     */
    public static void main(String[] args) throws IOException {
        var tool = new SemanticLineBreaker();
        tool.run(args);
    }

    private void run(String[] args) throws IOException {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        List<String> inputPaths = new ArrayList<>();
        String outputPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--output", "-o" -> outputPath = args[++i];
                case "--sentences-only" -> sentencesOnly = true;
                case "--clause-break" -> clauseBreak = true;
                case "--clause-threshold" -> clauseBreakThreshold = Integer.parseInt(args[++i]);
                case "--max-line-length" -> maxLineLength = Integer.parseInt(args[++i]);
                case "--min-remainder" -> minRemainder = Integer.parseInt(args[++i]);
                case "--min-line-length" -> minLineLength = Integer.parseInt(args[++i]);
                case "--no-wrap" -> maxLineLength = Integer.MAX_VALUE;
                case "--dry-run", "-n" -> dryRun = true;
                case "--verbose", "-v" -> verbose = true;
                case "--help", "-h" -> { printUsage(); System.exit(0); }
                default -> {
                    if (args[i].startsWith("-")) {
                        System.err.println("Unknown option: " + args[i]);
                        System.exit(1);
                    }
                    inputPaths.add(args[i]);
                }
            }
        }

        if (inputPaths.isEmpty()) {
            System.err.println("No input file specified.");
            System.exit(1);
        }

        // ── Resolve input paths: expand directories to *.adoc files ─────────
        List<Path> filesToProcess = new ArrayList<>();
        for (String input : inputPaths) {
            Path p = Path.of(input);
            if (!Files.exists(p)) {
                System.err.println("File not found: " + input);
                System.exit(1);
            }
            if (Files.isDirectory(p)) {
                collectAdocFiles(p, filesToProcess);
            } else {
                filesToProcess.add(p);
            }
        }

        if (filesToProcess.isEmpty()) {
            System.err.println("No .adoc files found.");
            System.exit(0);
        }

        // --output is only valid with a single file
        if (outputPath != null && filesToProcess.size() > 1) {
            System.err.println("--output cannot be used with multiple input files.");
            System.exit(1);
        }

        // ── Create AsciidoctorJ once, process all files ─────────────────────
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            int totalFiles = filesToProcess.size();
            int totalReformatted = 0;

            if (totalFiles > 1) {
                System.err.printf("Processing %d file(s)...%n", totalFiles);
            }

            for (int f = 0; f < totalFiles; f++) {
                Path inPath = filesToProcess.get(f);
                String effectiveOutput = outputPath;
                if (effectiveOutput == null && !dryRun) {
                    effectiveOutput = inPath.toString();
                }

                int reformatted = processFile(asciidoctor, inPath, effectiveOutput);
                if (reformatted > 0) totalReformatted++;
            }

            if (totalFiles > 1) {
                System.err.printf("Done. %d of %d file(s) had changes.%n",
                        totalReformatted, totalFiles);
            }
        }
    }

    // ── Single-file processing ───────────────────────────────────────────────

    /**
     * Process a single AsciiDoc file. Returns 1 if any paragraphs were
     * reformatted, 0 otherwise.
     *
     * @param asciidoctor the AsciidoctorJ instance for AST parsing
     * @param inPath the input file path
     * @param outputPath the output file path (null for stdout in dry-run)
     * @return 1 if paragraphs were reformatted, 0 otherwise
     * @throws IOException if an I/O error occurs
     */
    int processFile(Asciidoctor asciidoctor, Path inPath,
                    String outputPath) throws IOException {
        List<String> sourceLines = Files.readAllLines(inPath);
        String source = String.join("\n", sourceLines);

        Options options = Options.builder()
                .sourcemap(true)
                .safe(SafeMode.SAFE)
                .build();

        Document doc = asciidoctor.load(source, options);

        // ── Collect paragraph line ranges ───────────────────────────────
        List<int[]> paragraphRanges = new ArrayList<>();
        collectParagraphRanges(doc, paragraphRanges);

        paragraphRanges.sort(Comparator.comparingInt(r -> r[0]));

        if (verbose) {
            System.err.printf("%s: %d paragraph(s) to process.%n",
                    inPath, paragraphRanges.size());
            for (int[] range : paragraphRanges) {
                System.err.printf("  lines %d-%d%n", range[0] + 1, range[1] + 1);
            }
        }

        // ── Apply sentence breaks in reverse order ──────────────────────
        List<String> result = new ArrayList<>(sourceLines);
        int changes = 0;

        for (int i = paragraphRanges.size() - 1; i >= 0; i--) {
            int start = paragraphRanges.get(i)[0];
            int end = paragraphRanges.get(i)[1];

            // ── Separate preprocessor directives from prose ─────────────
            // The AST parser strips ifdef/endif/ifndef/ifeval/include
            // directives before building the tree, so the reported paragraph
            // line range may span source lines that contain directives.
            // We extract directives (with their 0-based positions relative
            // to the range), reformat only the prose lines, then reinsert
            // the directives at their original relative positions.

            var rawLines = new ArrayList<>(result.subList(start, end + 1));
            List<int[]> directivePositions = new ArrayList<>(); // [relativeIndex, ...]
            List<String> proseLines = new ArrayList<>();

            for (int k = 0; k < rawLines.size(); k++) {
                if (PREPROCESSOR_DIRECTIVE.matcher(rawLines.get(k)).matches()) {
                    directivePositions.add(new int[]{k, directivePositions.size()});
                } else {
                    proseLines.add(rawLines.get(k));
                }
            }

            String joined = joinParagraph(proseLines);
            List<String> broken = breakSemanticLines(joined);

            // Reinsert directives at their original positions
            if (!directivePositions.isEmpty()) {
                List<String> merged = new ArrayList<>(broken);
                for (int[] pos : directivePositions) {
                    int insertAt = Math.min(pos[0], merged.size());
                    merged.add(insertAt, rawLines.get(pos[0]));
                }
                broken = merged;
            }

            if (!broken.equals(rawLines)) {
                changes++;
                if (verbose) {
                    System.err.printf("  Reformatting lines %d-%d (%d lines -> %d lines)%n",
                            start + 1, end + 1, end - start + 1, broken.size());
                }
                for (int j = end; j >= start; j--) {
                    result.remove(j);
                }
                result.addAll(start, broken);
            }
        }

        System.err.printf("%s: %d paragraph(s), reformatted %d.%n",
                inPath.getFileName(), paragraphRanges.size(), changes);

        // ── Write output ────────────────────────────────────────────────
        String output = String.join("\n", result);
        if (source.endsWith("\n")) {
            output += "\n";
        }

        if (dryRun) {
            System.out.print(output);
        } else if (changes > 0) {
            Files.writeString(Path.of(outputPath), output);
            System.err.println("  Wrote: " + outputPath);
        }

        return changes > 0 ? 1 : 0;
    }

    // ── Directory Walking ────────────────────────────────────────────────────

    /**
     * Recursively collect all {@code *.adoc} files under a directory,
     * skipping {@code target/} directories.
     *
     * @param dir the directory to search
     * @param result list to accumulate found files into
     * @throws IOException if an I/O error occurs during traversal
     */
    static void collectAdocFiles(Path dir, List<Path> result) throws IOException {
        PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:*.adoc");
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
                if (d.getFileName() != null && d.getFileName().toString().equals("target")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (matcher.matches(file.getFileName())) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ── AST Walking ─────────────────────────────────────────────────────────────

    /**
     * Recursively walk the AST and collect [startLine, endLine] (0-based) for
     * every paragraph block. Skips paragraphs without source location (e.g.,
     * generated or from included files).
     */
    private void collectParagraphRanges(StructuralNode node, List<int[]> ranges) {
        if ("paragraph".equals(node.getContext())) {
            Cursor loc = node.getSourceLocation();
            if (loc != null && node instanceof Block block) {
                List<String> lines = block.getLines();
                if (lines != null && !lines.isEmpty()) {
                    int startLine = loc.getLineNumber() - 1; // convert to 0-based
                    int endLine = startLine + lines.size() - 1;
                    ranges.add(new int[]{startLine, endLine});
                }
            }
            // Paragraphs don't have child blocks, so no need to recurse further
            return;
        }

        // Recurse into child blocks (sections, lists, admonitions, etc.)
        List<StructuralNode> children = node.getBlocks();
        if (children != null) {
            for (StructuralNode child : children) {
                collectParagraphRanges(child, ranges);
            }
        }
    }

    // ── Paragraph Joining ───────────────────────────────────────────────────────

    /**
     * Join paragraph lines into a single string, respecting AsciiDoc hard line
     * breaks ({@code " +"} at end of line). Hard breaks are preserved as literal
     * {@code \n} boundaries that won't be reformatted.
     */
    private String joinParagraph(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i > 0) {
                sb.append(' ');
            }
            // Preserve hard line breaks: " +" at end of line
            if (line.endsWith(" +")) {
                sb.append(line, 0, line.length() - 2);
                sb.append(" +\n"); // keep as a forced break boundary
            } else {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    // ── Semantic Line Breaking ─────────────────────────────────────────────────

    /**
     * Break a joined paragraph into one-sentence-per-line.
     * <p>
     * If the paragraph contains hard line breaks ({@code \n} from " +" markers),
     * each segment is broken independently.
     */
    List<String> breakSemanticLines(String text) {
        // Handle hard line breaks as independent segments
        if (text.contains(" +\n")) {
            List<String> result = new ArrayList<>();
            String[] segments = text.split(" \\+\n");
            for (int s = 0; s < segments.length; s++) {
                List<String> broken = mergeShortLines(softWrap(breakSegment(segments[s].trim())));
                if (s < segments.length - 1 && !broken.isEmpty()) {
                    // Re-attach the hard break marker to the last line of this segment
                    int last = broken.size() - 1;
                    broken.set(last, broken.get(last) + " +");
                }
                result.addAll(broken);
            }
            return result;
        }
        return mergeShortLines(softWrap(breakSegment(text)));
    }

    /**
     * Core breaking logic for a single segment of prose (no hard breaks within).
     */
    private List<String> breakSegment(String text) {
        if (text.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int len = text.length();
        int i = 0;
        int bracketDepth = 0; // track nesting inside [...] (macro arguments)

        while (i < len) {
            char c = text.charAt(i);

            // ── Break before AsciiDoc macros that get their own line ─────────
            // indexterm:[], indexterm2:[], footnote:[], and shorthand index
            // entries (((…))) and ((…)).  Emit accumulated prose as one line,
            // then consume the entire macro (including brackets) into a new line.
            if (bracketDepth == 0) {
                String rest = text.substring(i);

                // Check for named macros: indexterm:[…], indexterm2:[…], footnote:[…]
                String matchedMacro = null;
                for (String macro : OWN_LINE_MACROS) {
                    if (rest.startsWith(macro)) {
                        matchedMacro = macro;
                        break;
                    }
                }

                if (matchedMacro != null) {
                    // Flush any accumulated prose before the macro
                    String prose = current.toString().trim();
                    if (!prose.isEmpty()) {
                        result.add(prose);
                        current.setLength(0);
                    }
                    // Consume the macro through its closing bracket
                    int depth = 0;
                    int j = i;
                    while (j < len) {
                        char mc = text.charAt(j);
                        current.append(mc);
                        if (mc == '[') depth++;
                        else if (mc == ']') {
                            depth--;
                            if (depth == 0) { j++; break; }
                        }
                        j++;
                    }
                    result.add(current.toString().trim());
                    current.setLength(0);
                    i = j;
                    // Skip trailing space after macro
                    if (i < len && text.charAt(i) == ' ') i++;
                    continue;
                }

                // Check for hidden shorthand index: (((…)))
                // Only triple-paren gets its own line — it's invisible in output.
                // Double-paren ((…)) is a visible inline term; leave it in prose.
                if (rest.startsWith("(((")) {
                    String prose = current.toString().trim();
                    if (!prose.isEmpty()) {
                        result.add(prose);
                        current.setLength(0);
                    }
                    String closer = ")))";
                    int closeIdx = rest.indexOf(closer, 3);
                    if (closeIdx >= 0) {
                        int endIdx = closeIdx + closer.length();
                        current.append(rest, 0, endIdx);
                        result.add(current.toString().trim());
                        current.setLength(0);
                        i += endIdx;
                        if (i < len && text.charAt(i) == ' ') i++;
                        continue;
                    }
                    // No closing found — fall through to normal processing
                }
            }

            current.append(c);

            // ── Track square bracket depth ───────────────────────────────────
            // Skip all breaking rules inside [...] — these contain AsciiDoc
            // macro arguments (indexterm, link, image, xref, etc.) where
            // commas and punctuation are syntax, not prose boundaries.
            if (c == '[') {
                bracketDepth++;
                i++;
                continue;
            }
            if (c == ']' && bracketDepth > 0) {
                bracketDepth--;
                i++;
                continue;
            }
            if (bracketDepth > 0) {
                i++;
                continue;
            }

            // ── Sentence-ending punctuation: . ? ! ──────────────────────────
            if ((c == '.' || c == '?' || c == '!')
                    && i + 2 < len
                    && text.charAt(i + 1) == ' '
                    && Character.isUpperCase(text.charAt(i + 2))) {

                // For periods, check if this is an abbreviation
                if (c == '.' && isAbbreviation(current.toString())) {
                    i++;
                    continue;
                }

                // Also check: is the "." part of a URL or file path?
                // e.g., "file.adoc The" — period is part of filename
                // Simple heuristic: if char before period is not a letter, don't break
                // (catches "2.0 The" → don't break on version numbers)
                if (c == '.' && current.length() >= 2) {
                    char beforeDot = current.charAt(current.length() - 2);
                    if (Character.isDigit(beforeDot)) {
                        i++;
                        continue;
                    }
                }

                // Break here
                result.add(current.toString().trim());
                current.setLength(0);
                i += 2; // skip the space; next char is the uppercase letter
                continue;
            }

            // ── Closing quote after sentence punctuation: ." or ?" or !" ─────
            if ((c == '"' || c == '\u201d' || c == '\'')
                    && current.length() >= 2) {
                char beforeQuote = current.charAt(current.length() - 2);
                if ((beforeQuote == '.' || beforeQuote == '?' || beforeQuote == '!')
                        && i + 2 < len
                        && text.charAt(i + 1) == ' '
                        && Character.isUpperCase(text.charAt(i + 2))) {

                    result.add(current.toString().trim());
                    current.setLength(0);
                    i += 2;
                    continue;
                }
            }

            // ── Em-dash (Unicode \u2014) ──────────────────────────────────────
            if (!sentencesOnly
                    && c == '\u2014'
                    && i + 1 < len
                    && text.charAt(i + 1) == ' ') {

                result.add(current.toString().trim());
                current.setLength(0);
                i += 2; // skip past the space
                continue;
            }

            // ── Em-dash (AsciiDoc: space-dash-dash-space) ────────────────────
            if (!sentencesOnly
                    && c == '-'
                    && i + 2 < len
                    && text.charAt(i + 1) == '-'
                    && text.charAt(i + 2) == ' '
                    && current.length() >= 2
                    && current.charAt(current.length() - 2) == ' ') {

                current.append('-'); // append second dash
                result.add(current.toString().trim());
                current.setLength(0);
                i += 3; // skip past second dash + space
                continue;
            }

            // ── Semicolon ────────────────────────────────────────────────────
            if (!sentencesOnly
                    && c == ';'
                    && i + 2 < len
                    && text.charAt(i + 1) == ' ') {

                result.add(current.toString().trim());
                current.setLength(0);
                i += 2;
                continue;
            }

            // ── Colon (guarded) ──────────────────────────────────────────────
            if (!sentencesOnly
                    && c == ':'
                    && i + 2 < len
                    && text.charAt(i + 1) == ' '
                    && text.charAt(i + 2) != '/') { // not URL scheme (://)

                // Guard: preceded by digit → likely time (10:30)
                boolean isTime = current.length() >= 2
                        && Character.isDigit(current.charAt(current.length() - 2));

                // Guard: preceded by colon → AsciiDoc definition list (Term::)
                boolean isDefList = current.length() >= 2
                        && current.charAt(current.length() - 2) == ':';

                // Guard: preceded by URL scheme
                String accumulated = current.toString();
                boolean isUrl = accumulated.endsWith("http:")
                        || accumulated.endsWith("https:")
                        || accumulated.endsWith("ftp:")
                        || accumulated.endsWith("mailto:");

                if (!isTime && !isDefList && !isUrl) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    i += 2;
                    continue;
                }
            }

            // ── Comma + conjunction ──────────────────────────────────────────
            if (!sentencesOnly
                    && c == ','
                    && i + 2 < len
                    && text.charAt(i + 1) == ' ') {

                String rest = text.substring(i + 2);
                if (startsWithConjunction(rest)) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    i += 2; // conjunction starts the new line
                    continue;
                }
            }

            // ── Simple comma/semicolon clause breaks (threshold-gated) ───────
            if (clauseBreak
                    && c == ','
                    && i + 2 < len
                    && text.charAt(i + 1) == ' '
                    && current.length() > clauseBreakThreshold) {

                result.add(current.toString().trim());
                current.setLength(0);
                i += 2;
                continue;
            }

            i++;
        }

        // Flush remainder
        String remainder = current.toString().trim();
        if (!remainder.isEmpty()) {
            result.add(remainder);
        }

        return result;
    }

    // ── Soft Wrap ──────────────────────────────────────────────────────────────

    /**
     * Wrap lines that exceed {@code maxLineLength} at the last word boundary
     * before the limit.  This is a post-processing pass applied after semantic
     * breaks, ensuring that long single-clause sentences don't produce
     * excessively wide lines in source.
     * <p>
     * Guards:
     * <ul>
     *   <li>Never breaks inside square brackets (macro arguments).</li>
     *   <li>Skips a break if the remainder would be shorter than
     *       {@code minRemainder} characters (avoids orphan words).</li>
     * </ul>
     */
    private List<String> softWrap(List<String> lines) {
        if (maxLineLength == Integer.MAX_VALUE) {
            return lines;
        }
        List<String> wrapped = new ArrayList<>();
        for (String line : lines) {
            if (line.length() <= maxLineLength) {
                wrapped.add(line);
                continue;
            }
            String remaining = line;
            while (remaining.length() > maxLineLength) {
                int breakAt = findSoftBreak(remaining);
                if (breakAt < 0) {
                    break; // no valid break point, keep as-is
                }
                String remainder = remaining.substring(breakAt + 1);
                // Skip break if remainder is too short (orphan guard)
                if (remainder.length() < minRemainder) {
                    break;
                }
                wrapped.add(remaining.substring(0, breakAt));
                remaining = remainder;
            }
            if (!remaining.isEmpty()) {
                wrapped.add(remaining);
            }
        }
        return wrapped;
    }

    // ── Merge Short Lines ────────────────────────────────────────────────────────

    /**
     * Merge lines shorter than {@code minLineLength} with the following line.
     * This prevents orphan words or very short fragments (like "A" or "The")
     * from sitting alone on a line before a macro or long phrase.
     * <p>
     * Lines that are own-line macros (indexterm, footnote, (((…))) are never
     * merged — they are intentionally on their own line.
     */
    private List<String> mergeShortLines(List<String> lines) {
        if (lines.size() <= 1) {
            return lines;
        }
        List<String> merged = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            // Merge forward if this line is short, not the last line,
            // and neither this line nor the next is an own-line macro
            if (line.length() < minLineLength
                    && i + 1 < lines.size()
                    && !isOwnLineMacro(line)
                    && !isOwnLineMacro(lines.get(i + 1))) {
                merged.add(line + " " + lines.get(i + 1));
                i += 2;
            } else {
                merged.add(line);
                i++;
            }
        }
        return merged;
    }

    /**
     * Check whether a line is an own-line macro that should not be merged.
     */
    private static boolean isOwnLineMacro(String line) {
        for (String macro : OWN_LINE_MACROS) {
            if (line.startsWith(macro.substring(0, macro.length() - 1))) {
                return true;
            }
        }
        return line.startsWith("(((");
    }

    // ── Macros whose bracket content is structured syntax (no wrapping) ────────

    private static final List<String> NO_WRAP_MACROS =
            List.of("indexterm:[", "indexterm2:[");

    /**
     * Find the best soft-wrap break point in a line: the last space at or
     * before {@code maxLineLength} that is not inside a no-wrap bracket
     * region (e.g. {@code indexterm:[…]}).  Spaces inside {@code footnote:[…]}
     * are valid break points because footnote content is prose.
     * Returns -1 if no valid break point exists.
     */
    private int findSoftBreak(String line) {
        int noWrapDepth = 0;   // depth inside no-wrap macro brackets
        int bestBreak = -1;

        int limit = Math.min(line.length(), maxLineLength);
        for (int j = 0; j < limit; j++) {
            char ch = line.charAt(j);
            if (ch == '[' && isNoWrapMacroAt(line, j)) {
                noWrapDepth++;
            } else if (ch == '[') {
                // footnote or other prose brackets — wrappable, ignore
            } else if (ch == ']' && noWrapDepth > 0) {
                noWrapDepth--;
            }
            if (ch == ' ' && noWrapDepth == 0) {
                bestBreak = j;
            }
        }

        if (bestBreak > 0) {
            return bestBreak;
        }

        // No space found before limit — find first space after limit
        // that's outside no-wrap brackets
        for (int j = limit; j < line.length(); j++) {
            char ch = line.charAt(j);
            if (ch == '[' && isNoWrapMacroAt(line, j)) {
                noWrapDepth++;
            } else if (ch == ']' && noWrapDepth > 0) {
                noWrapDepth--;
            }
            if (ch == ' ' && noWrapDepth == 0) return j;
        }

        return -1;
    }

    /**
     * Check whether the {@code '['} at position {@code bracketPos} is the
     * opening bracket of a no-wrap macro (indexterm, indexterm2).
     */
    private static boolean isNoWrapMacroAt(String line, int bracketPos) {
        for (String macro : NO_WRAP_MACROS) {
            int start = bracketPos - (macro.length() - 1); // macro ends with '['
            if (start >= 0 && line.startsWith(macro, start)) {
                return true;
            }
        }
        return false;
    }

    // ── Abbreviation Detection ──────────────────────────────────────────────────

    /**
     * Check if the text ending with a period is an abbreviation.
     * Extracts the last word before the period and checks the known set.
     *
     * @param textEndingWithPeriod the accumulated text including the trailing period
     */
    private static boolean isAbbreviation(String textEndingWithPeriod) {
        // Strip trailing period
        String text = textEndingWithPeriod.substring(0, textEndingWithPeriod.length() - 1).trim();
        if (text.isEmpty()) return false;

        // Find the last word (token after last space)
        int lastSpace = text.lastIndexOf(' ');
        String lastWord = (lastSpace >= 0) ? text.substring(lastSpace + 1) : text;

        // Direct lookup
        if (ABBREVIATIONS.contains(lastWord)) return true;

        // Check for single uppercase letter (common initial: "J. Smith")
        if (lastWord.length() == 1 && Character.isUpperCase(lastWord.charAt(0))) return true;

        return false;
    }

    // ── Conjunction Detection ────────────────────────────────────────────────────

    /**
     * Check if the given text starts with a coordinating conjunction followed by
     * a space (e.g., "and ", "but ", "or ").
     */
    private static boolean startsWithConjunction(String text) {
        for (String conj : CONJUNCTIONS) {
            if (text.startsWith(conj)) return true;
        }
        return false;
    }

    // ── Usage ───────────────────────────────────────────────────────────────────

    private void printUsage() {
        System.err.println("""
                Usage: semantic-linebreak [options] <file.adoc ...>
                       semantic-linebreak [options] <directory ...>

                Reformats AsciiDoc prose paragraphs to use semantic linefeeds.
                Breaks lines at logical boundaries: sentences, em-dashes, semicolons,
                colons, and comma+conjunction joints. Only paragraph blocks are
                modified; listings, diagrams, tables, and all other block types are
                preserved unchanged.

                Accepts one or more files or directories. Directories are walked
                recursively for *.adoc files, skipping target/ directories.
                AsciidoctorJ is initialized once and reused across all files.

                Options:
                  -o, --output <file>     Write to file (single-file mode only)
                  -n, --dry-run           Print result to stdout, don't modify files
                  -v, --verbose           Show which paragraphs are being reformatted
                  --sentences-only        Break only at sentence boundaries (. ? !)
                  --clause-break          Also break on simple commas (default: on)
                  --clause-threshold <n>  Min line length before comma break (default: 0)
                  --max-line-length <n>   Soft-wrap long lines at word boundary (default: 64)
                  --min-remainder <n>     Skip wrap if remainder < n chars (default: 15)
                  --min-line-length <n>   Merge short lines with next line (default: 10)
                  --no-wrap               Disable soft wrapping
                  -h, --help              Show this message

                Examples:
                  semantic-linebreak doc.adoc                        # single file, in-place
                  semantic-linebreak -n doc.adoc                     # preview to stdout
                  semantic-linebreak ch1.adoc ch2.adoc ch3.adoc      # multiple files
                  semantic-linebreak src/docs/asciidoc/               # entire directory tree
                  semantic-linebreak --sentences-only doc.adoc        # sentences only
                  semantic-linebreak -o reformatted.adoc doc.adoc    # write to new file
                """);
    }
}
