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
import network.ike.docs.koncept.DecisionTreeSource.Node;

import java.util.ArrayList;
import java.util.List;
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
record DecisionTreeTradeoff(Node chosen, Node nextBest, double money, double health, double priced, List<CountTrade> counts) {

    /** How a counted unit stands between the chosen alternative and the next best, per case. */
    record CountTrade(String unit, double chosen, double other) {
        double more() {
            return chosen - other;
        }
    }

    /** Whether the chosen alternative brings more of any counted unit than the next best. */
    boolean countedAgainst() {
        return counts.stream().anyMatch(trade -> trade.more() > 1e-9);
    }

    /** The price of a healthy year at which the two alternatives tie; NaN when nothing is traded. */
    double pricePerHealthyYear() {
        boolean trade = (money > 1e-9 && health > 1e-9) || (money < -1e-9 && health < -1e-9);
        return trade ? Math.abs(money / health) : Double.NaN;
    }

    static Optional<DecisionTreeTradeoff> of(Node root, DecisionTreeRollback base) {
        Optional<DecisionTreeLedger> ledger = DecisionTreeLedger.of(root, base);
        Optional<Node> chosen = base.chosen(root);
        if (ledger.isEmpty() || chosen.isEmpty()) {
            return Optional.empty();
        }
        Node nextBest = null;
        double nextCost = Double.POSITIVE_INFINITY;
        for (DecisionTreeLedger.Entry entry : ledger.get().entries()) {
            if (entry.alternative() == chosen.get()) {
                continue;
            }
            Optional<Interval> cost = base.value(entry.alternative());
            if (cost.isPresent() && mid(cost.get()) < nextCost) {
                nextCost = mid(cost.get());
                nextBest = entry.alternative();
            }
        }
        Optional<DecisionTreeLedger.Entry> chosenEntry = ledger.get().entry(chosen.get());
        Optional<DecisionTreeLedger.Entry> otherEntry = nextBest == null ? Optional.empty() : ledger.get().entry(nextBest);
        if (chosenEntry.isEmpty() || otherEntry.isEmpty()) {
            return Optional.empty();
        }
        List<CountTrade> counts = new ArrayList<>();
        for (String unit : ledger.get().units()) {
            counts.add(new CountTrade(unit, chosenEntry.get().count(unit), otherEntry.get().count(unit)));
        }
        return Optional.of(new DecisionTreeTradeoff(chosen.get(), nextBest, chosenEntry.get().money() - otherEntry.get().money(),
                otherEntry.get().health() - chosenEntry.get().health(), base.healthPrice(), List.copyOf(counts)));
    }

    private static double mid(Interval interval) {
        return (interval.lower() + interval.upper()) / 2;
    }
}
