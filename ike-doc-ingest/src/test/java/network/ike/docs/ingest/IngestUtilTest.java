package network.ike.docs.ingest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestUtilTest {

    @Test
    void safeSlug_lowercases() {
        assertThat(IngestUtil.safeSlug("FooBar")).isEqualTo("foobar");
    }

    @Test
    void safeSlug_replacesNonAlphanumWithDashes() {
        assertThat(IngestUtil.safeSlug("foo bar baz")).isEqualTo("foo-bar-baz");
        assertThat(IngestUtil.safeSlug("foo/bar.baz")).isEqualTo("foo-bar-baz");
    }

    @Test
    void safeSlug_collapsesRuns() {
        assertThat(IngestUtil.safeSlug("foo  /  bar")).isEqualTo("foo-bar");
    }

    @Test
    void safeSlug_stripsLeadingAndTrailingDashes() {
        assertThat(IngestUtil.safeSlug("/foo/")).isEqualTo("foo");
    }

    @Test
    void safeSlug_preservesAlphanumericAndExistingDashes() {
        assertThat(IngestUtil.safeSlug("us-core-patient")).isEqualTo("us-core-patient");
    }

    @Test
    void yamlEscape_quotesQuotes() {
        assertThat(IngestUtil.yamlEscape("said \"hi\"")).isEqualTo("said \\\"hi\\\"");
    }

    @Test
    void yamlEscape_escapesBackslashes() {
        assertThat(IngestUtil.yamlEscape("foo\\bar")).isEqualTo("foo\\\\bar");
    }

    @Test
    void yamlEscape_flattensNewlines() {
        assertThat(IngestUtil.yamlEscape("line1\nline2")).isEqualTo("line1 line2");
    }

    @Test
    void yamlEscape_handlesNull() {
        assertThat(IngestUtil.yamlEscape(null)).isEmpty();
    }

    @Test
    void truncate_returnsOriginalIfShort() {
        assertThat(IngestUtil.truncate("hello", 100)).isEqualTo("hello");
    }

    @Test
    void truncate_breaksAtWordBoundary() {
        String s = "the quick brown fox jumps over the lazy dog";
        String t = IngestUtil.truncate(s, 20);
        assertThat(t).endsWith("…");
        assertThat(t).doesNotContain("ov…"); // should break at a space
    }

    @Test
    void stripMarkup_collapsesAndTrims() {
        assertThat(IngestUtil.stripMarkup("  foo\n\n  bar   baz  "))
                .isEqualTo("foo bar baz");
    }

    @Test
    void stripMarkup_handlesNull() {
        assertThat(IngestUtil.stripMarkup(null)).isEmpty();
    }
}
