/*
 * Copyright © 2026 IKE Network (support@ike.network)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.koncept;

import network.ike.docs.koncept.ComplexConceptSource.Focused;
import network.ike.docs.koncept.ComplexConceptSource.Parsed;
import network.ike.docs.koncept.ComplexConceptSource.Prerequisite;
import network.ike.docs.koncept.ComplexConceptSource.Ref;
import network.ike.docs.koncept.ComplexConceptSource.Spec;
import network.ike.docs.koncept.ComplexConceptSource.Uniform;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.asciidoctor.ast.ContentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * The {@code [complex-concept]} block in the documentation build: the static projection of the
 * complex concept editor the assistant surface draws for the same block
 * ({@code dev-decision-point-knowledge}). It reads the block store-free
 * ({@link ComplexConceptSource}) and puts in its place the document form of the editor: one
 * sentence naming the concept, what it is a kind of and what its expansion focuses on, then a
 * table with one row per prerequisite, each an ANF performance statement whose circumstance fixes
 * the result, the statement's topic, the result in words, and the value as written with its unit.
 *
 * <pre>
 * [complex-concept, title="The decision point"]
 * ----
 * concept: k:SurveillanceDecision[Surveillance imaging decision after aneurysm seal] @id=rda-surveillance-decision
 *   expansion: focused k:ImagingDecision[Surveillance imaging decision] @id=rda-imaging-decision
 *     prerequisite: k:AneurysmSealed[Intracranial aneurysm sealed by endovascular treatment] @id=rda-aneurysm-sealed
 *       value = [1,1] k:Presence[presence] @id=rda-presence
 * ----
 * </pre>
 *
 * A concept with a SNOMED CT identifier or a UUID is a Koncept chip; one whose identifier is a
 * symbol local to the document is set in italics with the role {@code koncept-local}, the dashed
 * chip of the live surface, and a note says so. The block is remembered by its identifier, so a
 * {@code [decision-tree]} later in the document whose decision names it says which decision
 * point it is. A block with no readable root renders as a literal block, so the text is visible,
 * never dropped. The output is AsciiDoc re-parsed in place, so it is backend-agnostic.
 */
