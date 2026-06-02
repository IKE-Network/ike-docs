package network.ike.docs.koncept;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Derives the Tinkar PublicId UUID for a SNOMED CT concept from its SCTID.
 * <p>
 * This reproduces {@code dev.ikm.tinkar.common.util.uuid.UuidUtil.fromSNOMED}
 * exactly: a Type-5 (name-based, SHA-1) UUID computed over the SNOMED CT
 * namespace and the SCTID string. It is replicated here — rather than
 * depending on {@code dev.ikm.tinkar:common} — to avoid the
 * {@code ike-docs} &rarr; {@code tinkar-core} reverse-dependency cycle
 * (IKE-Network/ike-issues#215, #216): tinkar-core builds its own
 * documentation through this extension. The replication is pinned to the
 * upstream output by {@code SnomedUuidsTest}.
 * <p>
 * The derived UUID matches the identicon Komet renders for a concept only
 * when that concept's datastore PublicId is exactly this single SNOMED
 * UUID. Concepts carrying additional or non-SNOMED primordial UUIDs must
 * supply explicit {@code uuids} in their koncept definition instead.
 *
 * @see KonceptIdentity
 */
public final class SnomedUuids {

    private SnomedUuids() {
    }

    /**
     * The SNOMED CT Type-5 namespace, identical to
     * {@code dev.ikm.tinkar.common.util.uuid.UuidUtil.SNOMED_NAMESPACE}.
     */
    public static final UUID SNOMED_NAMESPACE =
            UUID.fromString("3094dbd1-60cf-44a6-92e3-0bb32ca4d3de");

    /**
     * Derives the Tinkar UUID for a SNOMED CT concept from its SCTID.
     *
     * @param sctid the SNOMED CT concept identifier (e.g. {@code "84114007"})
     * @return the deterministic Type-5 UUID for that concept
     * @throws NullPointerException if {@code sctid} is {@code null}
     */
    public static UUID fromSnomed(String sctid) {
        return type5(SNOMED_NAMESPACE, sctid);
    }

    /**
     * Computes an RFC 4122 Type-5 (name-based, SHA-1) UUID, byte-for-byte
     * compatible with {@code UuidT5Generator.getUuidWithEncoding(namespace,
     * name, "UTF-8")}: SHA-1 over the namespace's 16 raw bytes followed by
     * the UTF-8 bytes of the name, with the version and IETF variant bits set.
     *
     * @param namespace the namespace UUID
     * @param name      the name to hash within the namespace
     * @return the generated Type-5 UUID
     * @throws NullPointerException  if {@code namespace} or {@code name} is {@code null}
     * @throws IllegalStateException if the SHA-1 algorithm is unavailable
     */
    public static UUID type5(UUID namespace, String name) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(rawBytes(namespace));
            sha1.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] d = sha1.digest();
            d[6] = (byte) ((d[6] & 0x0f) | 0x50); // clear version, set to version 5
            d[8] = (byte) ((d[8] & 0x3f) | 0x80); // clear variant, set to IETF variant
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (d[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (d[i] & 0xff);
            }
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 MessageDigest unavailable", e);
        }
    }

    /**
     * Serializes a UUID to its 16 big-endian bytes (most-significant bits
     * first, then least-significant bits), matching
     * {@code UuidUtil.getRawBytes}.
     *
     * @param uuid the UUID to serialize
     * @return a 16-byte big-endian representation
     */
    private static byte[] rawBytes(UUID uuid) {
        byte[] b = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 0; i < 8; i++) {
            b[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return b;
    }
}
