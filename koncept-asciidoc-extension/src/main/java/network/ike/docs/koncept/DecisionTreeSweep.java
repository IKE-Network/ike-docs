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
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The one-way sensitivity of a decision tree read from source: the store-free twin of the live
 * surface's {@code DecisionTreeSensitivity} ({@code komet-claude-plugin}), kept in step with it
 * rule for rule. For each stated measure, and for the price of a healthy year, the tree is rolled
 * back again and again as that one value moves across its range while every other measure holds
 * its base, with the expected cost at the root at every step and every value at which a
 * decision's chosen alternative flips. This is what the tornado draws, bar by bar.
 *
 * <p>A measure stated as an interval swings from its lower to its upper bound, and the swing is
 * <em>stated</em>. A measure stated as a point swings over an assumed range: zero to one for a
 * share, half to double for anything else; a probability under a chance with a residual event may
 * rise only as far as the other events leave. A derived measure is never swept. The base is every
 * measure at its midpoint. A flip is found by bisection between two steps whose chosen alternatives
 * differ, to a ten-thousandth of the range. Measures are known by identity; the price of a healthy
 * year by {@link #HEALTH_PRICE}.
 */
final class DecisionTreeSweep {

    /** The key that stands for the price of a healthy year, which is not a measure of the tree. */
    static final Object HEALTH_PRICE = new Object() {
        @Override
        public String toString() {
            return "health price";
        }
    };

    /** The steps a swing is divided into when no one says otherwise. */
    static final int DEFAULT_STEPS = 12;

    /** One step of a swing: the value, the expected cost at the root, and what every decision chose. */
    record Sample(double value, double root, Map<Node, Node> chosen) {
        Node chosen(Node decision) {
            return chosen.get(decision);
        }
    }

    /** Where a decision's choice changes as the measure rises; a {@code null} alternative is a choice left open. */
    record Flip(Node decision, double at, Node chosenBelow, Node chosenAbove) {
    }

    /** One measure's swing: its range, whether the block stated it, the steps across it, and the flips between them. */
    record Swing(Object key, Interval range, boolean stated, double base, List<Sample> samples, List<Flip> flips) {

        double rootAtLow() {
            return samples.get(0).root();
        }

        double rootAtHigh() {
            return samples.get(samples.size() - 1).root();
        }

        double least() {
            return samples.stream().mapToDouble(Sample::root).min().orElse(Double.NaN);
        }

        double greatest() {
            return samples.stream().mapToDouble(Sample::root).max().orElse(Double.NaN);
        }

        double width() {
            return greatest() - least();
        }

        List<Flip> flipsAt(Node decision) {
            return flips.stream().filter(flip -> flip.decision() == decision).toList();
        }

        /** The expected cost at the root when the measure is a value, read off the steps by straight lines. */
        Optional<Double> rootAtValue(double value) {
            for (int i = 1; i < samples.size(); i++) {
                Sample a = samples.get(i - 1);
                Sample b = samples.get(i);
                if (value >= a.value() - Interval.EPSILON && value <= b.value() + Interval.EPSILON) {
                    if (b.value() == a.value()) {
                        return Optional.of(a.root());
                    }
                    double t = (value - a.value()) / (b.value() - a.value());
                    return Optional.of(a.root() + t * (b.root() - a.root()));
                }
            }
            return Optional.empty();
        }
    }

    /** A measure the sweep could not swing, and why. */
    record Unswept(Object key, String why) {
    }

    private final Node root;
    private final DecisionTreeRollback base;
    private final double baseRoot;
    private final List<Swing> swings;
    private final List<Unswept> unswept;

    private DecisionTreeSweep(Node root, DecisionTreeRollback base, List<Swing> swings, List<Unswept> unswept) {
        this.root = root;
        this.base = base;
        this.baseRoot = base.value(root).map(v -> (v.lower() + v.upper()) / 2).orElse(Double.NaN);
        this.swings = Collections.unmodifiableList(swings);
        this.unswept = Collections.unmodifiableList(unswept);
    }

    /** Sweeps every stated measure of a tree and the price of a healthy year. */
    static DecisionTreeSweep of(Node root, double price, int steps) {
        if (steps < 2) {
            throw new IllegalArgumentException("a sweep takes two or more steps, not " + steps);
        }
        Map<Measure, Interval> baseBounds = new IdentityHashMap<>();
        List<Measure> stated = new ArrayList<>();
        Map<Measure, Node> owners = new IdentityHashMap<>();
        collect(root, stated, owners);
        for (Measure measure : stated) {
            if (!measure.bounds().isPoint()) {
                baseBounds.put(measure, Interval.point((measure.bounds().lower() + measure.bounds().upper()) / 2));
            }
        }
        DecisionTreeRollback base = DecisionTreeRollback.of(root, price, baseBounds);
        List<Swing> swings = new ArrayList<>();
        List<Unswept> unswept = new ArrayList<>();
        List<Node> decisions = new ArrayList<>();
        decisions(root, decisions);
        if (base.value(root).isPresent()) {
            for (Measure measure : stated) {
                Optional<Interval> range = range(root, base, measure, owners.get(measure));
                if (range.isEmpty()) {
                    unswept.add(new Unswept(measure, measure.kind() == MeasureKind.PROBABILITY
                            ? "its chance has no residual event to take up what it gives or takes"
                            : "a value of zero has no assumed range"));
                    continue;
                }
                boolean statedRange = !measure.bounds().isPoint();
                double midpoint = (measure.bounds().lower() + measure.bounds().upper()) / 2;
                Optional<Swing> swing = sweep(measure, range.get(), statedRange, midpoint, value -> {
                    Map<Measure, Interval> moved = new IdentityHashMap<>(baseBounds);
                    moved.put(measure, Interval.point(value));
                    return DecisionTreeRollback.of(root, price, moved);
                }, steps, root, decisions);
                if (swing.isPresent()) {
                    swings.add(swing.get());
                } else {
                    unswept.add(new Unswept(measure, "the tree is not determinable when it moves"));
                }
            }
            if (price > 0) {
                sweep(HEALTH_PRICE, new Interval(price / 2, price * 2, false, false), false, price,
                        other -> DecisionTreeRollback.of(root, other, baseBounds), steps, root, decisions).ifPresent(swings::add);
            }
        }
        swings.sort(Comparator.comparingDouble(Swing::width).reversed());
        return new DecisionTreeSweep(root, base, swings, unswept);
    }

    /** Every stated measure of the tree, in reading order, and the node each sits under. */
    static void collect(Node node, List<Measure> stated, Map<Measure, Node> owners) {
        for (Measure measure : node.measures) {
            owners.put(measure, node);
            if (measure.stated() && !Double.isNaN(measure.bounds().lower())) {
                stated.add(measure);
            }
        }
        for (Node child : node.children) {
            collect(child, stated, owners);
        }
    }

    private static void decisions(Node node, List<Node> decisions) {
        if (node.kind == Kind.DECISION) {
            decisions.add(node);
        }
        for (Node child : node.children) {
            decisions(child, decisions);
        }
    }

    /** Whether a measure is a share: a probability, or a parameter with no measure semantic. */
    static boolean share(Measure measure) {
        return measure.kind() == MeasureKind.PROBABILITY
                || (measure.kind() == MeasureKind.PARAMETER && measure.semantic() == null);
    }

    /** The range a measure swings over: its stated bounds, or the assumed range for a point. */
    static Optional<Interval> range(Node root, DecisionTreeRollback base, Measure measure, Node owner) {
        Interval bounds = measure.bounds();
        if (!bounds.isPoint()) {
            return Optional.of(bounds);
        }
        double value = bounds.lower();
        if (share(measure) && value >= 0 && value <= 1) {
            double ceiling = 1;
            if (measure.kind() == MeasureKind.PROBABILITY) {
                Node chance = parent(root, owner);
                if (chance == null || chance.children.stream().noneMatch(e -> e.kind == Kind.EVENT && e.residual)) {
                    return Optional.empty();
                }
                double others = 0;
                for (Node event : chance.children) {
                    if (event.kind != Kind.EVENT || event == owner) {
                        continue;
                    }
                    Optional<Measure> p = event.probability();
                    if (p.isPresent()) {
                        others += base.measure(p.get()).map(v -> (v.lower() + v.upper()) / 2).orElse(0.0);
                    }
                }
                ceiling = Math.max(value, Math.min(1, 1 - others));
            }
            return Optional.of(new Interval(0, ceiling, false, false));
        }
        if (value == 0) {
            return Optional.empty();
        }
        return Optional.of(value > 0 ? new Interval(value / 2, value * 2, false, false)
                : new Interval(value * 2, value / 2, false, false));
    }

    private static Node parent(Node root, Node child) {
        for (Node candidate : root.children) {
            if (candidate == child) {
                return root;
            }
            Node found = parent(candidate, child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private interface Step {
        DecisionTreeRollback at(double value);
    }

    private static Optional<Swing> sweep(Object key, Interval range, boolean stated, double base, Step step, int steps,
                                         Node root, List<Node> decisions) {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double value = range.lower() + (range.upper() - range.lower()) * i / steps;
            sample(step, value, root, decisions).ifPresent(samples::add);
        }
        if (samples.size() < 2) {
            return Optional.empty();
        }
        List<Flip> flips = new ArrayList<>();
        double tolerance = (range.upper() - range.lower()) * 1e-4;
        for (int i = 1; i < samples.size(); i++) {
            Sample below = samples.get(i - 1);
            Sample above = samples.get(i);
            for (Node decision : decisions) {
                if (below.chosen(decision) != above.chosen(decision)) {
                    flips.add(bisect(step, decision, below, above, tolerance, root, decisions));
                }
            }
        }
        flips.sort(Comparator.comparingDouble(Flip::at));
        for (Flip flip : flips) {
            sample(step, flip.at(), root, decisions).ifPresent(samples::add);
        }
        samples.sort(Comparator.comparingDouble(Sample::value));
        return Optional.of(new Swing(key, range, stated, base, List.copyOf(samples), List.copyOf(flips)));
    }

    private static Flip bisect(Step step, Node decision, Sample below, Sample above, double tolerance, Node root,
                               List<Node> decisions) {
        double lo = below.value();
        double hi = above.value();
        Node chosenBelow = below.chosen(decision);
        Node chosenAbove = above.chosen(decision);
        for (int i = 0; i < 40 && hi - lo > tolerance; i++) {
            double mid = (lo + hi) / 2;
            Optional<Sample> at = sample(step, mid, root, decisions);
            if (at.isEmpty()) {
                break;
            }
            Node chosen = at.get().chosen(decision);
            if (chosen == chosenBelow) {
                lo = mid;
            } else {
                hi = mid;
                chosenAbove = chosen;
            }
        }
        return new Flip(decision, (lo + hi) / 2, chosenBelow, chosenAbove);
    }

    private static Optional<Sample> sample(Step step, double value, Node root, List<Node> decisions) {
        DecisionTreeRollback rollback = step.at(value);
        Optional<Interval> rootValue = rollback.value(root);
        if (rootValue.isEmpty()) {
            return Optional.empty();
        }
        Map<Node, Node> chosen = new IdentityHashMap<>();
        for (Node decision : decisions) {
            rollback.chosen(decision).ifPresent(alternative -> chosen.put(decision, alternative));
        }
        return Optional.of(new Sample(value, (rootValue.get().lower() + rootValue.get().upper()) / 2, chosen));
    }

    Node root() {
        return root;
    }

    /** The tree rolled back with every measure at its base. */
    DecisionTreeRollback base() {
        return base;
    }

    double baseRoot() {
        return baseRoot;
    }

    /** The alternative the root chose at the base, when it decided. */
    Optional<Node> baseChosen() {
        return base.chosen(root);
    }

    /** Every swing, longest first. */
    List<Swing> swings() {
        return swings;
    }

    Optional<Swing> swing(Object key) {
        return swings.stream().filter(swing -> swing.key() == key).findFirst();
    }

    List<Unswept> unswept() {
        return unswept;
    }

    double least() {
        return Math.min(baseRoot, swings.stream().mapToDouble(Swing::least).min().orElse(baseRoot));
    }

    double greatest() {
        return Math.max(baseRoot, swings.stream().mapToDouble(Swing::greatest).max().orElse(baseRoot));
    }
}
