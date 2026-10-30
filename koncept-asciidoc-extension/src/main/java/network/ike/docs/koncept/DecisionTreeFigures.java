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

import network.ike.docs.koncept.DecisionFigure.Circle;
import network.ike.docs.koncept.DecisionFigure.Mark;
import network.ike.docs.koncept.DecisionFigure.Polygon;
import network.ike.docs.koncept.DecisionFigure.Polyline;
import network.ike.docs.koncept.DecisionFigure.Rect;
import network.ike.docs.koncept.DecisionFigure.Stroke;
import network.ike.docs.koncept.DecisionFigure.Text;
import network.ike.docs.koncept.DecisionFigureTornado.Bar;
import network.ike.docs.koncept.DecisionFigureTornado.Choice;
import network.ike.docs.koncept.DecisionFigureTornado.Segment;
import network.ike.docs.koncept.DecisionFigureTornado.Tick;
import network.ike.docs.koncept.DecisionFigureTornado.Tornado;
import network.ike.docs.koncept.DecisionTreeGeometry.Kind;
import network.ike.docs.koncept.DecisionTreeGeometry.Placed;
import network.ike.docs.koncept.DecisionTreeGeometry.Run;
import network.ike.docs.koncept.DecisionTreeRollback.Step;
import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Measure;
import network.ike.docs.koncept.DecisionTreeSource.MeasureKind;
import network.ike.docs.koncept.DecisionTreeSource.Node;
import network.ike.docs.koncept.DecisionTreeSweep.Flip;
import network.ike.docs.koncept.DecisionTreeSweep.Swing;
import network.ike.docs.koncept.DecisionTreeSweep.Unswept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The figures of a decision tree read from source, for the documentation build: the tree on the
 * {@link DecisionTreeGeometry}, drawn as Komet's live diagram draws it, and the tornado from a
 * {@link DecisionTreeSweep}, read into the shared {@link DecisionFigureTornado} builder. The
 * store-free twin of the live surface's {@code DecisionTreeFigures}
 * ({@code komet-claude-plugin}), convention for convention: squares, circles, triangles; every
 * branch an elbow with its name and probability above the run and its costs below; the
 * rolled-back value at every glyph and on every alternative; the chosen alternative heavy and the
 * rejected ones barred. One thing the printed figure carries that the live one does not: the
 * orange callout of the worked table's step at every node and costed branch, so the table
 * beneath can work each number under the same callout.
 */
final class DecisionTreeFigures {

    private DecisionTreeFigures() {
    }

    static final double BASE = 12;
    private static final double BAR_ROOM = 16;
    private static final double SPAN_GAP = 5;
    /** The callout's colour: the topics' figure-callout orange. */
    static final String CALLOUT = "#eb6834";

    /** One piece of a label row; a callout span draws as a badge with its number. */
    record Span(String text, double size, boolean bold, boolean italic, String colour, String role) {
        boolean callout() {
            return "callout".equals(role);
        }
    }

    // ── The tree ─────────────────────────────────────────────────────────────

    /**
     * The tree as a figure.
     *
     * @param root     the tree
     * @param rollback its rollback, with the steps that callout each number
     * @param symbol   what goes before a rolled-back value
     * @param sweep    the sweep, when one ran: a measure whose range holds a flip of the root's choice is marked
     */
    static DecisionFigure tree(Node root, DecisionTreeRollback rollback, String symbol, Optional<DecisionTreeSweep> sweep) {
        return new TreeBuilder(root, rollback, symbol, sweep).build();
    }

    private static final class TreeBuilder {
        private final Node root;
        private final DecisionTreeRollback rollback;
        private final String symbol;
        private final Set<Measure> flipping = Collections.newSetFromMap(new IdentityHashMap<>());
        private final double lineHeight = DecisionFigureText.lineHeight(BASE);
        private final double glyph = Math.round(BASE * 1.1);
        private final double size = BASE * 0.9;

