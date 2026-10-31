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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import network.ike.docs.koncept.CalculatorSource.Band;
import network.ike.docs.koncept.CalculatorSource.Binding;
import network.ike.docs.koncept.CalculatorSource.Calculation;
import network.ike.docs.koncept.CalculatorSource.Calculator;
import network.ike.docs.koncept.CalculatorSource.Clarify;
import network.ike.docs.koncept.CalculatorSource.Coefficient;
import network.ike.docs.koncept.CalculatorSource.Condition;
import network.ike.docs.koncept.CalculatorSource.Conversion;
import network.ike.docs.koncept.CalculatorSource.Inline;
import network.ike.docs.koncept.CalculatorSource.InlineConcept;
import network.ike.docs.koncept.CalculatorSource.Input;
import network.ike.docs.koncept.CalculatorSource.Kind;
import network.ike.docs.koncept.CalculatorSource.Missing;
import network.ike.docs.koncept.CalculatorSource.Statement;
import network.ike.docs.koncept.CalculatorSource.Stratum;
import network.ike.docs.koncept.CalculatorSource.Term;
import network.ike.docs.koncept.DecisionTreeSource.Concept;
import network.ike.docs.koncept.DecisionTreeSource.Interval;

/**
 * One evaluation of a calculator against a calculation's bindings, store-free, the same fold as the
 * Komet assistant plugin's {@code Evaluation} ({@code dev-calculator-blocks}): each input resolved
 * to the bounds the equation reads or left not determinable, the stratum selected by the
 * concept-valued inputs, and the result computed pointwise at every corner of the inputs' bounds,
 * since a published risk equation is monotone in each input, so a bounded input gives honest
 * bounds without the dependency problem of interval arithmetic. The one-way sweep beneath it
 * ranks the inputs by how far each moves the result.
 */
final class CalculatorEvaluation {

    enum Status { OBSERVED, SUPPLIED, ASSUMED, PLAUSIBLE, NOT_DETERMINED, NOT_ADMITTED, OUT_OF_DOMAIN }

    record InputState(Input input, Status status, Interval bounds, Concept conceptValue, String note) {
        boolean standsIn() {
            return status == Status.ASSUMED || status == Status.PLAUSIBLE;
        }

        boolean interval() {
            return bounds != null && !bounds.isPoint();
        }

        String statusWord() {
            return switch (status) {
                case OBSERVED -> "observed";
                case SUPPLIED -> "stated";
                case ASSUMED -> "assumed";
                case PLAUSIBLE -> "plausible range";
                case NOT_DETERMINED -> "not determined";
                case NOT_ADMITTED -> "not admitted";
                case OUT_OF_DOMAIN -> "outside the domain";
            };
        }
    }

    /** One row of the sweep: the input's swing, the result at each end, and whether the swing is the input's own interval. */
    record Row(Input input, Interval swing, Interval low, Interval high, boolean solid) {
        double width() {
            return Math.abs(mid(high) - mid(low));
        }

        Interval span() {
            return new Interval(Math.min(low.lower(), high.lower()), Math.max(low.upper(), high.upper()), false, false);
        }
    }

    static final int MAX_CORNER_INPUTS = 12;

    private final Calculator calculator;
    private final Map<String, Binding> bindings;
    private final Map<String, Inline> assumptions;
    private final List<String> findings = new ArrayList<>();
    private final List<InputState> states = new ArrayList<>();
    private final Map<String, InputState> byKey = new HashMap<>();
    private Stratum stratum;
    private Interval share;
    private Interval result;
    private final List<Band> bands = new ArrayList<>();

    private CalculatorEvaluation(Calculator calculator, Map<String, Binding> bindings, Map<String, Inline> assumptions) {
        this.calculator = calculator;
        this.bindings = bindings;
        this.assumptions = assumptions;
        run();
    }

    static CalculatorEvaluation of(Calculator calculator, Calculation calculation) {
        return new CalculatorEvaluation(calculator, calculation.bindings(), calculation.assumptions());
    }

    static CalculatorEvaluation of(Calculator calculator, Map<String, Binding> bindings, Map<String, Inline> assumptions) {
        return new CalculatorEvaluation(calculator, bindings, assumptions);
    }

    Calculator calculator() {
        return calculator;
    }

    List<InputState> inputs() {
        return states;
    }

