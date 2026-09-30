package network.ike.docs.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
/// :topic-{key}: {value}        (one per TopicFragment.attributes entry, in order;
///   long values continue on lines ending in " \\")
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

        for (Map.Entry<String, String> a : fragment.attributes().entrySet()) {
            String value = a.getValue() == null ? "" : a.getValue().replaceAll("\\s+", " ").strip();
            if (!value.isEmpty()) {
                sb.append(attributeLine("topic-" + a.getKey(), value));
            }
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

    /// Render one header attribute, wrapping a long value at word
    /// boundaries onto continuation lines that end in ` \` and are
    /// indented two spaces, so each line stays near {@link #WRAP}
    /// characters.
    ///
    /// @param name  the attribute name, e.g. `topic-summary`
    /// @param value the single-line value
    /// @return the attribute line(s), newline-terminated
    static String attributeLine(String name, String value) {
        StringBuilder out = new StringBuilder();
        StringBuilder line = new StringBuilder(":").append(name).append(":");
        for (String word : value.split(" ")) {
            if (line.length() + 1 + word.length() > WRAP && !line.toString().isBlank()
                    && !line.toString().endsWith(":")) {
                out.append(line).append(" \\\n");
                line = new StringBuilder(" ");
            }
            line.append(' ').append(word);
        }
        return out.append(line).append('\n').toString();
    }

    /// Target width of a wrapped header attribute line.
    static final int WRAP = 92;

    private static String stripNewlines(String s) {
        return s == null ? "" : s.replace("\n", " ");
    }
}
