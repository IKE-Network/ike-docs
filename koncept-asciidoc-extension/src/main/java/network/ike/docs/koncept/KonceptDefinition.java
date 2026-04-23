package network.ike.docs.koncept;

/**
 * Immutable definition of a Koncept, including natural language definition,
 * description logic axiom, and optional terminology identifiers.
 *
 * @param identifier   CamelCase identifier used in markup (e.g., "HeartFailure")
 * @param label        Human-readable label (e.g., "Heart Failure")
 * @param definition   Natural language definition text
 * @param axiom        Description logic axiom string using Unicode DL symbols
 * @param sctid        Optional SNOMED CT concept identifier
 * @param iri          Optional OWL IRI for the concept
 */
public record KonceptDefinition(
        String identifier,
        String label,
        String definition,
        String axiom,
        String sctid,
        String iri
) {

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
            return new KonceptDefinition(identifier, label, definition, axiom, sctid, iri);
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
