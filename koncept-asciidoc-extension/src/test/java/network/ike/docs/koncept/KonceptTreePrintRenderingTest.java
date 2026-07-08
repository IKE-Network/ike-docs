/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 2 of the {@code [koncept-tree]} block ({@code IKE-Network/ike-issues#827}): the print
 * backends. Each tree row re-parses through the inline {@code k:} macro, so DocBook emits an
 * {@code inlinemediaobject} per node and Prawn a native identicon image — the tree becomes a real
 * indented figure in FO/PDF rather than the raw-token literal-block fallback.
 */
class KonceptTreePrintRenderingTest {

    private static Asciidoctor asciidoctor;

    private static final String TREE = """
            [koncept-tree]
            ----
            k:HeartFailure[]
              k:AorticStenosis[Aortic Stenosis]
            k:sctid=44054006[Type 2 Diabetes Mellitus]
            ----
            """;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        // Both are needed: the block lays out the rows, the inline macro renders each node's
        // identicon per backend (exactly as the full pipeline registers them).
        asciidoctor.javaExtensionRegistry()
                .block(KonceptTreeBlockProcessor.class)
                .inlineMacro(KonceptInlineMacro.class);
    }

    @AfterAll
    static void teardown() {
        if (asciidoctor != null) {
            asciidoctor.close();
        }
    }

    @Test
    void docbook_treeEmitsInlineMediaObjectsAndCrossReferences() {
        String xml = convert("docbook5");

        assertTrue(xml.contains("<inlinemediaobject>"),
                "each resolved node renders a DocBook inline media object");
        assertTrue(xml.contains("format=\"PNG\""), "the identicon is a PNG");
        assertTrue(xml.contains("linkend=\"koncept-HeartFailure\""),
                "a name-key node links to its glossary entry");
        assertTrue(xml.contains("linkend=\"koncept-TypeTwoDiabetesMellitus\""),
                "a typed sctid node resolves through koncepts.yml and links too");
    }

    @Test
    void docbook_isNotTheLiteralFallback() {
        String xml = convert("docbook5");
        // The pre-stage-2 fallback emitted the raw tokens verbatim in a <literallayout>/<screen>.
        assertFalse(xml.contains("k:HeartFailure"),
                "the raw k: tokens must not appear — the tree is rendered, not dumped");
    }

    @Test
    void prawn_embedsIdenticonImagesForTheTree() throws Exception {
        byte[] withIcons = toPdf(true);
        byte[] textOnly = toPdf(false);

        assertTrue(isPdf(withIcons), "the tree renders to a valid PDF");
        String iconBody = new String(withIcons, StandardCharsets.ISO_8859_1);
        String textBody = new String(textOnly, StandardCharsets.ISO_8859_1);
        assertTrue(iconBody.contains("/Width 128") && iconBody.contains("/Height 128"),
                "Prawn embeds the 128x128 identicons of the tree's nodes");
        assertFalse(textBody.contains("/Width 128"),
                ":koncept-identicon: false renders the tree with no identicon images");
    }

    private String convert(String backend) {
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend(backend)
                .build();
        return asciidoctor.convert(TREE, options);
    }

    private byte[] toPdf(boolean identicon) throws Exception {
        File out = Files.createTempFile("koncept-tree-prawn", ".pdf").toFile();
        out.deleteOnExit();
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("pdf")
                .toFile(out)
                .attributes(Attributes.builder()
                        .attribute("koncept-identicon", identicon ? "true" : "false")
                        .build())
                .build();
        asciidoctor.convert(TREE, options);
        return Files.readAllBytes(out.toPath());
    }

    private static boolean isPdf(byte[] b) {
        return b.length > 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
    }
}
