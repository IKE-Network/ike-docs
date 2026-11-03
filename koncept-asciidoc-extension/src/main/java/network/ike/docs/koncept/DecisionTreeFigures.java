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
import java.util.OptionalDouble;
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
    /** The colour of the words that name the tree an outcome folds in: a link's blue. */
    static final String FOLD_IN = "#1f5fa8";

    /** The colour of the mark that a node holds ANF statements: the chosen run's blue. */
    static final String STATEMENT_MARK = "#1f3b73";

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
        return tree(root, rollback, symbol, sweep, OptionalDouble.empty());
    }

    /** The tree, counted out for a cohort when one is given: how many go each way beneath every run, and where they end up. */
    static DecisionFigure tree(Node root, DecisionTreeRollback rollback, String symbol, Optional<DecisionTreeSweep> sweep,
                               OptionalDouble cohort) {
        return tree(root, rollback, symbol, sweep, cohort, node -> !node.statements.isEmpty());
    }

    /**
     * The tree, with the nodes that hold ANF statements marked beside their names: those that name
     * statements, and any the document says more of, a decision whose concept is a complex concept.
     */
    static DecisionFigure tree(Node root, DecisionTreeRollback rollback, String symbol, Optional<DecisionTreeSweep> sweep,
                               OptionalDouble cohort, java.util.function.Predicate<Node> stated) {
        TreeBuilder builder = new TreeBuilder(root, rollback, symbol, sweep, cohort);
        builder.stated = stated;
        return builder.build();
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
        /** The most lines any run or outcome stacks beneath itself, which every row makes room for. */
        private int belowLines = 1;
        private final Set<Node> barred = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Node> chosenBranches = Collections.newSetFromMap(new IdentityHashMap<>());

        private final Optional<DecisionTreeCohort> cohort;
        private final Optional<DecisionTreeTradeoff> tradeoff;
        /** Which nodes hold ANF statements, marked beside their names. */
        private java.util.function.Predicate<Node> stated = node -> !node.statements.isEmpty();

        TreeBuilder(Node root, DecisionTreeRollback rollback, String symbol, Optional<DecisionTreeSweep> sweep,
                    OptionalDouble cohortSize) {
            this.root = root;
            this.rollback = rollback;
            this.symbol = symbol == null ? "" : symbol;
            this.cohort = root != null && cohortSize.isPresent()
                    ? Optional.of(DecisionTreeCohort.of(root, rollback, cohortSize.getAsDouble())) : Optional.empty();
            this.tradeoff = root == null ? Optional.empty() : DecisionTreeTradeoff.of(root, rollback);
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
            // the readouts: what the choice trades, and where a cohort ends up; then the legend
            double legendSize = BASE * 0.78;
            double legendLine = DecisionFigureText.lineHeight(legendSize);
            double wrapWidth = Math.max(200, layout.width() - 12);
            double y = layout.height() + 2;
            // the ledger: every alternative's consequences in their own units, per cohort when one is counted
            List<List<String>> table = ledgerTable(root, rollback, symbol, healthUnit(), cohort);
            if (table.size() > (root.kind == DecisionTreeSource.Kind.DECISION ? 2 : 1)) {
                double[] widths = new double[table.get(0).size()];
                for (List<String> row : table) {
                    for (int i = 0; i < row.size(); i++) {
                        widths[i] = Math.max(widths[i], DecisionFigureText.width(row.get(i), legendSize, true) + 18);
                    }
                }
                marks.add(new Text(6, y, ledgerTitle(cohort, root.kind == DecisionTreeSource.Kind.DECISION), legendSize, true, false,
                        DecisionFigureText.INK, "ledger-title"));
                y += legendLine;
                Optional<Node> chosenAlternative = rollback.chosen(root);
                for (int r = 0; r < table.size(); r++) {
                    List<String> row = table.get(r);
                    boolean header = r == 0;
                    boolean chosenRow = !header && chosenAlternative.isPresent() && chosenAlternative.get().label().equals(row.get(0));
                    double x = 6;
                    for (int i = 0; i < row.size(); i++) {
                        marks.add(new Text(x, y, row.get(i), legendSize, chosenRow, header, header ? DecisionFigureText.CAPTION
                                : chosenRow ? DecisionFigureText.CHOSEN : DecisionFigureText.INK, header ? "ledger-head" : "ledger"));
                        x += widths[i];
                    }
                    y += legendLine;
                }
                y += 3;
            }
            List<String> readouts = readouts();
            for (String readout : readouts) {
                for (String line : DecisionFigureText.wrap(readout, legendSize, wrapWidth)) {
                    marks.add(new Text(6, y, line, legendSize, false, false, DecisionFigureText.INK, "readout"));
                    y += legendLine;
                }
            }
            if (!readouts.isEmpty()) {
                y += 3;
            }
            for (String line : DecisionFigureText.wrap(legend(), legendSize, wrapWidth)) {
                marks.add(new Text(6, y, line, legendSize, false, false, DecisionFigureText.CAPTION, "legend"));
                y += legendLine;
            }
            return new DecisionFigure(layout.width(), y + 4, "decision-tree", marks);
        }

        /** The sentences beneath the drawing: the trade at the root, and a tally per alternative when a cohort is counted. */
        List<String> readouts() {
            List<String> lines = new ArrayList<>();
            if (rollback.unpriced() && root.children.stream().filter(c -> c.kind == DecisionTreeSource.Kind.ALTERNATIVE).count() > 1) {
                lines.add(DecisionFigureText.UNPRICED);
            }
            tradeoff.ifPresent(t -> {
                String healthUnit = healthUnit();
                String moneyText = money(Interval.point(Math.abs(t.money())), symbol);
                // a tree with no health anywhere says nothing about health
                String healthText = healthUnit.isEmpty() && firstSemantic(root, MeasureKind.HEALTH).isEmpty() && !hasKind(root, MeasureKind.HEALTH) ? ""
                        : DecisionFigureText.quantity(Math.abs(t.health()), DecisionFigureText.Unit.PLAIN, healthUnit);
                String priceText = Double.isNaN(t.pricePerHealthyYear()) ? "" : money(Interval.point(t.pricePerHealthyYear()), symbol);
                double per = cohort.map(DecisionTreeCohort::size).orElse(1.0);
                String perText = cohort.isPresent() ? DecisionFigureText.count(per) : "case";
                List<String> clauses = new ArrayList<>();
                for (DecisionTreeTradeoff.CountTrade trade : t.counts()) {
                    double more = trade.more();
                    if (Math.abs(more) < 1e-9) {
                        continue;
                    }
                    boolean chosenSpares = more < 0;
                    double paid = chosenSpares ? t.money() : -t.money();
                    String each = paid > 1e-9 ? money(Interval.point(paid / Math.abs(more)), symbol) : null;
                    clauses.add(DecisionFigureText.countClause((chosenSpares ? t.chosen() : t.nextBest()).label(), chosenSpares,
                            DecisionFigureText.count(Math.abs(more) * per), trade.unit(), perText, each));
                }
                lines.add(DecisionFigureText.tradeoff(t.chosen().label(), t.nextBest().label(), t.money(), moneyText,
                        t.health(), healthText, priceText, money(Interval.point(t.priced()), symbol), t.countedAgainst(), clauses));
            });
            cohort.ifPresent(c -> {
                for (DecisionTreeCohort.Tally tally : c.tallies()) {
                    if (tally.leaves().isEmpty()) {
                        continue;
                    }
                    // like outcomes read alike: two leaves named "clear" are one entry, whatever their ids
                    Map<String, Double> byName = new java.util.LinkedHashMap<>();
                    for (DecisionTreeCohort.Leaf leaf : tally.leaves()) {
                        byName.merge(leaf.node().concept != null ? leaf.node().label() : "reach the end", leaf.count(), Double::sum);
                    }
                    List<Map.Entry<String, Double>> entries = new ArrayList<>(byName.entrySet());
                    entries.sort(Map.Entry.<String, Double>comparingByValue().reversed());
                    lines.add(DecisionFigureText.tally(tally.alternative().label(), tally.alternative() == root, tally.count(),
                            entries.stream().map(Map.Entry::getKey).toList(), entries.stream().map(Map.Entry::getValue).toList()));
                }
            });
            return lines;
        }

        /** The name of the health unit the tree's health costs are in, from the first such cost; empty when none. */
        private String healthUnit() {
            return firstSemantic(root, MeasureKind.HEALTH);
        }

        /** The ledger's title: what one row is counted for. */
        static String ledgerTitle(Optional<DecisionTreeCohort> cohort) {
            return ledgerTitle(cohort, true);
        }

        /** The ledger's title, for a tree with a choice at its root or, when not {@code decided}, a tree of consequences. */
        static String ledgerTitle(Optional<DecisionTreeCohort> cohort, boolean decided) {
            if (!decided) {
                return cohort.isPresent() ? "Per " + DecisionFigureText.count(cohort.get().size()) + " who reach it" : "Per case that reaches it";
            }
            return cohort.isPresent() ? "Per " + DecisionFigureText.count(cohort.get().size()) + " who take each alternative"
                    : "Per case who takes each alternative";
        }

        /** The ledger as rows of text, a header first; see the live surface's {@code DecisionTreeFigures.ledgerTable}. */
        static List<List<String>> ledgerTable(Node root, DecisionTreeRollback rollback, String symbol, String healthUnit,
                                              Optional<DecisionTreeCohort> cohort) {
            Optional<DecisionTreeLedger> ledger = DecisionTreeLedger.of(root, rollback);
            // a choice needs two rows to weigh; a tree of consequences has one, what everyone who reaches it can expect
            int least = root.kind == DecisionTreeSource.Kind.DECISION ? 2 : 1;
            if (ledger.isEmpty() || ledger.get().entries().size() < least) {
                return List.of();
            }
            double per = cohort.map(DecisionTreeCohort::size).orElse(1.0);
            boolean anyMoney = ledger.get().entries().stream().anyMatch(e -> Math.abs(e.money()) > 1e-9);
            boolean anyHealth = ledger.get().entries().stream().anyMatch(e -> Math.abs(e.health()) > 1e-9);
            List<List<String>> rows = new ArrayList<>();
            List<String> head = new ArrayList<>();
            head.add("");
            if (anyMoney) {
                head.add("money");
            }
            if (anyHealth) {
                head.add(healthUnit.isEmpty() ? "healthy life lost" : healthUnit + " lost");
            }
            head.addAll(ledger.get().units());
            rows.add(head);
            for (DecisionTreeLedger.Entry entry : ledger.get().entries()) {
                List<String> row = new ArrayList<>();
                row.add(entry.alternative().label());
                if (anyMoney) {
                    row.add(money(Interval.point(entry.money() * per), symbol));
                }
                if (anyHealth) {
                    row.add(DecisionFigureText.shown(entry.health() * per, true, false));
                }
                for (String unit : ledger.get().units()) {
                    row.add(DecisionFigureText.count(entry.count(unit) * per));
                }
                rows.add(row);
            }
            return rows;
        }

        private static boolean hasKind(Node node, MeasureKind kind) {
            for (Measure measure : node.measures) {
                if (measure.kind() == kind) {
                    return true;
                }
            }
            for (Node child : node.children) {
                if (hasKind(child, kind)) {
                    return true;
                }
            }
            return false;
        }

        private static String firstSemantic(Node node, MeasureKind kind) {
            for (Measure measure : node.measures) {
                if (measure.kind() == kind && measure.semantic() != null) {
                    return measure.semantic().shown();
                }
            }
            for (Node child : node.children) {
                String found = firstSemantic(child, kind);
                if (!found.isEmpty()) {
                    return found;
                }
            }
            return "";
        }

        /** One or two sentences saying what the numbers are, as the live surface says beneath its drawing. */
        private String legend() {
            String unit = symbol.isEmpty() ? "" : ", in " + symbol;
            // a tree with no health anywhere says nothing about health
            String priced = hasKind(root, MeasureKind.HEALTH)
                    ? ", a year of healthy life priced at " + money(Interval.point(rollback.healthPrice()), symbol) : "";
            String beneath = cohort.isPresent()
                    ? " beneath a branch, how many of the " + DecisionFigureText.count(cohort.get().size()) + " go that way, and what it costs."
                    : " beneath a branch, what it costs.";
            return "\u21D2 is the expected cost from that point on" + unit + priced
                    + ". A percentage is the share who go that way;"
                    + beneath + " The heavy run is the alternative chosen, a barred one is"
                    + " rejected, and \u25B2 marks a value whose range flips the root's choice.";
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
            double y = top;
            for (Span span : spans) {
                if (span == BREAK) {
                    y += lineHeight;
                    at = x;
                    continue;
                }
                if (span.callout()) {
                    double r = size * 0.62;
                    double cy = y + lineHeight / 2;
                    marks.add(new Circle(at + r, cy, r, CALLOUT, null, "callout"));
                    double numberSize = size * 0.7;
                    double w = DecisionFigureText.width(span.text(), numberSize, true);
                    marks.add(new Text(at + r - w / 2, cy - DecisionFigureText.lineHeight(numberSize) / 2, span.text(),
                            numberSize, true, false, "#ffffff", "callout-number"));
                    at += 2 * r + SPAN_GAP;
                    continue;
                }
                marks.add(new Text(at, y + (lineHeight - DecisionFigureText.lineHeight(span.size())) / 2, span.text(),
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
                statementMark(node).ifPresent(name::add);
                foldIn(node).ifPresent(name::add);
            }
            if (terminal) {
                top.addAll(name);
                nodeTop.put(node, top);
                if (node.kind == DecisionTreeSource.Kind.OUTCOME) {
                    List<Span> bottom = new ArrayList<>();
                    stacked(bottom, callout(node), node.costs());
                    nodeBottom.put(node, bottom);
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
            statementMark(branch).ifPresent(above::add);
            if (branch.kind == DecisionTreeSource.Kind.EVENT) {
                Optional<Measure> probability = branch.probability();
                if (probability.isPresent()) {
                    above.addAll(chip("p", probability.get()));
                } else if (branch.residual) {
                    // the residual's share is what the other events leave, printed so that no one subtracts
                    String rest = rollback.share(branch).map(b -> DecisionFigureText.chip(b.lower(), b.upper(), b.lowerOpen(),
                            b.upperOpen(), true, DecisionFigureText.Unit.SHARE, "")).orElse("?");
                    above.add(new Span(rest + " (residual)", size, false, true, DecisionFigureText.CAPTION, "measure"));
                } else {
                    above.add(new Span("p ?", size, false, false, DecisionFigureText.GAP, "measure"));
                }
            }
            if (branch.kind == DecisionTreeSource.Kind.ALTERNATIVE) {
                Optional<Interval> value = rollback.value(branch);
                above.add(new Span(value.map(v -> "⇒ " + money(v, symbol)).orElse("⇒ ?"), size, true, false,
                        value.isPresent() ? (chosen ? DecisionFigureText.CHOSEN : DecisionFigureText.INK)
                                : DecisionFigureText.GAP, "value"));
            }
            branchAbove.put(branch, above);
            List<Span> below = new ArrayList<>();
            cohort.ifPresent(c -> c.count(branch).ifPresent(count -> below.add(new Span(
                    DecisionFigureText.ofCohort(count, c.size()), size, false, false, DecisionFigureText.CAPTION, "count"))));
            stacked(below, callout(branch), branch.costs());
            branchBelow.put(branch, below);
            if (branch.kind == DecisionTreeSource.Kind.ALTERNATIVE && decided && !chosen) {
                barred.add(branch);
            }
        }

        private Optional<Span> callout(Node node) {
            return rollback.step(node).map(step -> new Span(Integer.toString(step.number()), size, true, false, CALLOUT, "callout"));
        }

        /** A branch's or an outcome's costs: money then health, each in its unit, joined by a plus. */
        /** A line break between stacked lines beneath a run or an outcome. */
        /** The mark beside a node's name that it holds ANF statements, as the live drawing marks it. */
        private Optional<Span> statementMark(Node node) {
            return !stated.test(node) ? Optional.empty()
                    : Optional.of(new Span("ANF", size * 0.72, true, false, STATEMENT_MARK, "anf-mark"));
        }

        /** What an outcome folds in, {@code tree=…}, named beside it as the live drawing links it. */
        private Optional<Span> foldIn(Node node) {
            return rollback.folded(node).map(inner -> new Span("folds in \u201C" + inner.root().label() + "\u201D \u2197",
                    size * 0.85, false, true, FOLD_IN, "fold-in"));
        }

        private static final Span BREAK = new Span("", 0, false, false, DecisionFigureText.CAPTION, "break");

        /**
         * Stacks a node's costs beneath what is there already, the callout first: the priced costs on
         * one line, money then health, and the counted ones on the next, so a column is as wide as its
         * widest line rather than the whole of a run's text.
         */
        private void stacked(List<Span> into, Optional<Span> callout, List<Measure> costs) {
            List<Span> priced = new ArrayList<>(costs(costs.stream().filter(c -> c.kind() != MeasureKind.COUNT).toList()));
            List<Span> counted = new ArrayList<>(costs(costs.stream().filter(c -> c.kind() == MeasureKind.COUNT).toList()));
            if (callout.isPresent() && !costs.isEmpty()) {
                (priced.isEmpty() ? counted : priced).add(0, callout.get());
            }
            for (List<Span> line : List.of(priced, counted)) {
                if (line.isEmpty()) {
                    continue;
                }
                if (!into.isEmpty()) {
                    into.add(BREAK);
                }
                into.addAll(line);
            }
            if (!into.isEmpty()) {
                belowLines = Math.max(belowLines, 1 + (int) into.stream().filter(span -> span == BREAK).count());
            }
        }

        private List<Span> costs(List<Measure> costs) {
            List<Span> below = new ArrayList<>();
            for (Measure cost : costs) {
                if (!below.isEmpty()) {
                    below.add(new Span("+", size, false, false, DecisionFigureText.CAPTION, "plus"));
                }
                below.addAll(chip(cost.kind() == MeasureKind.MONEY ? "money" : cost.kind() == MeasureKind.HEALTH ? "health" : "count", cost));
            }
            return below;
        }

        /**
         * A measure as its chip reads: the bounds in the measure's unit, a share as a percentage,
         * money after its symbol, health with its unit's name, the kind's word only when there is
         * no symbol or name to say it; and a mark when its range holds a flip of the root's choice.
         */
        private List<Span> chip(String word, Measure measure) {
            Optional<Interval> bounds = rollback.measure(measure);
            boolean computed = measure.derived();
            DecisionFigureText.Unit unit = switch (word) {
                case "p" -> DecisionFigureText.Unit.SHARE;
                case "money" -> DecisionFigureText.Unit.MONEY;
                default -> DecisionFigureText.Unit.PLAIN;
            };
            String unitSymbol = unit == DecisionFigureText.Unit.MONEY ? symbol
                    : ("health".equals(word) || "count".equals(word)) && measure.semantic() != null ? measure.semantic().shown() : "";
            boolean worded = unit != DecisionFigureText.Unit.SHARE && unitSymbol.isEmpty();
            String shown = bounds.map(b -> DecisionFigureText.chip(b.lower(), b.upper(), b.lowerOpen(), b.upperOpen(),
                    computed, unit, unitSymbol)).orElse("?");
            String colour = bounds.isEmpty() ? DecisionFigureText.GAP
                    : measure.derived() ? DecisionFigureText.DERIVED : DecisionFigureText.INK;
            List<Span> spans = new ArrayList<>();
            spans.add(new Span(worded ? word + " " + shown : shown, size, false, measure.derived(), colour, "measure"));
            if (flipping.contains(measure)) {
                spans.add(new Span("▲", size * 0.75, false, false, DecisionFigureText.HOT, "flip-mark"));
            }
            return spans;
        }

        private double widthOf(List<Span> spans) {
            if (spans == null || spans.isEmpty()) {
                return 0;
            }
            double widest = 0;
            double width = 0;
            for (Span span : spans) {
                if (span == BREAK) {
                    widest = Math.max(widest, width - SPAN_GAP);
                    width = 0;
                    continue;
                }
                width += (span.callout() ? 2 * size * 0.62 : DecisionFigureText.width(span.text(), span.size(), span.bold()))
                        + SPAN_GAP;
            }
            return Math.max(widest, width - SPAN_GAP);
        }

        private DecisionTreeGeometry.Sizing sizing() {
            return new DecisionTreeGeometry.Sizing() {
                @Override
                public double rowHeight() {
                    return lineHeight * (1 + Math.max(1, belowLines)) + 10;
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

    /**
     * The sweep read into the shared tornado builder's terms, named as the live chart names
     * things: on the net value of the base's choice where the sweep carries one, else on the
     * root's expected cost, every caption and threshold printed in the measure's own unit.
     */
    static Tornado tornado(DecisionTreeSweep sweep, String symbol) {
        Node root = sweep.root();
        Node baseChosen = sweep.baseChosen().orElse(null);
        boolean net = sweep.netValued();
        String chosen = baseChosen == null ? null : baseChosen.label();
        Map<Measure, Node> owners = new IdentityHashMap<>();
        DecisionTreeSweep.collect(root, new ArrayList<>(), owners);
        List<Bar> bars = new ArrayList<>();
        for (Swing swing : sweep.swings()) {
            if ((net ? swing.netWidth() : swing.width()) <= 0) {
                continue;
            }
            Quantity quantity = quantity(root, swing, symbol);
            List<Flip> rootFlips = swing.flipsAt(root);
            List<Double> cuts = new ArrayList<>();
            cuts.add(swing.range().lower());
            rootFlips.forEach(flip -> cuts.add(flip.at()));
            cuts.add(swing.range().upper());
            List<Segment> segments = new ArrayList<>();
            for (int i = 1; i < cuts.size(); i++) {
                Node chosenHere = i == 1
                        ? (rootFlips.isEmpty() ? baseChosen : rootFlips.get(0).chosenBelow())
                        : rootFlips.get(i - 2).chosenAbove();
                Choice choice = chosenHere == null ? Choice.OPEN : chosenHere == baseChosen ? Choice.BASE : Choice.OTHER;
                segments.add(new Segment(at(sweep, swing, cuts.get(i - 1), net), at(sweep, swing, cuts.get(i), net), choice));
            }
            List<Tick> ticks = new ArrayList<>();
            for (Flip flip : swing.flips()) {
                ticks.add(new Tick(at(sweep, swing, flip.at(), net), flip.decision() == root, quantity.print(flip.at())));
            }
            bars.add(new Bar(nameOf(swing.key(), owners), quantity.print(swing.range().lower()),
                    quantity.print(swing.range().upper()), swing.stated(),
                    net ? swing.netAtLow() : swing.rootAtLow(), net ? swing.netAtHigh() : swing.rootAtHigh(),
                    net ? swing.leastNet() : swing.least(), net ? swing.greatestNet() : swing.greatest(), segments, ticks));
        }
        List<String> notes = new ArrayList<>();
        int still = (int) sweep.swings().stream().filter(s -> (net ? s.netWidth() : s.width()) <= 0).count();
        if (still > 0) {
            notes.add(still == 1 ? "One value does not move the answer." : still + " values do not move the answer.");
        }
        for (Unswept unswept : sweep.unswept()) {
            notes.add("Not swept: " + nameOf(unswept.key(), owners) + ", " + unswept.why() + ".");
        }
        String unit = symbol == null || symbol.isBlank() ? "" : ", in " + symbol;
        String title = net
                ? "Net value of \u201C" + chosen + "\u201D: what the next best alternative would cost, less what it costs"
                + unit + ". Zero is break-even."
                : "Expected cost at the root" + unit;
        return new Tornado(title, net, net ? chosen : null, net ? sweep.baseNet() : sweep.baseRoot(),
                net ? sweep.leastNet() : sweep.least(), net ? sweep.greatestNet() : sweep.greatest(), bars, notes);
    }

    /** Where the axis stands when a swing's measure is a value: the net value, or the root's cost. */
    private static double at(DecisionTreeSweep sweep, Swing swing, double value, boolean net) {
        return net ? swing.netAtValue(value).orElse(sweep.baseNet()) : swing.rootAtValue(value).orElse(sweep.baseRoot());
    }

    /** How a swept quantity's values print: the unit, and the money symbol or the unit's name. */
    record Quantity(DecisionFigureText.Unit unit, String symbol) {
        String print(double value) {
            return DecisionFigureText.quantity(value, unit, symbol);
        }
    }

    /**
     * The quantity a swept measure is, as the live surface's {@code SweptQuantity} reads it: a
     * probability or a share-valued parameter is a share; a money cost, the price of a healthy
     * year, and a parameter on a money cost's semantic are money; a health cost and anything else
     * carry their unit's name.
     */
    static Quantity quantity(Node root, Swing swing, String symbol) {
        String money = symbol == null ? "" : symbol;
        if (swing.key() == DecisionTreeSweep.HEALTH_PRICE) {
            return new Quantity(DecisionFigureText.Unit.MONEY, money);
        }
        if (!(swing.key() instanceof Measure measure)) {
            return new Quantity(DecisionFigureText.Unit.PLAIN, "");
        }
        return switch (measure.kind()) {
            case MONEY -> new Quantity(DecisionFigureText.Unit.MONEY, money);
            case HEALTH, COUNT -> new Quantity(DecisionFigureText.Unit.PLAIN, measure.semantic() == null ? "" : measure.semantic().shown());
            case PROBABILITY -> new Quantity(DecisionFigureText.Unit.SHARE, "");
            case PARAMETER -> {
                if (measure.semantic() == null) {
                    boolean share = swing.range().lower() >= 0 && swing.range().upper() <= 1;
                    yield new Quantity(share ? DecisionFigureText.Unit.SHARE : DecisionFigureText.Unit.PLAIN, "");
                }
                if (namesSemanticOf(root, MeasureKind.MONEY, measure.semantic())) {
                    yield new Quantity(DecisionFigureText.Unit.MONEY, money);
                }
                yield new Quantity(DecisionFigureText.Unit.PLAIN, measure.semantic().shown());
            }
        };
    }

    /** Whether a concept is the semantic any cost of a kind in the tree is on, by identifier, else by key. */
    private static boolean namesSemanticOf(Node node, MeasureKind kind, DecisionTreeSource.Concept concept) {
        for (Measure measure : node.measures) {
            if (measure.kind() == kind && measure.semantic() != null && same(measure.semantic(), concept)) {
                return true;
            }
        }
        for (Node child : node.children) {
            if (namesSemanticOf(child, kind, concept)) {
                return true;
            }
        }
        return false;
    }

    private static boolean same(DecisionTreeSource.Concept a, DecisionTreeSource.Concept b) {
        if (a.identifier() != null && b.identifier() != null) {
            return a.identifier().equals(b.identifier());
        }
        return a.key() != null && a.key().equals(b.key());
    }

    /**
     * How a swept measure is named: its quantity for a parameter, the price of a healthy year, or
     * its kind at the node that carries it, the node's name in quotes.
     */
    static String nameOf(Object key, Map<Measure, Node> owners) {
        if (key == DecisionTreeSweep.HEALTH_PRICE) {
            return "price of a healthy year";
        }
        if (!(key instanceof Measure measure)) {
            return "a value";
        }
        Node owner = owners.get(measure);
        String ownerName = owner == null ? "a node" : "\u201C" + owner.label() + "\u201D";
        return switch (measure.kind()) {
            case PARAMETER -> measure.quantity() != null ? measure.quantity().shown() : "a parameter";
            case MONEY -> "money cost at " + ownerName;
            case HEALTH -> "health cost at " + ownerName;
            case COUNT -> (measure.semantic() == null ? "count" : measure.semantic().shown()) + " at " + ownerName;
            case PROBABILITY -> "p of " + ownerName;
        };
    }

    /** A rolled-back value to the whole unit, as the live diagram prints it, after a symbol when one is given. */
    static String money(Interval bounds, String symbol) {
        String prefix = symbol == null ? "" : symbol;
        if (bounds.isPoint()) {
            // a negative amount, which a net value can be, carries its sign before the symbol
            return (bounds.lower() < 0 ? "\u2212" : "") + prefix + String.format(Locale.ROOT, "%,d", Math.round(Math.abs(bounds.lower())));
        }
        return (bounds.lowerOpen() ? "(" : "[") + prefix + String.format(Locale.ROOT, "%,d", Math.round(bounds.lower()))
                + ", " + prefix + String.format(Locale.ROOT, "%,d", Math.round(bounds.upper())) + (bounds.upperOpen() ? ")" : "]");
    }

    /** An amount on the tornado's axis. */
    static String money(double amount, String symbol) {
        return Double.isNaN(amount) ? "?" : money(Interval.point(amount), symbol);
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
