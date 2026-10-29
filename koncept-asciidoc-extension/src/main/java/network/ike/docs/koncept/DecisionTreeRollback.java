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

import network.ike.docs.koncept.DecisionTreeSource.Concept;
import network.ike.docs.koncept.DecisionTreeSource.Derivation;
import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Kind;
import network.ike.docs.koncept.DecisionTreeSource.Measure;
import network.ike.docs.koncept.DecisionTreeSource.MeasureKind;
import network.ike.docs.koncept.DecisionTreeSource.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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
    record Step(int number, Node node, String mark, String what, String how, Interval result, boolean share) {
    }

    private final double healthPrice;
    private final Map<Node, Interval> values = new IdentityHashMap<>();
    private final Map<Node, Interval> branchCosts = new IdentityHashMap<>();
    private final Map<Node, Interval> eventShares = new IdentityHashMap<>();
    private final Map<Node, Node> chosen = new IdentityHashMap<>();
    private final Map<Node, Step> steps = new IdentityHashMap<>();
    private final List<Step> stepList = new ArrayList<>();
    private final List<String> findings = new ArrayList<>();
    /** The parameters by the key of the quantity they measure: its identifier, else its label. */
    private final Map<String, Measure> parameters = new HashMap<>();
    private final Map<Measure, Optional<Interval>> measureValues = new IdentityHashMap<>();
    /** The event a probability sits on, so a derived share is worked under the event's name. */
    private final Map<Measure, String> owners = new IdentityHashMap<>();
    private final List<Measure> evaluating = new ArrayList<>();
    /** Stated measures read at other bounds than the block states, by identity: what a sweep moves. */
    private final Map<Measure, Interval> overrides = new IdentityHashMap<>();

    private DecisionTreeRollback(double healthPrice) {
        this.healthPrice = healthPrice;
    }

    static DecisionTreeRollback of(Node root, double healthPrice) {
        return of(root, healthPrice, Map.of());
    }

    /**
     * Rolls the tree back with some stated measures at other bounds, by identity, every other
     * measure as the block states it: one step of a one-way sweep.
     */
    static DecisionTreeRollback of(Node root, double healthPrice, Map<Measure, Interval> overrides) {
        DecisionTreeRollback rollback = new DecisionTreeRollback(healthPrice);
        rollback.overrides.putAll(overrides);
        for (Measure parameter : root.parameters()) {
            if (parameter.quantity() != null) {
                String key = quantityKey(parameter.quantity());
                if (rollback.parameters.containsKey(key)) {
                    rollback.findings.add("line " + parameter.line() + ": a quantity has two parameters");
                } else {
                    rollback.parameters.put(key, parameter);
                }
            }
        }
        // Parameters are worked first, in their stated order, so the table opens with the assumptions.
        for (Measure parameter : root.parameters()) {
            rollback.measure(parameter);
        }
        rollback.compute(root);
        return rollback;
    }

    /** The key a quantity is referred to by: its identifier when it has one, else its label. */
    static String quantityKey(Concept quantity) {
        return quantity.identifier() != null ? quantity.identifier() : quantity.shown();
    }

    /** The parameters in stated order, for a table of assumptions. */
    List<Measure> parameters(Node root) {
        return root.parameters();
    }

    /** The value of a measure: stated, or derived from the parameters it reads. */
    Optional<Interval> measure(Measure measure) {
        if (measureValues.containsKey(measure)) {
            return measureValues.get(measure);
        }
        Optional<Interval> value = evaluate(measure);
        measureValues.put(measure, value);
        return value;
    }

    private Optional<Interval> evaluate(Measure measure) {
        if (measure.stated()) {
            if (overrides.containsKey(measure)) {
                return Optional.of(overrides.get(measure));
            }
            if (Double.isNaN(measure.bounds().lower())) {
                return notDeterminableLine(measure.line(), "a measure has no bounds");
            }
            return Optional.of(measure.bounds());
        }
        if (!measure.derived() || measure.derivation() == null) {
            return notDeterminableLine(measure.line(), "a derivation names no kind the build can run");
        }
        if (evaluating.contains(measure)) {
            return notDeterminableLine(measure.line(), "a derivation reads itself");
        }
        evaluating.add(measure);
        try {
            List<Interval> values = new ArrayList<>();
            StringBuilder how = new StringBuilder();
            for (Concept operand : measure.operands()) {
                Measure parameter = parameters.get(quantityKey(operand));
                if (parameter == null) {
                    return notDeterminableLine(measure.line(), "a derivation reads a quantity the tree has no parameter for: "
                            + operand.shown());
                }
                Optional<Interval> value = measure(parameter);
                if (value.isEmpty()) {
                    return notDeterminableLine(measure.line(), "an operand of a derivation is not determinable: " + operand.shown());
                }
                values.add(value.get());
                if (!how.isEmpty()) {
                    how.append(", ");
                }
                how.append(parameter.name()).append(" ").append(DecisionTreeFormat.plain(value.get()));
            }
            Interval result = derive(measure.derivation(), values);
            if (measure.kind() == MeasureKind.PARAMETER || measure.kind() == MeasureKind.PROBABILITY) {
                String what = measure.kind() == MeasureKind.PROBABILITY
                        ? "% " + owners.getOrDefault(measure, "probability") : measure.name();
                step(measure, "", what, measure.derivation().label + " of " + how, result,
                        measure.semantic() == null);
            }
            return Optional.of(result);
        } finally {
            evaluating.remove(measure);
        }
    }

    /** The kinds, over bounds. */
    static Interval derive(Derivation kind, List<Interval> v) {
        return switch (kind) {
            case COMPLEMENT -> v.get(0).complement();
            case PRODUCT -> {
                Interval product = v.get(0);
                for (int i = 1; i < v.size(); i++) {
                    product = product.times(v.get(i));
                }
                yield product;
            }
            case CUMULATIVE_HAZARD -> {
                double lo = 1 - Math.pow(1 - v.get(0).lower(), v.get(1).lower());
                double hi = 1 - Math.pow(1 - v.get(0).upper(), v.get(1).upper());
                yield new Interval(Math.min(lo, hi), Math.max(lo, hi),
                        v.get(0).lowerOpen() || v.get(1).lowerOpen(), v.get(0).upperOpen() || v.get(1).upperOpen());
            }
            case BAYES_POSTERIOR -> v.get(0).times(v.get(1))
                    .dividedBy(v.get(0).times(v.get(1)).plus(v.get(0).complement().times(v.get(2)))).clip(0, 1);
            case BAYES_MARGINAL -> v.get(0).times(v.get(1)).plus(v.get(0).complement().times(v.get(2))).clip(0, 1);
        };
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
            owners.put(p.get(), DecisionTreeFormat.label(event));
            Optional<Interval> share = measure(p.get());
            if (share.isEmpty()) {
                return notDeterminable(event, "a probability is not determinable");
            }
            eventShares.put(event, share.get());
            others = others.plus(share.get());
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
            Optional<Interval> value = measure(cost);
            if (value.isEmpty()) {
                return notDeterminable(node, "a cost is not determinable");
            }
            if (cost.semantic() == null) {
                return notDeterminable(node, "a cost names no measure semantic");
            }
            total = cost.kind() == MeasureKind.MONEY
                    ? total.plus(value.get())
                    : total.plus(value.get().times(Interval.point(healthPrice)));
        }
        return Optional.of(total);
    }

    private String costFormula(Node node) {
        StringBuilder how = new StringBuilder();
        for (Measure cost : node.costs()) {
            if (!how.isEmpty()) {
                how.append(" + ");
            }
            String amount = measure(cost).map(DecisionTreeFormat::plain).orElse("?");
            if (cost.derived() && cost.derivation() != null) {
                amount = cost.derivation().label.toLowerCase(Locale.ROOT) + " of "
                        + String.join(", ", cost.operands().stream().map(Concept::shown).toList()) + " = " + amount;
            }
            if (cost.kind() == MeasureKind.MONEY) {
                how.append(amount).append(" billed");
            } else {
                how.append(amount).append(" ")
                        .append(cost.semantic() == null ? "health" : cost.semantic().shown())
                        .append(" × ").append(DecisionTreeFormat.plain(Interval.point(healthPrice)));
            }
        }
        return how.isEmpty() ? "nothing on this branch" : how.toString();
    }

    private void step(Node node, String mark, String what, String how, Interval result) {
        Step step = new Step(stepList.size() + 1, node, mark, what, how, result, false);
        steps.put(node, step);
        stepList.add(step);
    }

    private void step(Measure measure, String mark, String what, String how, Interval result, boolean share) {
        stepList.add(new Step(stepList.size() + 1, null, mark, what, how, result, share));
    }

    private Optional<Interval> notDeterminableLine(int line, String why) {
        findings.add("line " + line + ": " + why);
        return Optional.empty();
    }

    private Optional<Interval> notDeterminable(Node node, String why) {
        findings.add("line " + node.line + ": " + why);
        return Optional.empty();
    }
}
