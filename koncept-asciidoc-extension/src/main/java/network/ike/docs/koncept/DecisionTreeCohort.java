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

import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Kind;
import network.ike.docs.koncept.DecisionTreeSource.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A decision tree read from source, counted out for a cohort: the store-free twin of the live
 * surface's {@code Cohort} ({@code komet-claude-plugin}), rule for rule. The whole cohort enters
 * the root and takes every alternative in turn; at a chance the count splits by the events'
 * shares as the rollback found them, the residual taking what the others leave; beneath a nested
 * decision the count follows the alternative the rollback chose. A tally is where the cohort
 * that takes each alternative at the root ends up, largest first, like outcomes merged by the
 * concept that names them.
 */
final class DecisionTreeCohort {

    /** One outcome a cohort reaches, and how many arrive. */
    record Leaf(Node node, double count) {
    }

    /** Where the cohort that takes one alternative at the root ends up. */
    record Tally(Node alternative, double count, List<Leaf> leaves) {
    }

    private final double size;
    private final Map<Node, Double> counts = new IdentityHashMap<>();
    private final List<Tally> tallies = new ArrayList<>();
    /** By outcome: the count of the tree it folds in, for those who reach the outcome. */
    private final Map<Node, DecisionTreeCohort> folded = new IdentityHashMap<>();

    private DecisionTreeCohort(Node root, DecisionTreeRollback rollback, double size) {
        this.size = size;
        if (root.kind == Kind.CHANCE) {
            // a tree of consequences: one tally, of everyone who enters it
            walk(root, rollback, size);
            Map<Object, Leaf> leaves = new LinkedHashMap<>();
            for (Node event : root.children) {
                if (event.kind == Kind.EVENT) {
                    gather(event, rollback, leaves);
                }
            }
            List<Leaf> sorted = new ArrayList<>(leaves.values());
            sorted.sort(Comparator.comparingDouble(Leaf::count).reversed());
            tallies.add(new Tally(root, size, List.copyOf(sorted)));
        }
        if (root.kind == Kind.DECISION) {
            walk(root, rollback, size);
            for (Node alternative : root.children) {
                if (alternative.kind != Kind.ALTERNATIVE) {
                    continue;
                }
                Map<Object, Leaf> leaves = new LinkedHashMap<>();
                gather(alternative, rollback, leaves);
                List<Leaf> sorted = new ArrayList<>(leaves.values());
                sorted.sort(Comparator.comparingDouble(Leaf::count).reversed());
                tallies.add(new Tally(alternative, size, List.copyOf(sorted)));
            }
        }
    }

    static DecisionTreeCohort of(Node root, DecisionTreeRollback rollback, double size) {
        return new DecisionTreeCohort(root, rollback, size);
    }

    double size() {
        return size;
    }

    /** How many of the cohort reach a node or take a branch; empty where a share on the way is not determinable. */
    OptionalDouble count(Node node) {
        Double count = counts.get(node);
        return count == null ? OptionalDouble.empty() : OptionalDouble.of(count);
    }

    List<Tally> tallies() {
        return tallies;
    }

    private void walk(Node node, DecisionTreeRollback rollback, double count) {
        counts.put(node, count);
        switch (node.kind) {
            case DECISION -> {
                for (Node alternative : node.children) {
                    walk(alternative, rollback, count);
                }
            }
            case CHANCE -> {
                for (Node event : node.children) {
                    Optional<Interval> share = rollback.share(event);
                    if (share.isPresent()) {
                        walk(event, rollback, count * (share.get().lower() + share.get().upper()) / 2);
                    }
                }
            }
            case ALTERNATIVE, EVENT -> node.next().ifPresent(next -> walk(next, rollback, count));
            case OUTCOME -> rollback.folded(node).ifPresent(inner ->
                    folded.put(node, new DecisionTreeCohort(inner.root(), inner, count)));
        }
    }

    private void gather(Node branch, DecisionTreeRollback rollback, Map<Object, Leaf> leaves) {
        OptionalDouble here = count(branch);
        if (here.isEmpty()) {
            return;
        }
        Optional<Node> next = branch.next();
        if (next.isEmpty()) {
            leaf(branch, here.getAsDouble(), leaves);
            return;
        }
        Node node = next.get();
        switch (node.kind) {
            case OUTCOME -> {
                // an outcome that folds in a tree ends where that tree's leaves do
                DecisionTreeCohort inner = folded.get(node);
                if (inner == null) {
                    leaf(node, here.getAsDouble(), leaves);
                } else {
                    for (Tally tally : inner.tallies()) {
                        for (Leaf leaf : tally.leaves()) {
                            leaf(leaf.node(), leaf.count(), leaves);
                        }
                    }
                }
            }
            case CHANCE -> {
                for (Node event : node.children) {
                    gather(event, rollback, leaves);
                }
            }
            case DECISION -> rollback.chosen(node).ifPresent(chosen -> gather(chosen, rollback, leaves));
            case ALTERNATIVE, EVENT -> gather(node, rollback, leaves);
        }
    }

    private static void leaf(Node node, double count, Map<Object, Leaf> leaves) {
        Object key = node.concept != null && node.concept.identifier() != null ? node.concept.identifier()
                : node.concept != null ? "label " + node.label() : node;
        Leaf was = leaves.get(key);
        leaves.put(key, new Leaf(node, (was == null ? 0 : was.count()) + count));
    }
}