        private final Map<Node, List<Span>> nodeTop = new IdentityHashMap<>();
        private final Map<Node, List<Span>> nodeBottom = new IdentityHashMap<>();
        private final Map<Node, List<Span>> branchAbove = new IdentityHashMap<>();
        private final Map<Node, List<Span>> branchBelow = new IdentityHashMap<>();
        private final Set<Node> barred = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Node> chosenBranches = Collections.newSetFromMap(new IdentityHashMap<>());

        TreeBuilder(Node root, DecisionTreeRollback rollback, String symbol, Optional<DecisionTreeSweep> sweep) {
            this.root = root;
            this.rollback = rollback;
            this.symbol = symbol == null ? "" : symbol;
            sweep.ifPresent(s -> {
                for (Swing swing : s.swings()) {
                    if (swing.key() instanceof Measure measure && !swing.flipsAt(s.root()).isEmpty()) {
                        flipping.add(measure);
                    }
                }
            });
        }

        DecisionFigure build() {
            if (root == null) {
                return new DecisionFigure(2 * 6, 2 * 6, "decision-tree", List.of());
            }
            node(root);
            DecisionTreeGeometry layout = DecisionTreeGeometry.of(root, sizing(), node -> false);
            List<Mark> marks = new ArrayList<>();
            for (Run run : layout.runs()) {
                Node branch = run.branch();
                boolean chosen = chosenBranches.contains(branch);
                boolean gap = rollback.value(branch).isEmpty();
                Stroke stroke = gap ? Stroke.dashed(DecisionFigureText.GAP, 1.2)
                        : Stroke.solid(chosen ? DecisionFigureText.CHOSEN : DecisionFigureText.INK, chosen ? 2.6 : 1.2);
                marks.add(new Polyline(new double[] {run.x0(), run.y0(), run.xBend(), run.y0(), run.xBend(), run.y(),
                        run.x1(), run.y()}, stroke, "run" + (chosen ? " chosen" : "") + (gap ? " gap" : "")));
                boolean isBarred = barred.contains(branch);
                double labelX = run.xBend() + 8 + (isBarred ? BAR_ROOM : 0);
                row(marks, branchAbove.get(branch), labelX, run.y() - lineHeight - 2);
                row(marks, branchBelow.get(branch), labelX, run.y() + 3);
                if (isBarred) {
                    double bx = run.xBend() + 9;
                    marks.add(new Polyline(new double[] {bx, run.y() - 6, bx, run.y() + 6},
                            Stroke.solid(DecisionFigureText.INK, 1.6), "barred"));
                    marks.add(new Polyline(new double[] {bx + 5, run.y() - 6, bx + 5, run.y() + 6},
                            Stroke.solid(DecisionFigureText.INK, 1.6), "barred"));
                }
            }
            for (Placed placed : layout.placed()) {
                marks.add(glyph(placed));
                boolean terminal = placed.kind() == Kind.OUTCOME || placed.kind() == Kind.END || placed.collapsed();
                double textLeft = placed.x() + glyph / 2 + 4;
                row(marks, nodeTop.get(placed.node()), textLeft, placed.y() - lineHeight);
                row(marks, nodeBottom.get(placed.node()), textLeft, placed.y() + (terminal ? 2 : 1));
            }
            return new DecisionFigure(layout.width(), layout.height(), "decision-tree", marks);
        }

        private Mark glyph(Placed placed) {
            boolean flagged = rollback.value(placed.node()).isEmpty();
            Stroke stroke = flagged ? Stroke.dashed(DecisionFigureText.GAP, 1.4) : Stroke.solid(DecisionFigureText.INK, 1.4);
            double left = placed.x() - glyph / 2;
            double top = placed.y() - glyph / 2;
            String role = "glyph " + placed.kind().name().toLowerCase(Locale.ROOT);
            return switch (placed.kind()) {
                case DECISION -> new Rect(left, top, glyph, glyph, DecisionFigureText.DECISION_FILL, 1, stroke, role);
                case CHANCE -> new Circle(placed.x(), placed.y(), glyph / 2, DecisionFigureText.CHANCE_FILL, stroke, role);
                case OUTCOME -> new Polygon(new double[] {left, top, left + glyph, placed.y(), left, top + glyph},
                        DecisionFigureText.OUTCOME_FILL, stroke, role);
                case END -> new Polyline(new double[] {placed.x(), top, placed.x(), top + glyph}, stroke, role);
            };
        }

