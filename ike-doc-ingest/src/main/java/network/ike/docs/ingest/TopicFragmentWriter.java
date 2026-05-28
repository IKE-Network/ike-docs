package network.ike.docs.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Serialize {@link TopicFragment} value objects to AsciiDoc files
/// with the IKE-ASCIIDOC-FRAGMENT standard header.
///
/// The emitted file has the shape:
///
/// ```adoc
/// // {topicId}
/// // Topic: {title}
/// // Type: {type}
/// // Status: {status}
/// :topic-id: {topicId}
/// :topic-type: {type}
/// :topic-status: {status}
/// :topic-provenance: {provenance.provenance}
/// :topic-citation: {provenance.citation}
/// :topic-license: {provenance.license}
/// :topic-keywords: kw1, kw2, ...
///
/// [[{topicId}]]
/// = {title}
///
/// {body}
/// ```
///
/// The provenance triplet block is omitted entirely when the
/// fragment's {@code provenance} field is null (for internally
/// authored fragments). The keywords line is omitted when the
/// keywords list is empty.
public final class TopicFragmentWriter {

    /// Write the fragment to {@code target}, creating parent
    /// directories as needed.
    ///
    /// @param target path where the .adoc file should be written
    /// @param fragment the fragment to serialize
    /// @throws IOException on I/O failure
    public void write(Path target, TopicFragment fragment) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, render(fragment));
    }

    /// Render the fragment to a string without writing it.
    ///
    /// Useful for tests and for callers that want to post-process
    /// the rendered form before writing.
    ///
    /// @param fragment the fragment to render
    /// @return the AsciiDoc source as a string
    public String render(TopicFragment fragment) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("// ").append(fragment.topicId()).append("\n");
        sb.append("// Topic: ").append(stripNewlines(fragment.title())).append("\n");
        sb.append("// Type: ").append(fragment.type()).append("\n");
        sb.append("// Status: ").append(fragment.status()).append("\n");
        sb.append(":topic-id: ").append(fragment.topicId()).append("\n");
        sb.append(":topic-type: ").append(fragment.type()).append("\n");
        sb.append(":topic-status: ").append(fragment.status()).append("\n");

        if (fragment.provenance() != null) {
            ProvenanceAttributes p = fragment.provenance();
            sb.append(":topic-provenance: ").append(p.provenance()).append("\n");
            sb.append(":topic-citation: ").append(p.citation()).append("\n");
            sb.append(":topic-license: ").append(p.license()).append("\n");
        }

        if (fragment.keywords() != null && !fragment.keywords().isEmpty()) {
            sb.append(":topic-keywords: ").append(String.join(", ", fragment.keywords())).append("\n");
        }

        sb.append("\n");
        sb.append("[[").append(fragment.topicId()).append("]]\n");
        sb.append("= ").append(stripNewlines(fragment.title())).append("\n\n");

        if (fragment.body() != null && !fragment.body().isEmpty()) {
            sb.append(fragment.body());
            if (!fragment.body().endsWith("\n")) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String stripNewlines(String s) {
        return s == null ? "" : s.replace("\n", " ");
    }
}
