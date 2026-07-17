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
        assertEquals(new KonceptDefinition.PatternField("FieldOneMeaning", "FieldOnePurpose", "FieldOneType", null),
                fields.get(0));
        assertEquals(new KonceptDefinition.PatternField("FieldTwoMeaning", "FieldTwoPurpose", "FieldTwoType", null),
                fields.get(1));
    }

    @Test
    void patternExampleValuesParseCorrectly() {
        // A dedicated fixture (not pattern-shape-test.yml): KonceptPatternListBlockMacroTest
        // asserts an exact pattern count/order against that shared file.
        KonceptDefinition def = KonceptDefinitionSource.fromClasspath("/pattern-shape-example-test.yml")
                .lookup("PatternWithExample").orElseThrow();

        assertEquals("ExampleTestMeaning", def.referencedComponentExample());

        List<KonceptDefinition.PatternField> fields = def.fields();
        assertEquals("ExampleFieldOneMeaning", fields.get(0).example());
        assertEquals("a literal example value", fields.get(1).example());
    }

    @Test
    void aPlainConceptCarriesNoPatternFields() {
        KonceptDefinition def = source().lookup("PlainConcept").orElseThrow();

        assertEquals(null, def.referencedComponentMeaning());
        assertEquals(null, def.referencedComponentPurpose());
        assertTrue(def.fields().isEmpty());
    }

    @Test
    void modelFeaturesLeadWithTheReferencedComponentThenFieldsOneIndexed() {
        KonceptDefinition def = source().lookup("TestPattern").orElseThrow();

        List<KonceptDefinition.ModelFeature> features = def.modelFeatures();
        assertEquals(3, features.size());
        assertTrue(features.get(0).label().startsWith("Referenced component"),
                "the referenced component leads, never numbered as Field 0");
        assertEquals(null, features.get(0).dataType(),
                "the referenced component has no data type");
        assertEquals("Field 1", features.get(1).label());
        assertEquals("FieldOneMeaning", features.get(1).meaning());
        assertEquals("Field 2", features.get(2).label());

        assertTrue(source().lookup("PlainConcept").orElseThrow().modelFeatures().isEmpty(),
                "a plain concept has no Model Features");
    }

    @Test
    void glossaryEntryRendersReferencedComponentAndFields() {
        KonceptDefinitionSource source = source();
        KonceptDefinition def = source.lookup("TestPattern").orElseThrow();

        String html = KonceptGlossaryEntryRenderer.entryHtml(
                "TestPattern", Optional.of(def), null, Map.of(), source);

        assertTrue(html.contains("koncept-pattern-table"),
                "must render the pattern-shape table with the block macro's shared style hook:\n" + html);
        assertTrue(html.contains("Referenced component") && html.contains("koncept-model-feature"),
                "must render the referenced-component label row:\n" + html);
        assertTrue(html.contains("Field 1") && html.contains("Field 2"),
                "fields display 1-indexed as spanning label rows:\n" + html);
        assertTrue(html.contains("&mdash;"),
                "the referenced component's data-type cell is an em dash:\n" + html);
        assertTrue(html.contains("#koncept-TestMeaning"), "must link to the referenced-component meaning koncept:\n" + html);
        assertTrue(html.contains("#koncept-TestPurpose"), "must link to the referenced-component purpose koncept:\n" + html);

        assertTrue(html.contains("#koncept-FieldOneMeaning") && html.contains("#koncept-FieldOnePurpose")
                        && html.contains("#koncept-FieldOneType"),
                "must link to the first field's meaning/purpose/dataType koncepts:\n" + html);
        assertTrue(html.contains("#koncept-FieldTwoMeaning") && html.contains("#koncept-FieldTwoPurpose")
                        && html.contains("#koncept-FieldTwoType"),
                "must link to the second field's meaning/purpose/dataType koncepts:\n" + html);
        assertFalse(html.contains("e.g."),
                "no example values in this fixture means no e.g. rows:\n" + html);
    }

    @Test
    void glossaryEntryRendersExampleRowsMatchingTheBlockMacro() {
        KonceptDefinitionSource source =
                KonceptDefinitionSource.fromClasspath("/pattern-shape-example-test.yml");
        KonceptDefinition def = source.lookup("PatternWithExample").orElseThrow();

        String html = KonceptGlossaryEntryRenderer.entryHtml(
                "PatternWithExample", Optional.of(def), null, Map.of(), source);

        assertTrue(html.contains("koncept-feature-example"),
                "example rows carry the shared stylesheet hook:\n" + html);
        assertTrue(html.contains("e.g."), "example rows carry the e.g. marker:\n" + html);
        assertTrue(html.contains("#koncept-ExampleTestMeaning"),
                "a resolvable example renders as a chip:\n" + html);
        assertTrue(html.contains("a literal example value"),
                "an unresolvable example renders as plain text:\n" + html);
    }

    @Test
    void glossaryEntryOmitsPatternSectionsForAPlainConcept() {
        KonceptDefinitionSource source = source();
        KonceptDefinition def = source.lookup("PlainConcept").orElseThrow();

        String html = KonceptGlossaryEntryRenderer.entryHtml(
                "PlainConcept", Optional.of(def), null, Map.of(), source);

        assertFalse(html.contains("koncept-pattern-table"),
                "a plain concept must not render a pattern-shape table:\n" + html);
    }
}
