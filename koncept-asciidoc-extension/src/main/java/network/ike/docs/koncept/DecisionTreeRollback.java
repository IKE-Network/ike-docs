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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The rollback of a decision tree read from source, for the documentation build: the same fold
 * the live surface's {@code Rollback} does, from the leaves to the root, health priced into money
 * at the health price given, the expected value at a chance with a residual event taking one
 * minus the others, the least alternative at a decision, every value an interval. Beside the
 * values it records the <em>steps</em>, numbered in the order the computation takes them, so
 * the processor can badge the figure and work every number in a table, as the regulatory topics
 * do by hand. A node whose inputs are missing is not determinable, with a finding saying why.
 */
final class DecisionTreeRollback {

    /** One worked number: its callout, the node it belongs to, what it is, how it is computed, and the result. */
    record Step(int number, Node node, String mark, String what, String how, Interval result) {
    }

    private final double healthPrice;
    private final Map<Node, Interval> values = new IdentityHashMap<>();
    private final Map<Node, Interval> branchCosts = new IdentityHashMap<>();
    private final Map<Node, Interval> eventShares = new IdentityHashMap<>();
    private final Map<Node, Node> chosen = new IdentityHashMap<>();
    private final Map<Node, Step> steps = new IdentityHashMap<>();
    private final List<Step> stepList = new ArrayList<>();
    private final List<String> findings = new ArrayList<>();

    private DecisionTreeRollback(double healthPrice) {
        this.healthPrice = healthPrice;
    }

    static DecisionTreeRollback of(Node root, double healthPrice) {
        DecisionTreeRollback rollback = new DecisionTreeRollback(healthPrice);
        rollback.compute(root);
        return rollback;
    }

    double healthPrice() {
        return healthPrice;
    }

    /** The expected cost still ahead at a node, when determinable. */
    Optional<Interval> value(Node node) {
        return Optional.ofNullable(values.get(node));
    }

    /** What taking a branch itself costs, money with health priced in. */
    Optional<Interval> branchCost(Node branch) {
        return Optional.ofNullable(branchCosts.get(branch));
    }

    /** The share of those at the chance that leave by an event, the residual's derived. */
    Optional<Interval> share(Node event) {
        return Optional.ofNullable(eventShares.get(event));
    }

    /** The alternative a decision takes, when one is no greater than every other across their bounds. */
    Optional<Node> chosen(Node decision) {
        return Optional.ofNullable(chosen.get(decision));
    }

    /** The step that worked a node's number, when it has one. */
    Optional<Step> step(Node node) {
        return Optional.ofNullable(steps.get(node));
    }

    List<Step> steps() {
        return stepList;
    }

    List<String> findings() {
        return findings;
    }

    // ── The fold ─────────────────────────────────────────────────────────────

    private Optional<Interval> compute(Node node) {
        Optional<Interval> value = switch (node.kind) {
            case DECISION -> decide(node);
            case CHANCE -> expect(node);
            case ALTERNATIVE, EVENT -> follow(node);
            case OUTCOME -> leaf(node);
        };
        value.ifPresent(v -> values.put(node, v));
        return value;
    }

    private Optional<Interval> leaf(Node outcome) {
        Optional<Interval> cost = costs(outcome);
        cost.ifPresent(c -> step(outcome, "‡", DecisionTreeFormat.label(outcome),
                costFormula(outcome), c));
        return cost;
    }

    private Optional<Interval> follow(Node branch) {
        Optional<Interval> own = costs(branch);
        if (own.isEmpty()) {
            return Optional.empty();
        }
        branchCosts.put(branch, own.get());
        if (!branch.costs().isEmpty()) {
            step(branch, "†", DecisionTreeFormat.label(branch), costFormula(branch), own.get());
        }
        Optional<Node> next = branch.next();
        if (next.isEmpty()) {
            return own;
        }
        Optional<Interval> ahead = compute(next.get());
        if (ahead.isEmpty()) {
            return notDeterminable(branch, "what follows the branch is not determinable");
        }
        return Optional.of(own.get().plus(ahead.get()));
    }

