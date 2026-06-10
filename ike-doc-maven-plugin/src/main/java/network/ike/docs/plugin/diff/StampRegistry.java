package network.ike.docs.plugin.diff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The packet's stamp set (ike-issues#656): one STAMP per (range commit,
 * status) pair, plus the uncommitted pair for working-tree marks. Emits
 * reusable AsciiDoc footnotes — every emission carries the full text,
 * so document order never matters; Asciidoctor reuses the first
 * definition per id — and accumulates the used stamps for the packet's
 * Stamp Register table.
 *
 * <p>Status is stated redundantly in the endnote for STAMP consistency:
 * <em>Active</em> for insertions and replacements (a replacement is a
 * new Active version; its predecessor is covered by the successor's
 * stamp), <em>Inactive</em> for unpaired deletions. The remaining
 * coordinates map to the git era as: Time = author date, Author =
 * commit author, Module = the topic's domain, Path = the branch.
 */
public final class StampRegistry {

    private static final Pattern TRAILER = Pattern.compile(
            "^(?:Refs|Fixes):\\s*(\\S+#\\d+)\\s*$", Pattern.MULTILINE);

    /**
     * Marked-region status, stated redundantly in each endnote. The
     * vocabulary is the STAMP status vocabulary — closed. Insertions
     * and replacements are Active (a replacement is a new Active
     * version; the struck predecessor is covered by its successor's
     * stamp); unpaired deletions are Inactive.
     */
    public enum Status {
        /** A new or replacing version of the text. */
        ACTIVE("Active"),
        /** An unpaired deletion — an inactivation. */
        INACTIVE("Inactive");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        /**
         * The label as printed in endnotes and the register.
         *
         * @return the human label
         */
        public String label() {
            return label;
        }
    }

    /**
     * One stamp as used somewhere in the packet.
     *
     * @param footnoteId the reusable footnote id
     * @param status     the redundant S coordinate
     * @param time       the T coordinate (author date or render note)
     * @param author     the A coordinate
     * @param module     the M coordinate (topic domain)
     * @param branch     the P coordinate (branch)
     * @param commitId   the abbreviated commit id, or {@code "worktree"}
     * @param refs       issue refs from the commit trailer, or empty
     */
    public record Stamp(String footnoteId, Status status, String time, String author,
                        String module, String branch, String commitId, String refs) {
    }

    private final Map<String, GitSource.CommitMeta> commitsById = new LinkedHashMap<>();
    private final Map<String, Stamp> used = new LinkedHashMap<>();
    private final String branch;
    private final String worktreeAuthor;
    private final String renderDate;
    private final String rangeLabel;

    /**
     * Create the registry for one packet.
     *
     * @param rangeCommits   the compared range's commits
     * @param branch         the branch (STAMP path coordinate)
     * @param worktreeAuthor author for uncommitted stamps
     * @param renderDate     date used for uncommitted stamps' time
     * @param rangeLabel     label for coarse attribution of unpaired
     *                       deletions in committed ranges, e.g.
     *                       {@code "9a958f7..HEAD"}
     */
    public StampRegistry(List<GitSource.CommitMeta> rangeCommits, String branch,
                         String worktreeAuthor, String renderDate, String rangeLabel) {
        rangeCommits.forEach(c -> commitsById.put(c.id(), c));
        this.branch = branch;
        this.worktreeAuthor = worktreeAuthor;
        this.renderDate = renderDate;
        this.rangeLabel = rangeLabel;
    }

    /**
     * Whether the packet can carry more than one distinct stamp; when
     * not, in-flow refs are suppressed and the cover states the single
     * stamp once.
     *
     * @param hasUncommitted whether any compared content is uncommitted
     * @return whether multiple stamps are possible
     */
    public boolean multipleStampsPossible(boolean hasUncommitted) {
        return commitsById.size() + (hasUncommitted ? 1 : 0) > 1;
    }

    /**
     * Render the single stamp's coordinate line for the cover when
     * in-flow refs are suppressed.
     *
     * @param module the module label to print
     * @return the one-line stamp statement
     */
    public String singleStampLine(String module) {
        Stamp s = commitsById.isEmpty()
            ? stamp(GitSource.UNCOMMITTED, Status.ACTIVE, module)
            : stamp(commitsById.keySet().iterator().next(), Status.ACTIVE, module);
        return text(s);
    }

    /**
     * The reusable footnote macro for one attribution + status. Every
     * call emits the full text; Asciidoctor keys reuse on the id.
     *
     * @param attribution an abbreviated commit id or
     *                    {@link GitSource#UNCOMMITTED}
     * @param status      the region's status
     * @param module      the topic's domain
     * @return the {@code footnote:id[text]} macro
     */
    public String footnote(String attribution, Status status, String module) {
        Stamp s = stamp(attribution, status, module);
        used.putIfAbsent(s.footnoteId(), s);
        return "footnote:" + s.footnoteId() + "[" + text(s) + "]";
    }

    /**
     * The stamps actually used in this packet, in first-use order.
     *
     * @return the used stamps
     */
    public List<Stamp> usedStamps() {
        return List.copyOf(used.values());
    }

    private Stamp stamp(String attribution, Status status, String module) {
        if (attribution == null) {
            // Unpaired deletion in a committed range: attribution is
            // deliberately coarse in v1 (pickaxe search deferred, #656).
            return new Stamp("stamp-range-" + suffix(status), status,
                    "within " + rangeLabel, "(range)", module, branch, "range", "");
        }
        if (GitSource.UNCOMMITTED.equals(attribution)) {
            return new Stamp("stamp-wt-" + suffix(status), status,
                    "uncommitted, as of " + renderDate, worktreeAuthor,
                    module, branch, "worktree", "");
        }
        GitSource.CommitMeta c = commitsById.get(attribution);
        String refs = "";
        if (c != null) {
            Matcher m = TRAILER.matcher(c.fullMessage());
            refs = m.find() ? m.group(1) : "";
        }
        return new Stamp("stamp-" + attribution + "-" + suffix(status), status,
                c == null ? "in range" : c.date(),
                c == null ? "(range)" : c.author(),
                module, branch, attribution, refs);
    }

    private static String suffix(Status status) {
        return status == Status.ACTIVE ? "a" : "i";
    }

    private static String text(Stamp s) {
        StringBuilder sb = new StringBuilder("STAMP ")
                .append(s.status().label())
                .append(" · ").append(s.time())
                .append(" · ").append(s.author())
                .append(" · ").append(s.module())
                .append(" · ").append(s.branch())
                .append(" · ").append(s.commitId());
        if (!s.refs().isEmpty()) {
            sb.append(" · ").append(s.refs());
        }
        return sb.toString();
    }
}