    Optional<InputState> input(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    Optional<Stratum> stratum() {
        return Optional.ofNullable(stratum);
    }

    /** The result as the formula gives it, a share, before the output's scale. */
    Optional<Interval> share() {
        return Optional.ofNullable(share);
    }

    /** The result at the output's scale. */
    Optional<Interval> result() {
        return Optional.ofNullable(result);
    }

    List<Band> bands() {
        return bands;
    }

    List<String> findings() {
        return findings;
    }

    boolean determinable() {
        return result != null;
    }

    /** Whether an input stood in for a statement, assumed or swept; a value stated inline is the author's own. */
    boolean interim() {
        return states.stream().anyMatch(InputState::standsIn);
    }

    static double mid(Interval interval) {
        return (interval.lower() + interval.upper()) / 2;
    }

    // ── The run ──────────────────────────────────────────────────────────────

    private void run() {
        boolean determinable = true;
        for (Input input : calculator.inputs()) {
            InputState state = resolve(input);
            states.add(state);
            byKey.put(input.key(), state);
            if (state.bounds() == null && state.conceptValue() == null) {
                determinable = false;
            }
        }
        stratum = selectStratum();
        if (calculator.formula() == null) {
            findings.add("a calculator has no formula");
            return;
        }
        if (!determinable || stratum == null) {
            return;
        }
        Interval computed = sweep();
        if (computed == null) {
            return;
        }
        share = computed;
        double scale = calculator.output() == null ? 1 : calculator.output().scale();
        result = new Interval(computed.lower() * scale, computed.upper() * scale, computed.lowerOpen(), computed.upperOpen());
        if (calculator.output() != null && calculator.output().domain() != null) {
            Interval domain = calculator.output().domain();
            if (result.lower() < domain.lower() - Interval.EPSILON || result.upper() > domain.upper() + Interval.EPSILON) {
                findings.add("the result lies outside the output's domain: " + DecisionTreeFormat.plain(result));
            }
        }
        for (Band band : calculator.bands()) {
            Interval i = band.interval();
            boolean touches = i.upper() >= result.lower() - Interval.EPSILON && i.lower() <= result.upper() + Interval.EPSILON
                    && !(i.upperOpen() && i.upper() <= result.lower() + (result.isPoint() ? 0 : Interval.EPSILON));
            if (touches) {
                bands.add(band);
            }
        }
    }

    private InputState resolve(Input input) {
        String key = input.key();
        Inline assumption = assumptions.get(key);
        if (assumption != null) {
            return measureState(input, assumption.bounds(), assumption.semantic(), Status.ASSUMED);
        }
        Binding binding = bindings.get(key);
        if (binding == null) {
            return missing(input, "no statement is bound");
        }
        return switch (binding) {
            case Inline inline -> input.conceptValued()
                    ? notAdmitted(input, "bound to a measure where a concept is expected")
                    : measureState(input, inline.bounds(), inline.semantic(), Status.SUPPLIED);
            case InlineConcept concept -> conceptState(input, concept.value(), Status.SUPPLIED);
            case Missing missing -> missing(input, "no statement is bound");
            case Clarify clarify -> missing(input, "left to clarify: " + clarify.question());
            case Statement statement -> missing(input, "the statement " + statement.identifier()
                    + " cannot be read by the documentation build");
        };
    }

    private InputState notAdmitted(Input input, String why) {
        findings.add(input.name() + ": " + why);
        return new InputState(input, Status.NOT_ADMITTED, null, null, why);
    }

    private InputState conceptState(Input input, Concept value, Status status) {
        if (!input.conceptValued()) {
            return notAdmitted(input, "bound to a concept where a measure is expected");
        }
        String key = DecisionTreeRollback.quantityKey(value);
        boolean admitted = input.values().stream().anyMatch(v -> DecisionTreeRollback.quantityKey(v).equals(key)
                || (v.identifier() != null && v.identifier().equals(value.identifier())));
        if (!admitted) {
            return notAdmitted(input, "the bound concept " + value.shown() + " is not one the input admits");
        }
        return new InputState(input, status, null, value, null);
    }

    private InputState measureState(Input input, Interval bounds, Concept semantic, Status status) {
        Interval converted = bounds;
        String note = null;
        String expected = input.semantic() == null ? null : DecisionTreeRollback.quantityKey(input.semantic());
        String given = semantic == null ? null : DecisionTreeRollback.quantityKey(semantic);
        if (given != null && expected != null && !given.equals(expected)) {
            Optional<Conversion> conversion = input.conversions().stream()
                    .filter(c -> DecisionTreeRollback.quantityKey(c.fromSemantic()).equals(given)).findFirst();
            if (conversion.isEmpty()) {
                return notAdmitted(input, "bound to a value on " + semantic.shown() + ", a measure semantic it does not admit");
            }
            converted = bounds.times(conversion.get().factor());
            note = "converted by " + DecisionTreeFormat.plain(conversion.get().factor());
        }
        Status finalStatus = status;
        Interval domain = input.domain();
        if (domain != null && (converted.lower() < domain.lower() - Interval.EPSILON || converted.upper() > domain.upper() + Interval.EPSILON)) {
            findings.add(input.name() + ": the value " + DecisionTreeFormat.plain(converted) + " lies outside the domain the equation is valid over, "
                    + DecisionTreeFormat.plain(domain));
            finalStatus = Status.OUT_OF_DOMAIN;
            note = "outside the equation's domain " + DecisionTreeFormat.plain(domain);
        }
        return new InputState(input, finalStatus, converted, null, note);
    }

    private InputState missing(Input input, String why) {
        String policy = input.whenMissing();
        if ("plausible".equals(policy) && input.missingBounds() != null && !input.conceptValued()) {
            return new InputState(input, Status.PLAUSIBLE, input.missingBounds(), null, "no statement; swept over the plausible range");
        }
        if ("assumed".equals(policy)) {
            if (input.conceptValued() && input.missingConcept() != null) {
                return new InputState(input, Status.ASSUMED, null, input.missingConcept(), "no statement; assumed by the calculator's policy");
            }
            if (!input.conceptValued() && input.missingBounds() != null) {
                return new InputState(input, Status.ASSUMED, input.missingBounds(), null, "no statement; assumed by the calculator's policy");
            }
        }
        findings.add(input.name() + ": not determinable, " + why);
        return new InputState(input, Status.NOT_DETERMINED, null, null, why);
    }

    private Stratum selectStratum() {
        if (calculator.strata().isEmpty()) {
            findings.add("a calculator has no stratum");
            return null;
        }
        List<Stratum> matching = new ArrayList<>();
        boolean undecidable = false;
        for (Stratum candidate : calculator.strata()) {
            boolean holds = true;
            for (Condition condition : candidate.conditions()) {
                InputState state = byKey.get(DecisionTreeRollback.quantityKey(condition.input()));
                if (state == null || state.conceptValue() == null) {
                    undecidable = true;
                    holds = false;
                    break;
                }
                String required = DecisionTreeRollback.quantityKey(condition.value());
                String actual = DecisionTreeRollback.quantityKey(state.conceptValue());
                if (!required.equals(actual)) {
                    holds = false;
                    break;
                }
            }
            if (holds) {
                matching.add(candidate);
            }
        }
        if (matching.size() == 1) {
            return matching.get(0);
        }
        findings.add(matching.isEmpty()
                ? (undecidable ? "no stratum can be selected while an input its conditions test is not determined"
                        : "no stratum's conditions hold for the bound values")
                : matching.size() + " strata's conditions hold; a calculator selects one");
        return null;
    }

    // ── The sweep over the corners ───────────────────────────────────────────

    private Interval sweep() {
        List<InputState> numeric = states.stream().filter(s -> !s.input().conceptValued() && s.bounds() != null).toList();
        List<InputState> intervals = new ArrayList<>(numeric.stream().filter(InputState::interval).toList());
        intervals.sort((a, b) -> Double.compare(b.bounds().upper() - b.bounds().lower(), a.bounds().upper() - a.bounds().lower()));
        if (intervals.size() > MAX_CORNER_INPUTS) {
            findings.add(intervals.size() + " inputs carry intervals; the sweep takes the corners of the " + MAX_CORNER_INPUTS
                    + " widest and the midpoints of the rest, so the bounds may understate");
            intervals = intervals.subList(0, MAX_CORNER_INPUTS);
        }
        Map<String, Double> point = new HashMap<>();
        for (InputState state : numeric) {
            point.put(state.input().key(), mid(state.bounds()));
        }
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        boolean lowerOpen = false;
        boolean upperOpen = false;
        int corners = 1 << intervals.size();
        Pointwise pointwise = new Pointwise();
        for (int corner = -1; corner < corners; corner++) {
            boolean anyOpen = false;
            for (int i = 0; i < intervals.size(); i++) {
                Interval b = intervals.get(i).bounds();
                boolean high = corner >= 0 && (corner & (1 << i)) != 0;
                double value = corner < 0 ? mid(b) : high ? b.upper() : b.lower();
                anyOpen |= corner >= 0 && (high ? b.upperOpen() : b.lowerOpen());
                point.put(intervals.get(i).input().key(), value);
            }
            Optional<Double> value = pointwise.at(point);
            if (value.isEmpty()) {
                return null;
            }
            double v = value.get();
            if (v < lo) {
                lo = v;
                lowerOpen = anyOpen;
            }
            if (v > hi) {
                hi = v;
                upperOpen = anyOpen;
            }
        }
        if (Double.isNaN(lo) || Double.isInfinite(lo) || Double.isNaN(hi) || Double.isInfinite(hi)) {
            findings.add("the formula gave no number");
            return null;
        }
        return new Interval(lo, hi, lowerOpen, upperOpen);
    }

    /** The equation at one point of the inputs: the terms, the linear predictor, the formula. */
    private final class Pointwise {
        private final Map<String, Double> constants = new HashMap<>();
        private final List<Coefficient> termCoefficients = new ArrayList<>();
        private boolean reported;

        Pointwise() {
            List<String> operands = calculator.formula().operands().stream().map(DecisionTreeRollback::quantityKey).toList();
            for (Coefficient coefficient : stratum.coefficients()) {
                if (operands.contains(coefficient.key())) {
                    constants.put(coefficient.key(), mid(coefficient.value()));
                } else {
                    termCoefficients.add(coefficient);
                }
            }
        }

        Optional<Double> at(Map<String, Double> point) {
            double predictor = 0;
            for (Coefficient coefficient : termCoefficients) {
                Optional<Double> value = quantity(coefficient.key(), point, new ArrayList<>());
                if (value.isEmpty()) {
                    report("line " + coefficient.line() + ": the term of a coefficient is not determinable: " + coefficient.quantity().shown());
                    return Optional.empty();
                }
                predictor += mid(coefficient.value()) * value.get();
            }
            if (calculator.formula().kind() == Kind.COX_SURVIVAL) {
                List<String> operands = calculator.formula().operands().stream().map(DecisionTreeRollback::quantityKey).toList();
                if (operands.size() != 2 || !constants.containsKey(operands.get(0)) || !constants.containsKey(operands.get(1))) {
                    report("line " + calculator.formula().line() + ": a Cox survival formula reads the stratum's baseline survival and mean,"
                            + " which the stratum does not state");
                    return Optional.empty();
                }
                return Optional.of(1 - Math.pow(constants.get(operands.get(0)), Math.exp(predictor - constants.get(operands.get(1)))));
            }
            report("a formula names no kind the build can run");
            return Optional.empty();
        }

        private Optional<Double> quantity(String key, Map<String, Double> point, List<String> evaluating) {
            if (point.containsKey(key)) {
                return Optional.of(point.get(key));
            }
            Optional<Term> term = calculator.term(key);
            if (term.isEmpty()) {
                return Optional.empty();
            }
            if (evaluating.contains(key)) {
                report("line " + term.get().line() + ": a term reads itself");
                return Optional.empty();
            }
            evaluating.add(key);
            try {
                Term t = term.get();
                if (t.kind() == null) {
                    return quantity(DecisionTreeRollback.quantityKey(t.reads()), point, evaluating);
                }
                List<Double> values = new ArrayList<>();
                for (Concept operand : t.operands()) {
                    Optional<Double> value = quantity(DecisionTreeRollback.quantityKey(operand), point, evaluating);
                    if (value.isEmpty()) {
                        return Optional.empty();
                    }
                    values.add(value.get());
                }
                return switch (t.kind()) {
                    case NATURAL_LOGARITHM -> values.size() == 1 && values.get(0) > 0 ? Optional.of(Math.log(values.get(0))) : Optional.empty();
                    case COMPLEMENT -> values.size() == 1 ? Optional.of(1 - values.get(0)) : Optional.empty();
                    case PRODUCT -> {
                        double product = 1;
                        for (double v : values) {
                            product *= v;
                        }
                        yield values.size() >= 2 ? Optional.of(product) : Optional.empty();
                    }
                    case COX_SURVIVAL -> Optional.empty();
                };
            } finally {
                evaluating.remove(key);
            }
        }

        private void report(String message) {
            if (!reported) {
                findings.add(message);
                reported = true;
            }
        }
    }

    // ── The sweep ────────────────────────────────────────────────────────────

    /** The one-way sweep of the numeric inputs, widest swing first; empty when not determinable. */
    List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (!determinable()) {
            return rows;
        }
        Map<String, Inline> midpoints = new LinkedHashMap<>(assumptions);
        for (InputState state : states) {
            if (!state.input().conceptValued() && state.bounds() != null) {
                midpoints.put(state.input().key(), new Inline(Interval.point(mid(state.bounds())), state.input().semantic()));
            }
        }
        for (InputState state : states) {
            if (state.input().conceptValued() || state.bounds() == null) {
                continue;
            }
            Interval own = state.bounds();
            boolean solid = !own.isPoint();
            Interval swing = solid ? own : assumedSwing(state.input(), own);
            if (swing.isPoint()) {
                continue;
            }
            Optional<Interval> low = at(midpoints, state.input(), swing.lower());
            Optional<Interval> high = at(midpoints, state.input(), swing.upper());
            if (low.isEmpty() || high.isEmpty()) {
                continue;
            }
            rows.add(new Row(state.input(), swing, low.get(), high.get(), solid));
        }
        rows.sort((a, b) -> Double.compare(b.width(), a.width()));
        return rows;
    }

