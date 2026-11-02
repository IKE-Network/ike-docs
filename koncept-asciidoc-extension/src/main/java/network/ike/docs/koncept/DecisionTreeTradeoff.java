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

import java.util.Optional;

/**
 * What the choice at the root trades, read from source: the store-free twin of the live
 * surface's {@code Tradeoff} ({@code komet-claude-plugin}): the expected money and the expected
 * healthy life lost along the course the rollback chose beneath each alternative, folded without
 * pricing one into the other. The chosen alternative then dominates, buys
 * health with money at a price per healthy year, or saves money by giving health up at one.
 *
 * @param money  how much more money the chosen alternative costs than the next best; negative when less
 * @param health how much more healthy life the chosen alternative keeps than the next best; negative when less
 * @param priced the price of a healthy year the tree was rolled back at
 */
record DecisionTreeTradeoff(Node chosen, Node nextBest, double money, double health, double priced) {

    /** The price of a healthy year at which the two alternatives tie; NaN when nothing is traded. */
    double pricePerHealthyYear() {
        boolean trade = (money > 1e-9 && health > 1e-9) || (money < -1e-9 && health < -1e-9);
        return trade ? Math.abs(money / health) : Double.NaN;
    }

    static Optional<DecisionTreeTradeoff> of(Node root, DecisionTreeRollback base) {
        Optional<Node> chosen = base.chosen(root);
        if (root.kind != Kind.DECISION || chosen.isEmpty()) {
            return Optional.empty();
        }
        Node nextBest = null;
        double nextCost = Double.POSITIVE_INFINITY;
        for (Node other : root.children) {
            if (other.kind != Kind.ALTERNATIVE || other == chosen.get()) {
                continue;
            }
            Optional<Interval> cost = base.value(other);
            if (cost.isPresent() && mid(cost.get()) < nextCost) {
                nextCost = mid(cost.get());
                nextBest = other;
            }
        }
        if (nextBest == null) {
            return Optional.empty();
        }
        Optional<double[]> chosenExpected = expected(base, chosen.get());
        Optional<double[]> otherExpected = expected(base, nextBest);
        if (chosenExpected.isEmpty() || otherExpected.isEmpty()) {
            return Optional.empty();
        }
        double money = chosenExpected.get()[0] - otherExpected.get()[0];
        double health = otherExpected.get()[1] - chosenExpected.get()[1];
        return Optional.of(new DecisionTreeTradeoff(chosen.get(), nextBest, money, health, base.healthPrice()));
    }

    /**
     * The expected money and the expected health lost from a node on, along the course the rollback
     * chose at every decision beneath it, kept apart; empty where a share or a cost is not
     * determinable, or a decision is open.
     */
    static Optional<double[]> expected(DecisionTreeRollback base, Node node) {
        switch (node.kind) {
            case DECISION -> {
                Optional<Node> chosen = base.chosen(node);
                return chosen.isPresent() ? expected(base, chosen.get()) : Optional.empty();
            }
            case CHANCE -> {
                double money = 0;
                double health = 0;
                for (Node event : node.children) {
                    Optional<Interval> share = base.share(event);
                    Optional<double[]> ahead = expected(base, event);
                    if (share.isEmpty() || ahead.isEmpty()) {
                        return Optional.empty();
                    }
                    money += mid(share.get()) * ahead.get()[0];
                    health += mid(share.get()) * ahead.get()[1];
                }
                return Optional.of(new double[] {money, health});
            }
            case ALTERNATIVE, EVENT -> {
                Optional<double[]> own = costs(base, node);
                Optional<Node> next = node.next();
                if (own.isEmpty()) {
                    return Optional.empty();
                }
                if (next.isEmpty()) {
                    return own;
                }
                return expected(base, next.get()).map(a -> new double[] {own.get()[0] + a[0], own.get()[1] + a[1]});
            }
            default -> {
                return costs(base, node);
            }
        }
    }

    private static Optional<double[]> costs(DecisionTreeRollback base, Node node) {
        double money = 0;
        double health = 0;
        for (Measure cost : node.costs()) {
            Optional<Interval> bounds = base.measure(cost);
            if (bounds.isEmpty()) {
                return Optional.empty();
            }
            if (cost.kind() == MeasureKind.MONEY) {
                money += mid(bounds.get());
            } else {
                health += mid(bounds.get());
            }
        }
        return Optional.of(new double[] {money, health});
    }

    private static double mid(Interval interval) {
        return (interval.lower() + interval.upper()) / 2;
    }
}
