/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.typst;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text-level Typst serialization: escaping for markup mode, string literals,
 * labels, and the protected-span convention that lets already-emitted Typst
 * travel through a text pass untouched.
 *
 * <p>Asciidoctor hands a converter substituted text: plain text with HTML
 * special characters entity-escaped ({@code &lt;}, {@code &#8212;}), and the
 * output of earlier inline conversions spliced in. Those earlier outputs are
 * finished Typst and must not be escaped again, so every inline conversion
 * wraps its result in {@link #protect(String)}; {@link #markup(String)} then
 * decodes and escapes only the text between protected spans.
 *
 * <p>This class is the seed of the shared Typst writer library (design topic
 * {@code dev-document-surface-pages}, "One shared Typst writer library"): the
 * same escaping rules serve the AsciiDoc converter and Komet's block-model
 * writer.
 */
public final class TypstText {

    /** Opens a protected span (finished Typst inside substituted text). */
    public static final char OPEN = '\u0001';
    /** Closes a protected span. */
    public static final char CLOSE = '\u0002';

    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9A-Fa-f]+|#[0-9]+|[A-Za-z]+);");

    private TypstText() {}

    /**
     * Wraps finished Typst so that the substitutions Asciidoctor still runs on
     * the surrounding text (macros, replacements, post-replacements) never see
     * it, and a later {@link #markup(String)} or {@link #plain(String)} pass
     * restores it verbatim. The span is Base64 between two control characters,
     * inside angle brackets: Base64 holds none of the characters those
     * substitutions key on, so a {@code ]} in a content block cannot end a
     * footnote macro early and an apostrophe in a string literal cannot become
     * a curly quote; and the angle brackets make the span look like an HTML
     * tag, which Asciidoctor strips when it derives a section id from the
     * converted title, so {@code == Using *bold*} still gets the id
     * {@code _using_bold}. (A real {@code <} in text is {@code &lt;} by then.)
     *
     * @param typst finished Typst source
     * @return the opaque span
     */
    public static String protect(String typst) {
        return "<" + OPEN + Base64.getEncoder().encodeToString(typst.getBytes(StandardCharsets.UTF_8)) + CLOSE + ">";
    }

    /**
     * Wraps substituted text in an opening and a closing piece of Typst, each
     * protected on its own, leaving the text in place for the enclosing text
     * pass to decode and escape. Used for every inline construct whose content
     * is markup ({@code #strong[}…{@code ]}, {@code #footnote[}…{@code ]}), so
     * that Asciidoctor still sees the words when it derives ids from a title.
     *
     * @param open  the Typst before the text, ending with {@code [}
     * @param text  substituted text, left as is
     * @param close the Typst after the text
     * @return the wrapped text
     */
    public static String wrap(String open, String text, String close) {
        return protect(open) + (text == null ? "" : text) + protect(close);
    }

    /**
     * Turns Asciidoctor-substituted text into Typst markup: decodes HTML entities
     * and escapes markup-significant characters outside protected spans, and
     * restores protected spans verbatim. After a protected span a {@code ;}
     * terminates the embedded expression when the following character would
     * otherwise continue it.
     *
     * @param substituted text as Asciidoctor delivers it, possibly null
     * @return Typst markup
     */
    public static String markup(String substituted) {
        return walk(substituted, true);
    }

    /**
     * Turns substituted text into plain text: entities decoded, protected spans
     * restored, nothing escaped. For raw blocks and string literals.
     *
     * @param substituted text as Asciidoctor delivers it, possibly null
     * @return plain text
     */
    public static String plain(String substituted) {
        return walk(substituted, false);
    }

    private static String walk(String substituted, boolean escape) {
        if (substituted == null || substituted.isEmpty()) return "";
        StringBuilder out = new StringBuilder(substituted.length() + 16);
        int n = substituted.length();
        int textStart = 0;
        int i = 0;
        while (i < n) {
            if (substituted.charAt(i) != OPEN) {
                i++;
                continue;
            }
            int open = i > 0 && substituted.charAt(i - 1) == '<' ? i - 1 : i;
            out.append(text(substituted.substring(textStart, open), escape));
            int close = substituted.indexOf(CLOSE, i + 1);
            if (close < 0) close = n;
            String encoded = substituted.substring(i + 1, close);
            String code = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            out.append(code);
            int next = Math.min(close + 1, n);
            if (next < n && substituted.charAt(next) == '>') next++;
            boolean endsExpression = !code.endsWith("[");
            if (escape && endsExpression && next < n && continuesExpression(substituted.charAt(next))) {
                out.append(';');
            }
            i = next;
            textStart = next;
        }
        out.append(text(substituted.substring(textStart), escape));
        return out.toString();
    }

    private static String text(String raw, boolean escape) {
        String decoded = decodeEntities(raw);
        return escape ? escapeMarkup(decoded) : decoded;
    }

    private static boolean continuesExpression(char next) {
        return next == '[' || next == '(' || next == '.';
    }

    /**
     * A Typst string literal.
     *
     * @param plain the plain text value
     * @return the quoted, escaped literal
     */
    public static String string(String plain) {
        StringBuilder sb = new StringBuilder(plain.length() + 2);
        sb.append('"');
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * A Typst label reference, {@code <name>}, from an AsciiDoc id. Characters
     * outside Typst's label alphabet become hyphens.
     *
     * @param id the AsciiDoc id
     * @return the label, angle brackets included
     */
    public static String label(String id) {
        return "<" + labelName(id) + ">";
    }

    /**
     * The bare label name for an AsciiDoc id.
     *
     * @param id the AsciiDoc id
     * @return a name in Typst's label alphabet ({@code A-Za-z0-9_-.:})
     */
    public static String labelName(String id) {
        StringBuilder sb = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' || c == ':';
            sb.append(ok ? c : '-');
        }
        return sb.toString();
    }

    /**
     * A raw block fence that cannot be closed by the content: one more backtick
     * than the longest run inside, never fewer than three.
     *
     * @param content the raw content
     * @return the fence string
     */
    public static String fence(String content) {
        int longest = 0;
        int run = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '`') {
                run++;
                longest = Math.max(longest, run);
            } else {
                run = 0;
            }
        }
        return "`".repeat(Math.max(3, longest + 1));
    }

    /**
     * Decodes the HTML entities Asciidoctor's substitutions produce.
     *
     * @param text entity-escaped text
     * @return decoded text
     */
    public static String decodeEntities(String text) {
        if (text.indexOf('&') < 0) return text;
        Matcher m = ENTITY.matcher(text);
        StringBuilder sb = new StringBuilder(text.length());
        while (m.find()) {
            String name = m.group(1);
            String rep;
            if (name.startsWith("#x") || name.startsWith("#X")) {
                rep = Character.toString(Integer.parseInt(name.substring(2), 16));
            } else if (name.startsWith("#")) {
                rep = Character.toString(Integer.parseInt(name.substring(1)));
            } else {
                rep = switch (name) {
                    case "lt" -> "<";
                    case "gt" -> ">";
                    case "amp" -> "&";
                    case "quot" -> "\"";
                    case "apos" -> "'";
                    case "nbsp" -> " ";
                    case "hellip" -> "…";
                    case "mdash" -> "—";
                    case "ndash" -> "–";
                    default -> m.group(0);
                };
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Escapes plain text for Typst markup mode: the characters that open
     * expressions, styling, math, labels, references and comments, the
     * shorthands ({@code --}, {@code -?}, {@code ...}), a {@code ;} (which
     * would silently end a preceding code expression), and the line-leading
     * characters that start lists, headings and terms.
     *
     * @param plain decoded plain text
     * @return escaped markup text
     */
    public static String escapeMarkup(String plain) {
        StringBuilder sb = new StringBuilder(plain.length() + 8);
        boolean lineStart = true;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (lineStart && c != ' ' && c != '\t') {
                if (startsBlockMarker(plain, i)) {
                    sb.append('\\');
                }
                lineStart = false;
            }
            char next = i + 1 < plain.length() ? plain.charAt(i + 1) : 0;
            char prev = i > 0 ? plain.charAt(i - 1) : 0;
            switch (c) {
                case '\\', '#', '*', '_', '`', '$', '<', '>', '@', '[', ']', '~', ';' -> sb.append('\\').append(c);
                case '-' -> {
                    if (next == '-' || next == '?' || prev == '-') sb.append("\\-");
                    else sb.append(c);
                }
                case '.' -> {
                    if (next == '.' && i + 2 < plain.length() && plain.charAt(i + 2) == '.') sb.append("\\.");
                    else sb.append(c);
                }
                case '/' -> {
                    if (next == '/' || next == '*') {
                        sb.append("\\/");
                    } else {
                        sb.append(c);
                    }
                }
                case '\n' -> {
                    sb.append(c);
                    lineStart = true;
                }
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Whether the text at {@code i} (a line's first non-blank) would start a list item, heading or term. */
    private static boolean startsBlockMarker(String s, int i) {
        char c = s.charAt(i);
        boolean spaceAfter = i + 1 < s.length() && (s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t');
        if ((c == '-' || c == '+' || c == '=' || c == '/') && (spaceAfter || i + 1 == s.length())) return true;
        if (c == '=' ) return true;
        if (Character.isDigit(c)) {
            int j = i;
            while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
            return j < s.length() && s.charAt(j) == '.' && (j + 1 == s.length() || s.charAt(j + 1) == ' ');
        }
        return false;
    }
}