    /** The result with every input at its midpoint, the hairline the bars hang from. */
    Optional<Interval> midpoint() {
        Map<String, Inline> midpoints = new LinkedHashMap<>(assumptions);
        for (InputState state : states) {
            if (!state.input().conceptValued() && state.bounds() != null) {
                midpoints.put(state.input().key(), new Inline(Interval.point(mid(state.bounds())), state.input().semantic()));
            }
        }
        return new CalculatorEvaluation(calculator, bindings, midpoints).result();
    }

    private Optional<Interval> at(Map<String, Inline> midpoints, Input input, double value) {
        Map<String, Inline> moved = new LinkedHashMap<>(midpoints);
        moved.put(input.key(), new Inline(Interval.point(value), input.semantic()));
        return new CalculatorEvaluation(calculator, bindings, moved).result();
    }

    /** A point reading's swing: the declared plausible range; a presence's zero to one; else a tenth either way within the domain. */
    static Interval assumedSwing(Input input, Interval point) {
        if (input.missingBounds() != null && !input.missingBounds().isPoint()) {
            return input.missingBounds();
        }
        Interval domain = input.domain();
        double value = mid(point);
        if (domain != null && domain.lower() == 0 && domain.upper() == 1) {
            return domain;
        }
        if (value == 0) {
            return new Interval(0, 1, false, false);
        }
        double low = value - Math.abs(value) / 10;
        double high = value + Math.abs(value) / 10;
        if (domain != null && Double.isFinite(domain.lower()) && Double.isFinite(domain.upper())) {
            low = Math.max(low, domain.lower());
            high = Math.min(high, domain.upper());
        }
        return low < high ? new Interval(low, high, false, false) : point;
    }

    /** The disposition a recording would carry. */
    String disposition() {
        return !determinable() ? "nothing to record" : interim() ? "in progress, interim result" : "performed";
    }

    /** A result at the output's scale as the build prints it. */
    String shown(Interval value) {
        String unit = calculator.output() == null ? "" : calculator.output().scale() == 100 ? "%"
                : calculator.output().semantic() == null ? "" : calculator.output().semantic().shown();
        String text = value.isPoint() ? figure(value.lower()) : figure(value.lower()) + "–" + figure(value.upper());
        return unit.isEmpty() ? text : text + " " + unit;
    }

    static String figure(double value) {
        double magnitude = Math.abs(value);
        if (magnitude >= 100) {
            return String.format(Locale.ROOT, "%,.0f", value);
        }
        if (magnitude >= 10) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        if (magnitude >= 1) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
