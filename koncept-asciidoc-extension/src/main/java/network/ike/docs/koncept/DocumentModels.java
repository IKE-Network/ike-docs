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

import org.asciidoctor.ast.Document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * The graphlet models of one document, by id: the text each {@code [graphlet-model]} block holds,
 * which the renderings after it, {@code [decision-tree, model=…]} and {@code [tornado, model=…]},
 * read again as if it were their own body. The latest block under an id holds it. Held per
 * Asciidoctor document and dropped with it, as the document's trees and calculations are.
 */
final class DocumentModels {

    private static final Map<Document, DocumentModels> BY_DOCUMENT = new WeakHashMap<>();

    private final Map<String, List<String>> models = new LinkedHashMap<>();

    private DocumentModels() {
    }

    static synchronized DocumentModels of(Document document) {
        return BY_DOCUMENT.computeIfAbsent(document, d -> new DocumentModels());
    }

    synchronized void define(String id, List<String> lines) {
        if (id != null && !id.isBlank()) {
            models.put(id, List.copyOf(lines));
        }
    }

    /** The text of the model a block before this point defined under an id. */
    synchronized Optional<List<String>> model(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(models.get(id));
    }
}
