---
date_published: 2026-05-09
date_modified: 2026-05-09
canonical_url: https://github.com/IKE-Network/ike-docs/dependencies.html
---

# Project Dependencies

## [provided](#provided)

The following is a list of provided dependencies for this project. These dependencies are required to compile the application, but should be provided by default when using the library:

| GroupId | ArtifactId | Version | Classifier | Type | Licenses |
| --- | --- | --- | --- | --- | --- |
| network.ike.tooling | [ike-build-standards](https://ike.network/ike-tooling/ike-build-standards/)[1] | 147 | site-theme | zip | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[2] |

# Project Transitive Dependencies

No transitive dependencies are required for this project.

# Project Dependency Graph

## [Dependency Tree](#dependency-tree)

- network.ike.docs:ike-docs:pom:8 ** 
  
  | IKE Docs |
  | --- |
  | **Description: **Documentation plumbing for the IKE Network. Hosts the ike-doc-maven-plugin (ike-doc packaging, AsciiDoc/render pipeline, PDF dispatch), the Koncept AsciiDoc extension, DocBook XSL + fonts, shared doc resources, and the semantic linebreak reformatter. Split from ike-pipeline to resolve the extensions=true reactor-load cycle. See IKE-Network/ike-issues#216. **URL: **[https://github.com/IKE-Network/ike-docs](https://github.com/IKE-Network/ike-docs)[3] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
  
    - network.ike.tooling:ike-build-standards:zip:site-theme:147 (provided) ** 
      
      | IKE Build Standards |
      | --- |
      | **Description: **Versioned Claude instruction files for IKE projects. Modular standards (Maven, Java, IKE-specific) distributed as a classified Maven artifact. **URL: **[https://ike.network/ike-tooling/ike-build-standards/](https://ike.network/ike-tooling/ike-build-standards/)[1] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[2] |

# Licenses

**Apache License, Version 2.0: **IKE Build Standards, IKE Docs

# Dependency File Details

| Total | Size | Entries | Classes | Packages | Java Version | Debug Information |
| --- | --- | --- | --- | --- | --- | --- |
| ike-build-standards-147-site-theme.zip | 3.4 kB | - | - | - | - | - |
| 1 | 3.4 kB | - | - | - | - | - |
| provided: 1 | provided: 3.4 kB | - | - | - | - | - |