@Name("complex-concept")
@Contexts(Contexts.LISTING)
@ContentModel(ContentModel.RAW)
public class ComplexConceptBlockProcessor extends BlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ComplexConceptBlockProcessor.class);

    /** The role the block's container carries, so a stylesheet can set it apart. */
    static final String ROLE = "complex-concept";

    /** The role of a concept local to the document: the dashed chip of the live surface. */
    static final String ROLE_LOCAL = "koncept-local";

    private static final Map<Document, Map<String, Spec>> BY_DOCUMENT = new WeakHashMap<>();

    /** Creates a new block processor instance. */
    public ComplexConceptBlockProcessor() {
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        List<String> lines = reader.readLines();
        Parsed parsed = ComplexConceptSource.parse(lines);
        if (parsed.spec() == null) {
            LOG.debug("complex-concept: no readable root; rendering raw content as literal: {}", parsed.problems());
            return createBlock(parent, "literal", lines);
        }
        Spec spec = parsed.spec();
        define(parent.getDocument(), spec);
        Map<String, Object> containerAttributes = new HashMap<>();
        containerAttributes.put("role", ROLE);
        StructuralNode container = createBlock(parent, "open", new ArrayList<String>(), containerAttributes);
        String title = attributes.get("title") == null ? null : String.valueOf(attributes.get("title"));
        parseContent(container, source(spec, parsed, title));
        return container;
    }

    /** The AsciiDoc that stands in the block's place. */
    static List<String> source(Spec spec, Parsed parsed, String title) {
        List<String> source = new ArrayList<>();
        source.add(sentence(spec));
        source.add("");
        switch (spec.shape()) {
            case null -> {
            }
            case Focused focused -> {
                if (!focused.prerequisites().isEmpty()) {
                    source.add("." + (title == null ? "The prerequisites, each an ANF performance statement whose circumstance fixes the result"
                            : title));
                    source.add("[cols=\"6,2,4\", options=\"header\", role=\"" + ROLE + "\"]");
                    source.add("|===");
                    source.add("| Statement topic | Result | Value");
                    for (Prerequisite prerequisite : focused.prerequisites()) {
                        String value = prerequisite.value() == null ? "" : ComplexConceptSource.interval(prerequisite.value())
                                + (prerequisite.unit() == null ? "" : " " + chip(prerequisite.unit()));
                        source.add("| " + chip(prerequisite.topic()) + " | " + cell(ComplexConceptSource.valueWords(prerequisite))
                                + " | " + value);
                    }
                    source.add("|===");
                    source.add("");
                }
            }
            case Uniform uniform -> {
                if (!uniform.members().isEmpty()) {
                    source.add("." + (title == null ? "The members of " + spec.concept().shown() : title));
                    source.add("[cols=\"1\", options=\"header\", role=\"" + ROLE + "\"]");
                    source.add("|===");
                    source.add("| Member");
                    for (Ref member : uniform.members()) {
                        source.add("| " + chip(member));
                    }
                    source.add("|===");
                    source.add("");
                }
            }
        }
        List<String> notes = new ArrayList<>();
        if (!parsed.localSymbols().isEmpty()) {
            notes.add("A name in italics is a concept local to the document: the knowledge base holds nothing under its"
                    + " identifier yet, and proposing the complex concept in Komet writes it.");
        }
        List<String> missing = ComplexConceptSource.completeness(spec);
        if (!missing.isEmpty()) {
            notes.add("The specification still needs " + String.join("; ", missing) + ".");
        }
        if (!parsed.problems().isEmpty()) {
            notes.add("The block has problems: " + String.join("; ", parsed.problems()) + ".");
        }
        if (!notes.isEmpty()) {
            source.add("NOTE: " + String.join(" ", notes));
            source.add("");
        }
        return source;
    }

    /** The sentence that names the concept, its definition and its expansion. */
    static String sentence(Spec spec) {
        StringBuilder sb = new StringBuilder("*Complex concept* ").append(chip(spec.concept()));
        spec.definitionParent().ifPresent(parent -> sb.append(", a kind of ").append(chip(parent)));
        switch (spec.shape()) {
            case null -> sb.append(", with no expansion yet.");
            case Focused focused -> sb.append(". Its focused expansion is on ").append(chip(focused.focus()))
                    .append(focused.prerequisites().isEmpty() ? "." : ", and it holds where every prerequisite below holds.");
            case Uniform uniform -> {
                sb.append(". Its uniform expansion holds where ")
                        .append("and".equals(uniform.conjunction()) ? "every member" : "any member");
                if (uniform.domain() != null) {
                    sb.append(" is ").append(chip(uniform.domain()));
                }
                if (uniform.unsafe() != null) {
                    sb.append("; a value that is not absent is handled by ").append(uniform.unsafe());
                }
                sb.append('.');
            }
        }
        return sb.toString();
    }

    /** A concept as the document shows it: a Koncept chip when published, italics with a role when local. */
    static String chip(Ref ref) {
        String label = ref.shown().replace("]", "\\]");
        if (ref.sctid()) {
            return "k:sctid=" + ref.identifier() + "[" + label + "]";
        }
        if (ref.uuid()) {
            return "k:uuid=" + ref.identifier() + "[" + label + "]";
        }
        return "[." + ROLE_LOCAL + "]_" + cell(ref.shown()) + "_";
    }

    private static String cell(String text) {
        return DecisionTreeFormat.cell(text);
    }

    // ── The document's complex concepts ──────────────────────────────────────

    private static synchronized void define(Document document, Spec spec) {
        BY_DOCUMENT.computeIfAbsent(document, d -> new HashMap<>()).put(spec.concept().identifier(), spec);
    }

    /** The complex concept a block earlier in the document stated under an identifier. */
    static synchronized Optional<Spec> defined(Document document, String identifier) {
        if (document == null || identifier == null) {
            return Optional.empty();
        }
        Map<String, Spec> specs = BY_DOCUMENT.get(document);
        return specs == null ? Optional.empty() : Optional.ofNullable(specs.get(identifier));
    }
}
