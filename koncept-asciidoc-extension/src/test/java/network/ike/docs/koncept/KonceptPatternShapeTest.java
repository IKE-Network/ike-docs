package network.ike.docs.koncept;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code kind: pattern} koncept's {@code referencedComponentMeaning}/
 * {@code referencedComponentPurpose}/{@code fields} — parsing (IKE-Network/ike-issues#880)
 * and glossary rendering, against the {@code pattern-shape-test.yml} fixture.
 */
class KonceptPatternShapeTest {

    private static KonceptDefinitionSource source() {
        return KonceptDefinitionSource.fromClasspath("/pattern-shape-test.yml");
    }

    @Test
    void patternFieldsParseInDeclaredOrder() {
        KonceptDefinition def = source().lookup("TestPattern").orElseThrow();

        assertEquals("TestMeaning", def.referencedComponentMeaning());
        assertEquals("TestPurpose", def.referencedComponentPurpose());

        List<KonceptDefinition.PatternField> fields = def.fields();
        assertEquals(2, fields.size());
        assertEquals(new KonceptDefinition.PatternField("FieldOneMeaning", "FieldOnePurpose", "FieldOneType"),
                fields.get(0));
        assertEquals(new KonceptDefinition.PatternField("FieldTwoMeaning", "FieldTwoPurpose", "FieldTwoType"),
                fields.get(1));
    }

    @Test
    void aPlainConceptCarriesNoPatternFields() {
        KonceptDefinition def = source().lookup("PlainConcept").orElseThrow();

        assertEquals(null, def.referencedComponentMeaning());
        assertEquals(null, def.referencedComponentPurpose());
        assertTrue(def.fields().isEmpty());
    }

    @Test
    void glossaryEntryRendersReferencedComponentAndFields() {
        KonceptDefinitionSource source = source();
        KonceptDefinition def = source.lookup("TestPattern").orElseThrow();

        String html = KonceptGlossaryEntryRenderer.entryHtml(
                "TestPattern", Optional.of(def), null, Map.of(), source);

        assertTrue(html.contains("koncept-pattern-shape"), "must render the pattern-shape table:\n" + html);
        assertTrue(html.contains("koncept-pattern-referenced-component"),
                "must render the referenced-component row:\n" + html);
        assertTrue(html.contains("#koncept-TestMeaning"), "must link to the referenced-component meaning koncept:\n" + html);
        assertTrue(html.contains("#koncept-TestPurpose"), "must link to the referenced-component purpose koncept:\n" + html);

        assertTrue(html.contains("#koncept-FieldOneMeaning") && html.contains("#koncept-FieldOnePurpose")
                        && html.contains("#koncept-FieldOneType"),
                "must link to the first field's meaning/purpose/dataType koncepts:\n" + html);
        assertTrue(html.contains("#koncept-FieldTwoMeaning") && html.contains("#koncept-FieldTwoPurpose")
                        && html.contains("#koncept-FieldTwoType"),
                "must link to the second field's meaning/purpose/dataType koncepts:\n" + html);
    }

    @Test
    void glossaryEntryOmitsPatternSectionsForAPlainConcept() {
        KonceptDefinitionSource source = source();
        KonceptDefinition def = source.lookup("PlainConcept").orElseThrow();

        String html = KonceptGlossaryEntryRenderer.entryHtml(
                "PlainConcept", Optional.of(def), null, Map.of(), source);

        assertFalse(html.contains("koncept-pattern-shape"),
                "a plain concept must not render a pattern-shape table:\n" + html);
    }
}
