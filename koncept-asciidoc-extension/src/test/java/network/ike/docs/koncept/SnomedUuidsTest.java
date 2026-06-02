package network.ike.docs.koncept;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the local Type-5 derivation to the upstream tinkar-core output. The
 * golden UUIDs were produced by
 * {@code dev.ikm.tinkar.common.util.uuid.UuidUtil.fromSNOMED} (verified via
 * the parity spike, run against {@code dev.ikm.tinkar:common:1.127.1}).
 */
class SnomedUuidsTest {

    @Test
    void fromSnomed_matchesTinkarUuidUtil() {
        assertEquals(UUID.fromString("f05fae71-345a-5f4b-9a3c-4588409fa692"),
                SnomedUuids.fromSnomed("84114007"), "HeartFailure");
        assertEquals(UUID.fromString("6f383d3c-d502-5c46-a134-317dc546983e"),
                SnomedUuids.fromSnomed("73211009"), "DiabetesMellitus");
        assertEquals(UUID.fromString("227bfb98-7a30-5709-be2c-a725c54549f3"),
                SnomedUuids.fromSnomed("60573004"), "AorticStenosis");
    }

    @Test
    void namespace_isSnomedNamespace() {
        assertEquals(UUID.fromString("3094dbd1-60cf-44a6-92e3-0bb32ca4d3de"),
                SnomedUuids.SNOMED_NAMESPACE);
    }
}
