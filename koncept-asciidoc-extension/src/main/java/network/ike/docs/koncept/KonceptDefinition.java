package network.ike.docs.koncept;

import java.util.ArrayList;
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
 * @param referencedComponentMeaning Optional identifier of a {@code kind: pattern} koncept's
 *                        own referenced-component meaning concept — what a semantic of this
 *                        pattern's referenced component means (Tinkar's wire schema calls
 *                        this pair {@code ReferencedComponentMeaning}/{@code Purpose}; the
 *                        Java entity API calls it {@code semanticMeaning}/{@code Purpose} —
 *                        both names describe the same field); {@code null} for a concept
 * @param referencedComponentPurpose Optional identifier of a {@code kind: pattern} koncept's
 *                        own referenced-component purpose concept; {@code null} for a concept
 * @param referencedComponentExample Optional referenced component of a real, deterministically
 *                        chosen semantic of this pattern already in the knowledge base — a
 *                        koncept identifier when it resolves to one, otherwise its plain display
 *                        text; {@code null} for a concept, or for a pattern with no semantics yet
 * @param fields          Optional field definitions of a {@code kind: pattern} koncept, in
 *                        declared order — each field's own meaning, purpose, and data-type
 *                        koncept identifiers; empty for a concept
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
        String narrative,
        String referencedComponentMeaning,
        String referencedComponentPurpose,
        String referencedComponentExample,
        List<PatternField> fields
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
     * One field of a {@code kind: pattern} koncept: its own meaning, purpose, and data-type
     * koncept identifiers, in the pattern's own declared field order.
     *
     * @param meaning  the field's own meaning koncept identifier
     * @param purpose  the field's own purpose koncept identifier
     * @param dataType the field's own data-type koncept identifier
     * @param example  this field's actual value on the same example semantic used for
     *                 {@link KonceptDefinition#referencedComponentExample()} — a koncept
     *                 identifier if the value resolves to one, otherwise its plain display
     *                 text — or {@code null} if no example semantic was found
     */
    public record PatternField(String meaning, String purpose, String dataType, String example) {
    }

    /**
     * One Model Feature of a {@code kind: pattern} koncept in presentation form: the
     * referenced component first, then each declared field, labeled 1-indexed
     * ({@code Field 1} is the first declared field). The referenced component is never
     * numbered because it is not part of the fields array at either the schema or the
     * instance level.
     *
     * @param label    the feature's display label — {@code Referenced component} with the
     *                 generic any-component phrase, or {@code Field N}
     * @param meaning  the feature's meaning koncept identifier, or {@code null}
     * @param purpose  the feature's purpose koncept identifier, or {@code null}
     * @param dataType the feature's data-type koncept identifier, or {@code null} for the
     *                 referenced component, which has none
     * @param example  the feature's live example value, or {@code null} when the
     *                 extraction found no example semantic
     */
    public record ModelFeature(String label, String meaning, String purpose,
                               String dataType, String example) {
    }

    /**
     * This pattern's Model Features in presentation order — the single source of truth
     * behind every shape rendering (the {@code koncept-pattern-table} block macro and the
     * glossary's pattern-shape table): the referenced component, then each declared field
     * in order.
     *
     * @return the Model Features, or an empty list for a koncept with no shape at all
     */
    public List<ModelFeature> modelFeatures() {
        if (referencedComponentMeaning == null && referencedComponentPurpose == null
                && fields.isEmpty()) {
            return List.of();
        }
        List<ModelFeature> features = new ArrayList<>();
        features.add(new ModelFeature(
                "Referenced component — the component (concept, semantic, pattern, or STAMP) "
                        + "this pattern's semantics attach to",
                referencedComponentMeaning, referencedComponentPurpose, null,
                referencedComponentExample));
        int displayIndex = 1;
        for (PatternField field : fields) {
            features.add(new ModelFeature("Field " + displayIndex++,
                    field.meaning(), field.purpose(), field.dataType(), field.example()));
        }
        return List.copyOf(features);
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
        private String referencedComponentMeaning;
        private String referencedComponentPurpose;
        private String referencedComponentExample;
        private List<PatternField> fields;

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
         * Sets a {@code kind: pattern} koncept's own referenced-component meaning concept
         * identifier.
         *
         * @param referencedComponentMeaning the referenced-component meaning identifier to set
         * @return this builder
         */
        public Builder referencedComponentMeaning(String referencedComponentMeaning) {
            this.referencedComponentMeaning = referencedComponentMeaning;
            return this;
        }

        /**
         * Sets a {@code kind: pattern} koncept's own referenced-component purpose concept
         * identifier.
         *
         * @param referencedComponentPurpose the referenced-component purpose identifier to set
         * @return this builder
         */
        public Builder referencedComponentPurpose(String referencedComponentPurpose) {
            this.referencedComponentPurpose = referencedComponentPurpose;
            return this;
        }

        /**
         * Sets a {@code kind: pattern} koncept's referenced-component example — the referenced
         * component of a real semantic of this pattern already in the knowledge base.
         *
         * @param referencedComponentExample the referenced-component example to set
         * @return this builder
         */
        public Builder referencedComponentExample(String referencedComponentExample) {
            this.referencedComponentExample = referencedComponentExample;
            return this;
        }

        /**
         * Sets a {@code kind: pattern} koncept's own field definitions, in declared order.
         *
         * @param fields the field definitions to set
         * @return this builder
         */
        public Builder fields(List<PatternField> fields) {
            this.fields = fields;
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
            List<PatternField> fieldsList = fields != null ? List.copyOf(fields) : List.of();
            return new KonceptDefinition(identifier, label, definition, axiom, sctid, iri,
                    uuidList, kind, broaderList, section, since, commentsList, retiredCommentsList, seeAlsoList,
                    narrative, referencedComponentMeaning, referencedComponentPurpose,
                    referencedComponentExample, fieldsList);
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
