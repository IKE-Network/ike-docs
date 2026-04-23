package network.ike.docs.koncept;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class YamlKonceptDefinitionSourceTest {

    @Test
    void loadsDefinitionsFromClasspath() {
        // Uses the main koncepts.yml in src/main/resources
        KonceptDefinitionSource source =
                KonceptDefinitionSource.fromClasspath("/koncepts.yml");

        Optional<KonceptDefinition> hf = source.lookup("HeartFailure");
        assertTrue(hf.isPresent(), "HeartFailure should be defined");

        KonceptDefinition def = hf.get();
        assertEquals("Heart Failure", def.label());
        assertNotNull(def.definition());
        assertNotNull(def.axiom());
        assertEquals("84114007", def.sctid());
    }

    @Test
    void unknownIdentifier_returnsEmpty() {
        KonceptDefinitionSource source =
                KonceptDefinitionSource.fromClasspath("/koncepts.yml");

        Optional<KonceptDefinition> result = source.lookup("Nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void missingResource_returnsEmptySource() {
        KonceptDefinitionSource source =
                KonceptDefinitionSource.fromClasspath("/does-not-exist.yml");

        Optional<KonceptDefinition> result = source.lookup("HeartFailure");
        assertTrue(result.isEmpty());
    }

    @Test
    void axiomContainsDLSymbols() {
        KonceptDefinitionSource source =
                KonceptDefinitionSource.fromClasspath("/koncepts.yml");

        KonceptDefinition def = source.lookup("HeartFailure").orElseThrow();
        String axiom = def.axiom();

        // Verify DL symbols are preserved through YAML parsing
        assertTrue(axiom.contains("≡"), "Should contain equivalence symbol");
        assertTrue(axiom.contains("⊓"), "Should contain intersection symbol");
        assertTrue(axiom.contains("∃"), "Should contain existential quantifier");
    }

    @Test
    void camelCaseSplitting() {
        assertEquals("Heart Failure",
                KonceptInlineMacro.splitCamelCase("HeartFailure"));
        assertEquals("Acute Myocardial Infarction",
                KonceptInlineMacro.splitCamelCase("AcuteMyocardialInfarction"));
        assertEquals("Type Two Diabetes Mellitus",
                KonceptInlineMacro.splitCamelCase("TypeTwoDiabetesMellitus"));
    }
}
