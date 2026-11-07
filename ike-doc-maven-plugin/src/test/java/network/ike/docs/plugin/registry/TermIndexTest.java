package network.ike.docs.plugin.registry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TermIndex#termsOf}: the four syntax forms {@code IKE-INDEX.md}
 * allows, levels joined, duplicates folded, comments passed over.
 */
class TermIndexTest {

    @Test
    void termsOf_readsEveryForm_inOrder_once() {
        assertThat(TermIndex.termsOf("""
                // indexterm:[commented, out]
                indexterm:[description logic, EL++ profile]
                The ((necessary normal form)) transformation and indexterm2:[subsumption] testing.
                A (((role group, nesting))) concealed term, then ((necessary normal form)) again.
                indexterm:[ description logic , EL++ profile ]
                Not a term: a ((( stray.
                """)).containsExactly(
                "description logic > EL++ profile",
                "necessary normal form",
                "subsumption",
                "role group > nesting");
    }

    @Test
    void termsOf_isEmptyForAFileWithoutTerms() {
        assertThat(TermIndex.termsOf("= Title\n\nPlain text (with parentheses).\n")).isEmpty();
    }
}
