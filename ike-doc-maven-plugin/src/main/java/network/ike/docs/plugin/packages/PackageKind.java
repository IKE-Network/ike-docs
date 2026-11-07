package network.ike.docs.plugin.packages;

import java.util.Locale;

/**
 * What a package holds, as the manifest records it under {@code kind}
 * ({@code dev-adoc-packages}, IKE-Network/ike-issues#1341).
 *
 * @since 136
 */
public enum PackageKind {

    /** Topics: files that declare a {@code :topic-id:}, with their images. */
    TOPICS,

    /**
     * The library's own package: the preview index and shared images,
     * rendered content that is not metadata and so not under {@code META-INF}.
     * A package is of this kind when {@code ike.adoc.library-package} names
     * it, or when it holds an {@code index.adoc} and no topic.
     */
    LIBRARY;

    /**
     * The kind as the manifest writes it.
     *
     * @return the lowercase name
     */
    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
