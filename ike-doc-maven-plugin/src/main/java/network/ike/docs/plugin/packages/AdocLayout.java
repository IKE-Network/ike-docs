package network.ike.docs.plugin.packages;

import java.util.Locale;

/**
 * How a library lays out its AsciiDoc root, chosen by the POM property
 * {@code ike.adoc.layout} ({@code dev-adoc-packages},
 * IKE-Network/ike-issues#1341). The property gates every rule of the
 * packages layout, so a library that has not declared it keeps building
 * exactly as before through the migration.
 *
 * @since 136
 */
public enum AdocLayout {

    /**
     * The layout before packages: a {@code topics/<domain>/} tree under the
     * root with the preview index and shared images beside it, the registry
     * written to {@code target/topic-registry.yaml}. The default.
     */
    LEGACY,

    /**
     * The packages layout: the root holds one directory per owning group,
     * written as one dotted segment, with the topic domain as the directory
     * under it; artifact metadata goes under {@code META-INF/ike/<group>/<artifact>/}.
     */
    PACKAGES;

    /** The property that selects the layout. */
    public static final String PROPERTY = "ike.adoc.layout";

    /**
     * Read the layout from the property's value.
     *
     * @param value the value of {@code ike.adoc.layout}, or {@code null} when
     *              the property is absent
     * @return the layout; {@link #LEGACY} when the value is absent or blank
     * @throws IllegalArgumentException if the value names no layout
     */
    public static AdocLayout of(String value) {
        if (value == null || value.isBlank()) {
            return LEGACY;
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "packages" -> PACKAGES;
            case "legacy" -> LEGACY;
            default -> throw new IllegalArgumentException("Unknown " + PROPERTY + " '" + value
                    + "' (expected 'packages' or 'legacy')");
        };
    }
}
