package network.ike.docs.ingest;

import java.util.List;

/// A sectioned `_includes.adoc` manifest — a single AsciiDoc fragment
/// that an assembly module includes with one directive to pull in
/// every ingested topic for a source, organized into named sections.
///
/// @param header   prose to emit at the top of the file (comments,
///                 typically auto-generation provenance)
/// @param sections the named sections; each rendered as a `==` H2
///                 followed by the section's include directives
public record IncludesFile(String header, List<IncludesSection> sections) {

    /// One section of an includes file.
    ///
    /// @param title       the section heading (rendered as `== title`)
    /// @param includePaths the include paths relative to the location
    ///                    of the `_includes.adoc` file; each is rendered as
    ///                    `include::{path}[leveloffset=+1]`
    public record IncludesSection(String title, List<String> includePaths) {
    }
}
