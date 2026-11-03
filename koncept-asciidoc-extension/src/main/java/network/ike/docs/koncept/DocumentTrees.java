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

import network.ike.docs.koncept.DecisionTreeSource.Node;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The decision trees of one document, by their roots' identifiers: a tree written once, the
 * consequences of a rupture, is folded in at an outcome of a tree after it, {@code tree=…}, and
 * the two link to each other. A tree that folds another in links forward to it, and appends to the
 * other's block, drawn already, the link back.
 */
final class DocumentTrees {

    private static final Map<Document, DocumentTrees> BY_DOCUMENT = new WeakHashMap<>();

    private final Map<String, Node> trees = new LinkedHashMap<>();
    /** By root identifier: the block that draws the tree, where a link back is appended. */
    private final Map<String, StructuralNode> blocks = new LinkedHashMap<>();
    /** By the root of a folded tree: the roots of the trees that fold it in. */
    private final Map<String, Set<String>> foldedBy = new LinkedHashMap<>();

    private DocumentTrees() {
    }

    static synchronized DocumentTrees of(Document document) {
        return BY_DOCUMENT.computeIfAbsent(document, d -> new DocumentTrees());
    }

    /** The anchor a tree's block carries, for links to it. */
    static String anchor(String identifier) {
        return "decision-tree-" + identifier.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    synchronized void define(String identifier, Node root, StructuralNode block) {
        if (identifier != null && root != null) {
            trees.put(identifier, root);
            blocks.put(identifier, block);
        }
    }

    /** The tree whose root carries an identifier, when a block before this point drew it. */
    synchronized Optional<Node> tree(String identifier) {
        return Optional.ofNullable(trees.get(identifier));
    }

    /** The block that draws a tree, when a block before this point drew it. */
    synchronized Optional<StructuralNode> block(String identifier) {
        return Optional.ofNullable(blocks.get(identifier));
    }

    /** Notes that one tree folds in another; true the first time, when the link back is still to be written. */
    synchronized boolean foldedBy(String folded, String folding) {
        return folded != null && folding != null && foldedBy.computeIfAbsent(folded, k -> new LinkedHashSet<>()).add(folding);
    }
}
