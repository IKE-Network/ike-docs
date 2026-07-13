package network.ike.docs.koncept;

import java.util.List;

/**
 * Immutable definition of a Koncept, including natural language definition,
 * description logic axiom, and optional terminology identifiers.
 *
 * @param identifier      CamelCase identifier used in markup (e.g., "HeartFailure")
 * @param label           Human-readable label (e.g., "Heart Failure")
 * @param definition      Natural language definition text
 * @param axiom           Description logic axiom string using Unicode DL symbols
 * @param sctid           Optional SNOMED CT concept identifier
 * @param iri             Optional OWL IRI for the concept
 * @param uuids           Optional explicit Tinkar PublicId UUIDs, in datastore
 *                        order, used to compute the Komet identicon; when absent
 *                        the identicon is derived from {@code sctid}
 * @param kind            Optional component kind ({@code concept}, {@code description},
 *                        {@code semantic}, {@code pattern}, {@code stamp}, {@code unknown});
 *                        {@code null} or absent means {@code concept} (the bare default)
 * @param broader         Optional identifiers of this koncept's supertypes (is-a parents);
 *                        the glossary renders them as parent chips and inverts them across
 *                        the source to render each koncept's children
 * @param section         Optional stable grouping key computed against the live knowledge
 *                        base (a taxonomy-subtree root's own identifier, or a positional
 *                        {@code ResidualN}/{@code Unclassified} bucket); the grouped
 *                        glossary mode sections the document by this field
 * @param since           Optional earliest stamp time of this koncept's own version chain,
 *                        formatted as recorded by the knowledge base
 * @param comments        Optional text of every currently active free-text comment
 *                        attached to this koncept
 * @param retiredComments Optional prior text and retirement time of a comment that has
 *                        since been superseded by an inactive version — "maybe in some
 *                        cases," per the source data; absent far more often than present
 * @param seeAlso         Optional identifiers of related koncepts (a javadoc-{@code @see}
 *                        analog); no Tinkar association/replacement pattern is wired
 *                        anywhere today, so this is reserved plumbing, not yet populated
 *                        by any known source
 * @param narrative       Optional curated, long-form AsciiDoc prose for this koncept (as
 *                        opposed to the short {@code definition} gloss) — real AsciiDoc
 *                        source, including embedded {@code k:} chip references, meant to
 *                        be spliced into a document and re-parsed (see the
 *                        {@code koncept-narrative} block macro), not rendered as flat text
 */
