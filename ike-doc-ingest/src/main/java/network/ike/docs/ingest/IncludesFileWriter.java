package network.ike.docs.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Serialize {@link IncludesFile} value objects to AsciiDoc files —
/// the `_includes.adoc` assembly-composition manifests that let an
/// assembly module include every ingested topic for a source with a
/// single directive.
public final class IncludesFileWriter {

    /// Write the includes file to {@code target}, creating parent
    /// directories as needed.
    public void write(Path target, IncludesFile manifest) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, render(manifest));
    }

    /// Render the includes file as a string without writing it.
    public String render(IncludesFile manifest) {
        StringBuilder sb = new StringBuilder(4096);

        if (manifest.header() != null && !manifest.header().isEmpty()) {
            sb.append(manifest.header());
            if (!manifest.header().endsWith("\n")) {
                sb.append("\n");
            }
            sb.append("\n");
        }

        for (IncludesFile.IncludesSection s : manifest.sections()) {
            if (s.includePaths() == null || s.includePaths().isEmpty()) {
                continue;
            }
            sb.append("== ").append(s.title()).append("\n\n");
            for (String path : s.includePaths()) {
                sb.append("include::").append(path).append("[leveloffset=+1]\n\n");
            }
        }

        return sb.toString();
    }
}
