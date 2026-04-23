# Koncept AsciiDoc Extension

AsciidoctorJ extension for inline **Koncept** markup with auto-generated
glossary containing natural language definitions and description logic axioms.

## Quick Start

### 1. Add dependency to your documentation build

```xml
<plugin>
    <groupId>org.asciidoctor</groupId>
    <artifactId>asciidoctor-maven-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.ike.community</groupId>
            <artifactId>koncept-asciidoc-extension</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</plugin>
```

### 2. Create a `koncepts.yml` definitions file

```yaml
HeartFailure:
  label: Heart Failure
  definition: >
    A clinical syndrome resulting from structural or functional
    impairment of ventricular filling or ejection of blood.
  axiom: "≡ ClinicalSyndrome ⊓ ∃hasPathology.(InsufficientCardiacOutput)"
  sctid: "84114007"
```

### 3. Use `k:` inline macros in your AsciiDoc

```asciidoc
The patient presented with k:HeartFailure[] and k:AorticStenosis[].

A label override works too: k:HeartFailure[Congestive Heart Failure].
```

A **Referenced Koncepts** glossary section is automatically appended to
the document with deduplicated entries.

## Syntax

| Markup | Renders As | Notes |
|--------|-----------|-------|
| `k:HeartFailure[]` | **K** Heart Failure (SVG badge) | CamelCase auto-split |
| `k:HeartFailure[CHF]` | **K** CHF (SVG badge) | Explicit label |
| `k:HeartFailure[Congestive Heart Failure]` | **K** Congestive Heart Failure | Long label |

## Configuration

Set these document attributes to control definition loading:

| Attribute | Default | Description |
|-----------|---------|-------------|
| `:koncept-definitions-file:` | — | Filesystem path to YAML definitions |
| `:koncept-definitions-classpath:` | — | Classpath resource path |
| (neither set) | `/koncepts.yml` | Default classpath resource |

## YAML Definition Format

```yaml
ConceptIdentifier:
  label: Human Readable Label       # optional, defaults to CamelCase split
  definition: Natural language text  # displayed in glossary
  axiom: "DL axiom string"          # displayed in code block
  sctid: "12345678"                 # optional SNOMED CT ID
  iri: "http://snomed.info/id/..."  # optional OWL IRI
```

### DL Notation Symbols

| Symbol | Meaning | Unicode |
|--------|---------|---------|
| ≡ | equivalentTo | U+2261 |
| ⊑ | subClassOf | U+2291 |
| ⊓ | intersection (and) | U+2293 |
| ⊔ | union (or) | U+2294 |
| ∃ | someValuesFrom | U+2203 |
| ∀ | allValuesFrom | U+2200 |
| ¬ | complement (not) | U+00AC |

## Architecture

```
k:HeartFailure[]   ──→  KonceptInlineMacro  ──→  SVG badge + anchor link
                              │
                              ▼
                       Document Registry
                       (koncept-registry attr)
                              │
                              ▼
                   KonceptGlossaryProcessor  ──→  HTML glossary section
                              │
                              ▼
                   KonceptDefinitionSource
                   (YAML / OWL API / IKE graph)
```

## Extending

Implement `KonceptDefinitionSource` to source definitions from
alternative backends:

```java
public class OwlApiKonceptSource implements KonceptDefinitionSource {
    private final OWLOntology ontology;
    private final OWLReasoner reasoner;

    @Override
    public Optional<KonceptDefinition> lookup(String identifier) {
        // Resolve concept, extract axioms, render to DL string
    }
}
```

## Building

```bash
mvn clean install
```

## License

Apache 2.0 (or as specified by IKE Community governance)