        private void row(List<Mark> marks, List<Span> spans, double x, double top) {
            if (spans == null) {
                return;
            }
            double at = x;
            for (Span span : spans) {
                if (span.callout()) {
                    double r = size * 0.62;
                    double cy = top + lineHeight / 2;
                    marks.add(new Circle(at + r, cy, r, CALLOUT, null, "callout"));
                    double numberSize = size * 0.7;
                    double w = DecisionFigureText.width(span.text(), numberSize, true);
                    marks.add(new Text(at + r - w / 2, cy - DecisionFigureText.lineHeight(numberSize) / 2, span.text(),
                            numberSize, true, false, "#ffffff", "callout-number"));
                    at += 2 * r + SPAN_GAP;
                    continue;
                }
                marks.add(new Text(at, top + (lineHeight - DecisionFigureText.lineHeight(span.size())) / 2, span.text(),
                        span.size(), span.bold(), span.italic(), span.colour(), span.role()));
                at += DecisionFigureText.width(span.text(), span.size(), span.bold()) + SPAN_GAP;
            }
        }

        private void node(Node node) {
            List<Node> branches = DecisionTreeGeometry.branches(node);
            boolean terminal = node.kind == DecisionTreeSource.Kind.OUTCOME || branches.isEmpty();
            Optional<Interval> value = rollback.value(node);
            List<Span> top = new ArrayList<>();
            callout(node).ifPresent(top::add);
            top.add(new Span(value.map(v -> "⇒ " + money(v, symbol)).orElse("⇒ ?"), size, true, false,
                    value.isPresent() ? DecisionFigureText.INK : DecisionFigureText.GAP, "value"));
            List<Span> name = new ArrayList<>();
            if (!DecisionTreeGeometry.isBranch(node)) {
                name.add(new Span(node.label(), size, false, false, DecisionFigureText.INK, "name"));
            }
            if (terminal) {
                top.addAll(name);
                nodeTop.put(node, top);
                if (node.kind == DecisionTreeSource.Kind.OUTCOME) {
                    nodeBottom.put(node, costs(node.costs()));
                }
            } else {
                nodeTop.put(node, top);
                nodeBottom.put(node, name);
            }
            if (!terminal) {
                Optional<Node> chosen = rollback.chosen(node);
                for (Node branch : branches) {
                    branch(branch, chosen.isPresent(), chosen.orElse(null) == branch);
                    branch.next().ifPresent(this::node);
                }
            }
        }

        private void branch(Node branch, boolean decided, boolean chosen) {
            if (chosen) {
                chosenBranches.add(branch);
            }
            List<Span> above = new ArrayList<>();
            above.add(new Span(branch.label(), size, false, false, DecisionFigureText.INK, "name"));
            if (branch.kind == DecisionTreeSource.Kind.EVENT) {
                Optional<Measure> probability = branch.probability();
                if (probability.isPresent()) {
                    above.addAll(chip("p", probability.get()));
                } else {
                    above.add(new Span(branch.residual ? "p (residual)" : "p ?", size, false, false,
                            branch.residual ? DecisionFigureText.CAPTION : DecisionFigureText.GAP, "measure"));
                }
            }
            if (branch.kind == DecisionTreeSource.Kind.ALTERNATIVE) {
                Optional<Interval> value = rollback.value(branch);
                above.add(new Span(value.map(v -> "⇒ " + money(v, symbol)).orElse("⇒ ?"), size, true, false,
                        value.isPresent() ? (chosen ? DecisionFigureText.CHOSEN : DecisionFigureText.INK)
                                : DecisionFigureText.GAP, "value"));
            }
            if (chosen) {
                above.add(new Span("✓ chosen", size, true, false, DecisionFigureText.CHOSEN, "chosen"));
            }
            branchAbove.put(branch, above);
            List<Span> below = new ArrayList<>();
            if (!branch.costs().isEmpty()) {
                callout(branch).ifPresent(below::add);
            }
            below.addAll(costs(branch.costs()));
            branchBelow.put(branch, below);
            if (branch.kind == DecisionTreeSource.Kind.ALTERNATIVE && decided && !chosen) {
                barred.add(branch);
            }
        }

