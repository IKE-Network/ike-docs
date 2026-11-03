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
import network.ike.docs.koncept.DecisionTreeSource.Measure;
import network.ike.docs.koncept.DecisionTreeSource.MeasureKind;
import network.ike.docs.koncept.DecisionTreeSource.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The consequence ledger of a decision read from source: the store-free twin of the live
 * surface's {@code Ledger} ({@code komet-claude-plugin}). For each alternative at the root, what
 * a case who takes it can expect, every consequence kept in its own unit: money, healthy life
 * lost, and whatever the tree counts, folded from the leaves along the course the rollback chose
 * at every decision beneath, never priced into one another. Units are known by the name of the
 * concept that names them.
 */
final class DecisionTreeLedger {

    /** What a case who takes an alternative can expect. */
    record Entry(Node alternative, double money, double health, Map<String, Double> counts) {
        double count(String unit) {
            return counts.getOrDefault(unit, 0.0);
        }
    }

    private final List<Entry> entries;
    private final List<String> units;
    private final Optional<Node> chosen;

    private DecisionTreeLedger(List<Entry> entries, List<String> units, Optional<Node> chosen) {
        this.entries = Collections.unmodifiableList(entries);
        this.units = Collections.unmodifiableList(units);
        this.chosen = chosen;
    }

    static Optional<DecisionTreeLedger> of(Node root, DecisionTreeRollback rollback) {
        if (root == null || (root.kind != Kind.DECISION && root.kind != Kind.CHANCE)) {
            return Optional.empty();
        }
        List<String> units = new ArrayList<>();
        collectUnits(root, units);
        for (DecisionTreeRollback inner : rollback.foldedAll().values()) {
            collectUnits(inner.root(), units);
        }
        List<Entry> entries = new ArrayList<>();
        if (root.kind == Kind.CHANCE) {
            // a tree of consequences: one row, what everyone who enters it can expect
            expected(rollback, root).ifPresent(entries::add);
        }
        for (Node alternative : root.children) {
            if (alternative.kind == Kind.ALTERNATIVE) {
                expected(rollback, alternative).ifPresent(entries::add);
            }
        }
        return Optional.of(new DecisionTreeLedger(entries, units, rollback.chosen(root)));
    }

    private static void collectUnits(Node node, List<String> units) {
        for (Measure measure : node.measures) {
            if (measure.kind() == MeasureKind.COUNT && measure.semantic() != null && !units.contains(measure.semantic().shown())) {
                units.add(measure.semantic().shown());
            }
        }
        for (Node child : node.children) {
            collectUnits(child, units);
        }
    }

    List<Entry> entries() {
        return entries;
    }

    Optional<Entry> entry(Node alternative) {
        return entries.stream().filter(e -> e.alternative() == alternative).findFirst();
    }

    /** Every unit the tree counts, by the name of what is counted, in order of first appearance. */
    List<String> units() {
        return units;
    }

    Optional<Node> chosen() {
        return chosen;
    }

    /** What a case can expect from a node on, along the course the rollback chose; empty where not determinable or a decision is open. */
    static Optional<Entry> expected(DecisionTreeRollback base, Node node) {
        switch (node.kind) {
            case DECISION -> {
                Optional<Node> chosen = base.chosen(node);
                return chosen.isPresent() ? expected(base, chosen.get()).map(e -> new Entry(node, e.money(), e.health(), e.counts()))
                        : Optional.empty();
            }
            case CHANCE -> {
                Entry sum = new Entry(node, 0, 0, new LinkedHashMap<>());
                for (Node event : node.children) {
                    Optional<Interval> share = base.share(event);
                    Optional<Entry> ahead = expected(base, event);
                    if (share.isEmpty() || ahead.isEmpty()) {
                        return Optional.empty();
                    }
                    sum = plus(sum, ahead.get(), mid(share.get()));
                }
                return Optional.of(sum);
            }
            case ALTERNATIVE, EVENT -> {
                Optional<Entry> own = costs(base, node);
                Optional<Node> next = node.next();
                if (own.isEmpty()) {
                    return Optional.empty();
                }
                if (next.isEmpty()) {
                    return own;
                }
                return expected(base, next.get()).map(a -> plus(own.get(), a, 1));
            }
            default -> {
                Optional<Entry> own = costs(base, node);
                Optional<DecisionTreeRollback> inner = base.folded(node);
                if (own.isEmpty() || inner.isEmpty()) {
                    return own;
                }
                // what the tree it folds in holds for everyone who enters it
                return expected(inner.get(), inner.get().root()).map(a -> plus(own.get(), a, 1));
            }
        }
    }

    private static Entry plus(Entry a, Entry b, double weight) {
        Map<String, Double> counts = new LinkedHashMap<>(a.counts());
        b.counts().forEach((unit, count) -> counts.merge(unit, count * weight, Double::sum));
        return new Entry(a.alternative(), a.money() + weight * b.money(), a.health() + weight * b.health(), counts);
    }

    private static Optional<Entry> costs(DecisionTreeRollback base, Node node) {
        double money = 0;
        double health = 0;
        Map<String, Double> counts = new LinkedHashMap<>();
        for (Measure cost : node.costs()) {
            Optional<Interval> bounds = base.measure(cost);
            if (bounds.isEmpty()) {
                return Optional.empty();
            }
            switch (cost.kind()) {
                case MONEY -> money += mid(bounds.get());
                case HEALTH -> health += mid(bounds.get());
                case COUNT -> counts.merge(cost.semantic() == null ? "counted" : cost.semantic().shown(), mid(bounds.get()), Double::sum);
                default -> { }
            }
        }
        return Optional.of(new Entry(node, money, health, counts));
    }

    private static double mid(Interval interval) {
        return (interval.lower() + interval.upper()) / 2;
    }
}
