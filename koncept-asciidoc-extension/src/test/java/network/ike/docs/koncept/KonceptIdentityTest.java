package network.ike.docs.koncept;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies idString resolution matches {@code PublicId.idString()} and the
 * derive/override precedence.
 */
class KonceptIdentityTest {

    @Test
    void explicitUuids_winAndNormalizeToLowercase() {
        KonceptDefinition def = KonceptDefinition.builder()
                .identifier("HeartFailure")
                .uuids(List.of("F05FAE71-345A-5F4B-9A3C-4588409FA692"))
                .sctid("99999999") // must be ignored in favour of explicit uuids
                .build();
        assertEquals(Optional.of("[\"f05fae71-345a-5f4b-9a3c-4588409fa692\"]"),
                KonceptIdentity.idString(def));
    }

    @Test
    void multipleUuids_joinTinkarStyle() {
        KonceptDefinition def = KonceptDefinition.builder()
                .identifier("Foo")
                .uuids(List.of("11111111-1111-1111-1111-111111111111",
                        "22222222-2222-2222-2222-222222222222"))
                .build();
        assertEquals(Optional.of(
                        "[\"11111111-1111-1111-1111-111111111111\", "
                                + "\"22222222-2222-2222-2222-222222222222\"]"),
                KonceptIdentity.idString(def));
    }

    @Test
    void sctidOnly_derivesIdString() {
        KonceptDefinition def = KonceptDefinition.builder()
                .identifier("HeartFailure")
                .sctid("84114007")
                .build();
        assertEquals(Optional.of("[\"f05fae71-345a-5f4b-9a3c-4588409fa692\"]"),
                KonceptIdentity.idString(def));
    }

    @Test
    void noUuidsNoSctid_isEmpty() {
        KonceptDefinition def = KonceptDefinition.builder().identifier("Foo").build();
        assertTrue(KonceptIdentity.idString(def).isEmpty());
    }
}
