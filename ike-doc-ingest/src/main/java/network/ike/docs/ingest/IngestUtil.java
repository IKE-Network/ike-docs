package network.ike.docs.ingest;

/// Pure helper utilities for ingest pipelines.
///
/// All methods are stateless and side-effect free.
public final class IngestUtil {

    private IngestUtil() {
    }

    /// Convert an arbitrary string into a safe URL/path slug.
    ///
    /// - Lowercases the input.
    /// - Collapses runs of non-`[a-z0-9-]` to single `-`.
    /// - Strips leading and trailing `-`.
    ///
    /// @param s the input string (must not be null)
    /// @return the slugified form
    public static String safeSlug(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /// Escape a string for safe inclusion as a YAML scalar value.
    ///
    /// Backslashes and double-quotes are escaped, newlines are flattened
    /// to spaces, and surrounding whitespace is trimmed.
    ///
    /// @param s the input string; null is treated as empty
    /// @return the YAML-safe form
    public static String yamlEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .trim();
    }

    /// Truncate to at most `max` characters, breaking at a word boundary
    /// if possible. Appends `…` (U+2026) if truncated.
    ///
    /// @param s the input string (must not be null)
    /// @param max the maximum length
    /// @return the original string if short enough, else a truncated form
    public static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        int cut = s.lastIndexOf(' ', max);
        return s.substring(0, Math.max(cut, max - 20)) + "…";
    }

    /// Collapse runs of whitespace to single spaces and trim.
    ///
    /// Useful for one-lining descriptions ingested from upstream that
    /// embed newlines and indentation in their source format.
    ///
    /// @param s the input string; null is treated as empty
    /// @return the cleaned single-line form
    public static String stripMarkup(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }
}