    private Optional<Interval> expect(Node chance) {
        List<Node> events = chance.children.stream().filter(c -> c.kind == Kind.EVENT).toList();
        if (events.isEmpty()) {
            return notDeterminable(chance, "a chance has no event");
        }
        Node residual = null;
        Interval others = Interval.point(0);
        for (Node event : events) {
            if (event.residual) {
                if (residual != null) {
                    return notDeterminable(chance, "a chance has more than one residual event");
                }
                residual = event;
                continue;
            }
            Optional<Measure> p = event.probability();
            if (p.isEmpty()) {
                return notDeterminable(event, "an event carries no probability and is not residual");
            }
            if (Double.isNaN(p.get().bounds().lower())) {
                return notDeterminable(event, "a probability has no bounds");
            }
            eventShares.put(event, p.get().bounds());
            others = others.plus(p.get().bounds());
        }
        if (residual != null) {
            eventShares.put(residual, others.complement().clip(0, 1));
        } else if (others.lower() > 1 + Interval.EPSILON || others.upper() < 1 - Interval.EPSILON) {
            return notDeterminable(chance, "the events do not close: their probabilities sum to "
                    + DecisionTreeFormat.plain(others) + ", and no event is residual");
        }
        Interval expected = Interval.point(0);
        StringBuilder how = new StringBuilder();
        for (Node event : events) {
            Optional<Interval> ahead = compute(event);
            if (ahead.isEmpty()) {
                return notDeterminable(chance, "an event is not determinable");
            }
            Interval share = eventShares.get(event);
            expected = expected.plus(share.times(ahead.get()));
            if (!how.isEmpty()) {
                how.append(" + ");
            }
            how.append(DecisionTreeFormat.percent(share)).append(" × ").append(DecisionTreeFormat.plain(ahead.get()));
        }
        step(chance, "§", DecisionTreeFormat.label(chance), how.toString(), expected);
        return Optional.of(expected);
    }

    private Optional<Interval> decide(Node decision) {
        List<Node> alternatives = decision.children.stream().filter(c -> c.kind == Kind.ALTERNATIVE).toList();
        if (alternatives.isEmpty()) {
            return notDeterminable(decision, "a decision has no alternative");
        }
        List<Interval> costs = new ArrayList<>();
        StringBuilder how = new StringBuilder();
        for (Node alternative : alternatives) {
            Optional<Interval> cost = compute(alternative);
            if (cost.isEmpty()) {
                return notDeterminable(decision, "an alternative is not determinable");
            }
            costs.add(cost.get());
            if (!how.isEmpty()) {
                how.append("; ");
            }
            how.append(DecisionTreeFormat.label(alternative)).append(": ").append(DecisionTreeFormat.plain(cost.get()));
        }
        Interval least = costs.get(0);
        for (int i = 1; i < costs.size(); i++) {
            least = least.least(costs.get(i));
        }
        for (int i = 0; i < costs.size(); i++) {
            boolean sure = true;
            for (int j = 0; j < costs.size(); j++) {
                if (i != j && !costs.get(i).noGreaterThan(costs.get(j))) {
                    sure = false;
                    break;
                }
            }
            if (sure) {
                chosen.put(decision, alternatives.get(i));
                break;
            }
        }
        how.append(chosen.containsKey(decision)
                ? ". The square takes the cheaper"
                : ". The bounds overlap, so the square takes neither for sure");
        step(decision, "§", DecisionTreeFormat.label(decision), how.toString(), least);
        return Optional.of(least);
    }

    /** The costs a node carries, health priced into money. */
    private Optional<Interval> costs(Node node) {
        Interval total = Interval.point(0);
        for (Measure cost : node.costs()) {
            if (Double.isNaN(cost.bounds().lower())) {
                return notDeterminable(node, "a cost has no bounds");
            }
            if (cost.semantic() == null) {
                return notDeterminable(node, "a cost names no measure semantic");
            }
            total = cost.kind() == MeasureKind.MONEY
                    ? total.plus(cost.bounds())
                    : total.plus(cost.bounds().times(Interval.point(healthPrice)));
        }
        return Optional.of(total);
    }

    private String costFormula(Node node) {
        StringBuilder how = new StringBuilder();
        for (Measure cost : node.costs()) {
            if (!how.isEmpty()) {
                how.append(" + ");
            }
            if (cost.kind() == MeasureKind.MONEY) {
                how.append(DecisionTreeFormat.plain(cost.bounds())).append(" billed");
            } else {
                how.append(DecisionTreeFormat.plain(cost.bounds())).append(" ")
                        .append(cost.semantic() == null ? "health" : cost.semantic().shown())
                        .append(" × ").append(DecisionTreeFormat.plain(Interval.point(healthPrice)));
            }
        }
        return how.isEmpty() ? "nothing on this branch" : how.toString();
    }

    private void step(Node node, String mark, String what, String how, Interval result) {
        Step step = new Step(stepList.size() + 1, node, mark, what, how, result);
        steps.put(node, step);
        stepList.add(step);
    }

    private Optional<Interval> notDeterminable(Node node, String why) {
        findings.add("line " + node.line + ": " + why);
        return Optional.empty();
    }
}
