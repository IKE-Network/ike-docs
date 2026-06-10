package network.ike.docs.plugin.diff;

/**
 * Per-file status of a compared AsciiDoc source between the two sides
 * of a doc-diff comparison (ike-issues#649).
 */
public enum ChangeStatus {
    /** Present on both sides with differing content. */
    MODIFIED,
    /** Absent on the from side, present on the to side. */
    ADDED,
    /** Present on the from side, absent on the to side. */
    DELETED,
    /** Moved between paths (content compared across the rename). */
    RENAMED
}
