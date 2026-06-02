package network.ike.docs.koncept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the Tinkar PublicId {@code idString} for a koncept — the exact
 * string Komet feeds its identicon generator
 * ({@code dev.ikm.komet.framework.Identicon} calls
 * {@code LifeHash.makeFromUTF8(publicId.idString(), VERSION2, …)}).
 * <p>
 * Producing the byte-identical string is what makes this extension's
 * identicon match Komet's. The UUID source is resolved as:
 * <ol>
 *   <li>explicit {@code uuids} on the {@link KonceptDefinition} (in
 *       declaration order), else</li>
 *   <li>the SNOMED-derived UUID from the definition's {@code sctid}
 *       ({@link SnomedUuids#fromSnomed}), else</li>
 *   <li>none — no identicon can be computed.</li>
 * </ol>
 *
 * @see SnomedUuids
 */
final class KonceptIdentity {

    private KonceptIdentity() {
    }

    /**
     * Resolves the identicon {@code idString} for a koncept definition.
     *
     * @param def the koncept definition (may be {@code null})
     * @return the Tinkar {@code idString} (e.g.
     *         {@code ["f05fae71-345a-5f4b-9a3c-4588409fa692"]}), or empty
     *         when no UUIDs can be resolved
     */
    static Optional<String> idString(KonceptDefinition def) {
        if (def == null) {
            return Optional.empty();
        }
        List<UUID> uuids = resolveUuids(def);
        return uuids.isEmpty() ? Optional.empty() : Optional.of(idString(uuids));
    }

    /**
     * Resolves the ordered UUID list for a koncept: explicit {@code uuids}
     * win (normalized to lowercase canonical form), otherwise the
     * SNOMED-derived UUID from {@code sctid}.
     *
     * @param def the koncept definition
     * @return the resolved UUIDs in order, or an empty list when none apply
     * @throws IllegalArgumentException if an explicit {@code uuids} entry is
     *                                  not a valid UUID
     */
    static List<UUID> resolveUuids(KonceptDefinition def) {
        if (def.uuids() != null && !def.uuids().isEmpty()) {
            List<UUID> out = new ArrayList<>(def.uuids().size());
            for (String u : def.uuids()) {
                out.add(UUID.fromString(u.strip()));
            }
            return out;
        }
        if (def.sctid() != null && !def.sctid().isBlank()) {
            return List.of(SnomedUuids.fromSnomed(def.sctid().strip()));
        }
        return List.of();
    }

    /**
     * Formats a UUID list exactly as
     * {@code dev.ikm.tinkar.common.id.PublicId.idString(UUID[])} does for
     * the realistic case of at most 32 UUIDs: a bracketed, comma-space
     * separated list of double-quoted lowercase UUIDs, e.g.
     * {@code ["u1", "u2"]}. (The upstream {@code ..., } truncation past 32
     * UUIDs is omitted; koncepts never carry that many.)
     *
     * @param uuids the UUIDs in datastore order
     * @return the Tinkar {@code idString} representation
     */
    static String idString(List<UUID> uuids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < uuids.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(uuids.get(i)).append('"');
        }
        return sb.append(']').toString();
    }
}
