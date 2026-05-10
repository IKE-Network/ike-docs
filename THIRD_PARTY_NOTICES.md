# Third-Party Notices — IKE Docs

Three layers of attribution ship with each release:

1. **Software Bill of Materials (CycloneDX, machine-readable):**
   - https://ike.network/ike-docs/bom.json
   - https://ike.network/ike-docs/bom.xml
   - Full transitive dependency graph, SPDX-normalized licenses, artifact hashes.
   - Also reachable as a Maven artifact with `<classifier>cyclonedx</classifier>`.

2. **Maven Site dependency report (HTML, human-browseable):**
   - https://ike.network/ike-docs/dependencies.html
   - https://ike.network/ike-docs/licenses.html

3. **Curated Third-Party Notices (this document):**
   - **Current release:** https://ike.network/ike-docs/THIRD_PARTY_NOTICES.html
   - **Versioned:** https://ike.network/ike-docs/&lt;version&gt;/THIRD_PARTY_NOTICES.html
   - **Latest:** https://ike.network/ike-docs/latest/THIRD_PARTY_NOTICES.html
   - The source AsciiDoc lives at [`src/site/asciidoc/THIRD_PARTY_NOTICES.adoc`](src/site/asciidoc/THIRD_PARTY_NOTICES.adoc).

## What's covered

The curated document acknowledges third-party open-source software
that mechanical reports either don't reach or under-report: the
AsciiDoc rendering chain, Kroki diagram service, external PDF
renderers (Prince, Antenna House, WeasyPrint, FOP, XEP), DocBook
XSL toolchain, embedded Noto fonts, and frontend assets (Font
Awesome, Prism) bundled into rendered HTML.

For corresponding notices in the rest of the IKE platform see:

- [ike-tooling](https://ike.network/ike-tooling/THIRD_PARTY_NOTICES.html) — Maven build infrastructure, plugin core, signing.
- [ike-platform](https://ike.network/ike-platform/THIRD_PARTY_NOTICES.html) — Java toolchain, BOM-managed dependencies, test framework.

Issues or omissions: file at https://github.com/IKE-Network/ike-issues.