        private Optional<Span> callout(Node node) {
            return rollback.step(node).map(step -> new Span(Integer.toString(step.number()), size, true, false, CALLOUT, "callout"));
        }

        private List<Span> costs(List<Measure> costs) {
            List<Span> below = new ArrayList<>();
            for (Measure cost : costs) {
                below.addAll(chip(cost.kind() == MeasureKind.MONEY ? "money" : "health", cost));
            }
            return below;
        }

        /** A measure as its chip reads: the word, the bounds, and a mark when its range holds a flip of the root's choice. */
        private List<Span> chip(String word, Measure measure) {
            Optional<Interval> bounds = rollback.measure(measure);
            String shown = bounds.map(b -> bounds(b, measure.derived(), "money".equals(word))).orElse("?");
            String colour = bounds.isEmpty() ? DecisionFigureText.GAP
                    : measure.derived() ? DecisionFigureText.DERIVED : DecisionFigureText.INK;
            List<Span> spans = new ArrayList<>();
            spans.add(new Span(word.isEmpty() ? shown : word + " " + shown, size, false, measure.derived(), colour, "measure"));
            if (flipping.contains(measure)) {
                spans.add(new Span("▲", size * 0.75, false, false, DecisionFigureText.HOT, "flip-mark"));
            }
            return spans;
        }

        private double widthOf(List<Span> spans) {
            if (spans == null || spans.isEmpty()) {
                return 0;
            }
            double width = 0;
            for (Span span : spans) {
                width += (span.callout() ? 2 * size * 0.62 : DecisionFigureText.width(span.text(), span.size(), span.bold()))
                        + SPAN_GAP;
            }
            return width - SPAN_GAP;
        }

        private DecisionTreeGeometry.Sizing sizing() {
            return new DecisionTreeGeometry.Sizing() {
                @Override
                public double rowHeight() {
                    return 2 * lineHeight + 10;
                }

                @Override
                public double glyph() {
                    return glyph;
                }

                @Override
                public double gap() {
                    return 10;
                }

                @Override
                public double margin() {
                    return 6;
                }

                @Override
                public double branchLabelWidth(Node branch) {
                    return Math.max(widthOf(branchAbove.get(branch)), widthOf(branchBelow.get(branch))) + 14
                            + (barred.contains(branch) ? BAR_ROOM : 0);
                }

                @Override
                public double nodeLabelWidth(Node node) {
                    return Math.max(widthOf(nodeTop.get(node)), widthOf(nodeBottom.get(node))) + 4;
                }
            };
        }
    }

    // ── The tornado ──────────────────────────────────────────────────────────