public record KonceptDefinition(
        String identifier,
        String label,
        String definition,
        String axiom,
        String sctid,
        String iri,
        List<String> uuids,
        String kind,
        List<String> broader,
        String section,
        String since,
        List<String> comments,
        List<RetiredComment> retiredComments,
        List<String> seeAlso,
        String narrative
) {

    /**
     * A comment's text immediately before it was retired, and the time of that
     * retirement.
     *
     * @param text      the comment's prior, active text
     * @param retiredAt the time it was superseded by an inactive version
     */
    public record RetiredComment(String text, String retiredAt) {
    }

    /**
     * Builder for constructing KonceptDefinition instances from parsed YAML
     * or programmatic sources.
     */
    public static class Builder {
        private String identifier;
        private String label;
        private String definition;
        private String axiom;
        private String sctid;
        private String iri;
        private List<String> uuids;
        private String kind;
        private List<String> broader;
        private String section;
        private String since;
        private List<String> comments;
        private List<RetiredComment> retiredComments;
        private List<String> seeAlso;
        private String narrative;

        /** Creates a new empty builder. */
        public Builder() {
        }

        /**
         * Sets the CamelCase identifier.
         *
         * @param identifier the identifier to set
         * @return this builder
         */
        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Sets the human-readable label.
         *
         * @param label the label to set
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the natural language definition text.
         *
         * @param definition the definition to set
         * @return this builder
         */
        public Builder definition(String definition) {
            this.definition = definition;
            return this;
        }

        /**
         * Sets the description logic axiom string.
         *
         * @param axiom the axiom to set
         * @return this builder
         */
        public Builder axiom(String axiom) {
            this.axiom = axiom;
            return this;
        }

        /**
         * Sets the SNOMED CT concept identifier.
         *
         * @param sctid the SNOMED CT identifier to set
         * @return this builder
         */
        public Builder sctid(String sctid) {
            this.sctid = sctid;
            return this;
        }

        /**
         * Sets the OWL IRI for the concept.
         *
         * @param iri the IRI to set
         * @return this builder
         */
        public Builder iri(String iri) {
            this.iri = iri;
            return this;
        }

        /**
         * Sets the explicit Tinkar PublicId UUIDs (in datastore order) used
         * to compute the Komet identicon.
         *
         * @param uuids the UUID strings to set
         * @return this builder
         */
        public Builder uuids(List<String> uuids) {
            this.uuids = uuids;
            return this;
        }

        /**
         * Sets the component kind ({@code concept}, {@code description}, {@code semantic},
         * {@code pattern}, {@code stamp}, {@code unknown}); {@code null} or absent means concept.
         *
         * @param kind the kind name to set
         * @return this builder
         */
        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        /**
         * Sets the identifiers of this koncept's supertypes (is-a parents).
         *
         * @param broader the parent identifiers to set
         * @return this builder
         */
        public Builder broader(List<String> broader) {
            this.broader = broader;
            return this;
        }

        /**
         * Sets the stable section-grouping key computed against the live knowledge base.
         *
         * @param section the section key to set
         * @return this builder
         */
        public Builder section(String section) {
            this.section = section;
            return this;
        }

        /**
         * Sets the earliest stamp time of this koncept's own version chain.
         *
         * @param since the formatted time to set
         * @return this builder
         */
        public Builder since(String since) {
            this.since = since;
            return this;
        }

        /**
         * Sets the text of every currently active comment attached to this koncept.
         *
         * @param comments the comment texts to set
         * @return this builder
         */
        public Builder comments(List<String> comments) {
            this.comments = comments;
            return this;
        }

        /**
         * Sets the prior text and retirement time of comments superseded by an
         * inactive version.
         *
         * @param retiredComments the retired comments to set
         * @return this builder
         */
        public Builder retiredComments(List<RetiredComment> retiredComments) {
            this.retiredComments = retiredComments;
            return this;
        }

        /**
         * Sets the identifiers of related koncepts (a javadoc-{@code @see} analog).
         *
         * @param seeAlso the related identifiers to set
         * @return this builder
         */
        public Builder seeAlso(List<String> seeAlso) {
            this.seeAlso = seeAlso;
            return this;
        }

        /**
         * Sets the curated, long-form AsciiDoc prose for this koncept.
         *
         * @param narrative the AsciiDoc source to set
         * @return this builder
         */
        public Builder narrative(String narrative) {
            this.narrative = narrative;
            return this;
        }

        /**
         * Builds an immutable {@link KonceptDefinition} from this builder's state.
         *
         * @return the constructed definition
         * @throws IllegalStateException if identifier is null or blank
         */
        public KonceptDefinition build() {
            if (identifier == null || identifier.isBlank()) {
                throw new IllegalStateException("KonceptDefinition requires an identifier");
            }
            if (label == null) {
                // Default: split camelCase
                label = identifier.replaceAll("([a-z])([A-Z])", "$1 $2");
            }
            List<String> uuidList = uuids != null ? List.copyOf(uuids) : List.of();
            List<String> broaderList = broader != null ? List.copyOf(broader) : List.of();
            List<String> commentsList = comments != null ? List.copyOf(comments) : List.of();
            List<RetiredComment> retiredCommentsList =
                    retiredComments != null ? List.copyOf(retiredComments) : List.of();
            List<String> seeAlsoList = seeAlso != null ? List.copyOf(seeAlso) : List.of();
            return new KonceptDefinition(identifier, label, definition, axiom, sctid, iri,
                    uuidList, kind, broaderList, section, since, commentsList, retiredCommentsList, seeAlsoList,
                    narrative);
        }
    }

    /**
     * Creates a new builder for constructing {@link KonceptDefinition} instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