    /** The sweep read into the shared tornado builder's terms, named as the live chart names things. */
    static Tornado tornado(DecisionTreeSweep sweep) {
        Node root = sweep.root();
        Node baseChosen = sweep.baseChosen().orElse(null);
        Map<Measure, Node> owners = new IdentityHashMap<>();
        DecisionTreeSweep.collect(root, new ArrayList<>(), owners);
        List<Bar> bars = new ArrayList<>();
        for (Swing swing : sweep.swings()) {
            if (swing.width() <= 0) {
                continue;
            }
            List<Flip> rootFlips = swing.flipsAt(root);
            List<Double> cuts = new ArrayList<>();
            cuts.add(swing.range().lower());
            rootFlips.forEach(flip -> cuts.add(flip.at()));
            cuts.add(swing.range().upper());
            List<Segment> segments = new ArrayList<>();
            for (int i = 1; i < cuts.size(); i++) {
                Node chosen = i == 1
                        ? (rootFlips.isEmpty() ? baseChosen : rootFlips.get(0).chosenBelow())
                        : rootFlips.get(i - 2).chosenAbove();
                Choice choice = chosen == null ? Choice.OPEN : chosen == baseChosen ? Choice.BASE : Choice.OTHER;
                segments.add(new Segment(swing.rootAtValue(cuts.get(i - 1)).orElse(sweep.baseRoot()),
                        swing.rootAtValue(cuts.get(i)).orElse(sweep.baseRoot()), choice));
            }
            List<Tick> ticks = new ArrayList<>();
            for (Flip flip : swing.flips()) {
                ticks.add(new Tick(swing.rootAtValue(flip.at()).orElse(sweep.baseRoot()), flip.decision() == root,
                        DecisionFigureText.value(flip.at())));
            }
            bars.add(new Bar(nameOf(swing.key(), owners), swing.range().lower(), swing.range().upper(), swing.stated(),
                    swing.rootAtLow(), swing.rootAtHigh(), swing.least(), swing.greatest(), segments, ticks));
        }
        List<String> notes = new ArrayList<>();
        int still = (int) sweep.swings().stream().filter(s -> s.width() <= 0).count();
        if (still > 0) {
            notes.add(still == 1 ? "One value does not move the expected cost." : still + " values do not move the expected cost.");
        }
        for (Unswept unswept : sweep.unswept()) {
            notes.add("Not swept: " + nameOf(unswept.key(), owners) + ", " + unswept.why() + ".");
        }
        return new Tornado(sweep.baseRoot(), sweep.least(), sweep.greatest(), bars, notes);
    }

    /** How a swept measure is named: its quantity, or its kind under the branch that carries it. */
    static String nameOf(Object key, Map<Measure, Node> owners) {
        if (key == DecisionTreeSweep.HEALTH_PRICE) {
            return "price of a healthy year";
        }
        if (!(key instanceof Measure measure)) {
            return "a value";
        }
        Node owner = owners.get(measure);
        String ownerName = owner == null ? "a node" : owner.label();
        return switch (measure.kind()) {
            case PARAMETER -> measure.quantity() != null ? measure.quantity().shown() : "a parameter";
            case MONEY -> ownerName + " · money";
            case HEALTH -> ownerName + " · health";
            case PROBABILITY -> "p of " + ownerName;
        };
    }

    /** A rolled-back value to the whole unit, as the live diagram prints it, after a symbol when one is given. */
    static String money(Interval bounds, String symbol) {
        String prefix = symbol == null ? "" : symbol;
        if (bounds.isPoint()) {
            return prefix + String.format(Locale.ROOT, "%,d", Math.round(bounds.lower()));
        }
        return (bounds.lowerOpen() ? "(" : "[") + prefix + String.format(Locale.ROOT, "%,d", Math.round(bounds.lower()))
                + ", " + prefix + String.format(Locale.ROOT, "%,d", Math.round(bounds.upper())) + (bounds.upperOpen() ? ")" : "]");
    }

    /** A root cost on the tornado's axis. */
    static String money(double root, String symbol) {
        return Double.isNaN(root) ? "?" : money(Interval.point(root), symbol);
    }

    /** A measure's bounds as its chip prints them, which is how the live surface's {@code Bounds} prints: whole, or to four places. */
    /** An interval as a chip prints it: a stated number as written, a computed one no wider than it earns. */
    static String bounds(Interval interval, boolean computed, boolean money) {
        if (interval.isPoint()) {
            return DecisionFigureText.shown(interval.lower(), computed, money);
        }
        return (interval.lowerOpen() ? "(" : "[") + DecisionFigureText.shown(interval.lower(), computed, money) + ", "
                + DecisionFigureText.shown(interval.upper(), computed, money) + (interval.upperOpen() ? ")" : "]");
    }


    /** The step that worked a node's number, for a callout; package-visible for the processor's legend. */
    static Optional<Step> step(DecisionTreeRollback rollback, Node node) {
        return rollback.step(node);
    }
}
