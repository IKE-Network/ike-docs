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

import network.ike.docs.koncept.DecisionTreeSource.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The drawing of a decision tree read from source, as geometry alone: the store-free twin of the
 * live surface's {@code DecisionTreeLayout} ({@code komet-claude-plugin}), kept in step with it
 * computation for computation so the documentation build draws the picture Komet draws. The tree
 * reads left to right from the root, a square where someone chooses, a circle where the world
 * decides, a triangle where the consequences are counted, and each branch an elbow from its node
 * to the node it leads to, with room along its horizontal run for what the branch says.
 *
 * <p>Every terminal takes a row: an outcome, a branch that leads nowhere, or a node the reader
 * has collapsed. A node sits at the midpoint of the rows its branches reach. A column holds the
 * nodes at one depth, counting decisions, chances and outcomes and not the branches between them,
 * and is as wide as its widest value label plus its widest branch label.
 */
final class DecisionTreeGeometry {

    /** What a glyph stands for in the drawing. */
    enum Kind { DECISION, CHANCE, OUTCOME, END }

    /** One glyph: a node at the centre {@code (x, y)}, in its column; an {@code END} is a branch that leads to no node. */
    record Placed(Node node, Kind kind, int column, double x, double y, boolean collapsed) {
    }

    /**
     * One branch as an elbow: a stub right from {@code (x0, y0)} to {@code xBend}, down or up to
     * the branch's row {@code y}, then right to {@code x1}, where the next glyph's left edge is.
     */
    record Run(Node branch, Node from, Node to, double x0, double y0, double xBend, double y, double x1) {
        double runLength() {
            return x1 - xBend;
        }
    }

    /** The sizes a surface draws by, in its own units. */
    interface Sizing {
        double rowHeight();

        double glyph();

        double gap();

        double margin();

        double branchLabelWidth(Node branch);

        double nodeLabelWidth(Node node);
    }

    private static final double LABEL_PAD = 6;

    private final List<Placed> placed;
    private final List<Run> runs;
    private final double width;
    private final double height;
    private final int rows;
    private final int columns;

    private DecisionTreeGeometry(List<Placed> placed, List<Run> runs, double width, double height, int rows, int columns) {
        this.placed = Collections.unmodifiableList(placed);
        this.runs = Collections.unmodifiableList(runs);
        this.width = width;
        this.height = height;
        this.rows = rows;
        this.columns = columns;
    }

    static DecisionTreeGeometry of(Node root, Sizing sizing, Predicate<Node> collapsed) {
        Builder builder = new Builder(sizing, collapsed);
        if (root != null) {
            builder.place(root, 0);
        }
        return builder.build();
    }

    /** The branches beneath a node: its alternatives or its events. */
    static List<Node> branches(Node node) {
        List<Node> branches = new ArrayList<>();
        for (Node child : node.children) {
            if (child.kind == DecisionTreeSource.Kind.ALTERNATIVE || child.kind == DecisionTreeSource.Kind.EVENT) {
                branches.add(child);
            }
        }
        return branches;
    }

    static boolean isBranch(Node node) {
        return node.kind == DecisionTreeSource.Kind.ALTERNATIVE || node.kind == DecisionTreeSource.Kind.EVENT;
    }

    List<Placed> placed() {
        return placed;
    }

    List<Run> runs() {
        return runs;
    }

    Optional<Placed> placed(Node node) {
        return placed.stream().filter(p -> p.node() == node).findFirst();
    }

    Optional<Run> run(Node branch) {
        return runs.stream().filter(r -> r.branch() == branch).findFirst();
    }

    double width() {
        return width;
    }

    double height() {
        return height;
    }

    int rows() {
        return rows;
    }

    int columns() {
        return columns;
    }

    private static final class Builder {
        private final Sizing sizing;
        private final Predicate<Node> collapsed;
        private final List<Pending> pendingNodes = new ArrayList<>();
        private final List<PendingRun> pendingRuns = new ArrayList<>();
        private int rows;

        private record Pending(Node node, Kind kind, int column, double y, boolean collapsed) {
        }

        private record PendingRun(Node branch, Node from, Node to, int column, double y) {
        }

        Builder(Sizing sizing, Predicate<Node> collapsed) {
            this.sizing = sizing;
            this.collapsed = collapsed;
        }

        double place(Node node, int column) {
            boolean folded = collapsed.test(node);
            List<Node> branches = branches(node);
            Kind kind = switch (node.kind) {
                case DECISION -> Kind.DECISION;
                case CHANCE -> Kind.CHANCE;
                case OUTCOME -> Kind.OUTCOME;
                case ALTERNATIVE, EVENT -> Kind.END;
            };
            if (node.kind == DecisionTreeSource.Kind.OUTCOME || folded || branches.isEmpty()) {
                double y = rowCentre(rows++);
                pendingNodes.add(new Pending(node, kind, column, y, folded && !branches.isEmpty()));
                return y;
            }
            int own = pendingNodes.size();
            pendingNodes.add(null);
            double first = Double.NaN;
            double last = Double.NaN;
            for (Node branch : branches) {
                Optional<Node> next = branch.next();
                double y;
                Node to;
                if (next.isPresent()) {
                    y = place(next.get(), column + 1);
                    to = next.get();
                } else {
                    y = rowCentre(rows++);
                    to = branch;
                    pendingNodes.add(new Pending(to, Kind.END, column + 1, y, false));
                }
                pendingRuns.add(new PendingRun(branch, node, to, column, y));
                if (Double.isNaN(first)) {
                    first = y;
                }
                last = y;
            }
            double y = (first + last) / 2;
            pendingNodes.set(own, new Pending(node, kind, column, y, false));
            return y;
        }

        private double rowCentre(int row) {
            return sizing.margin() + row * sizing.rowHeight() + sizing.rowHeight() / 2;
        }

        DecisionTreeGeometry build() {
            int columns = pendingNodes.stream().mapToInt(Pending::column).max().orElse(-1) + 1;
            double[] stub = new double[Math.max(columns, 1)];
            double[] branchLabel = new double[Math.max(columns, 1)];
            for (Pending node : pendingNodes) {
                stub[node.column()] = Math.max(stub[node.column()], sizing.nodeLabelWidth(node.node()) + LABEL_PAD);
            }
            for (PendingRun run : pendingRuns) {
                branchLabel[run.column()] = Math.max(branchLabel[run.column()], sizing.branchLabelWidth(run.branch()) + LABEL_PAD);
            }
            double glyph = sizing.glyph();
            double[] x = new double[Math.max(columns, 1)];
            x[0] = sizing.margin() + glyph / 2;
            for (int c = 1; c < columns; c++) {
                x[c] = x[c - 1] + glyph / 2 + stub[c - 1] + branchLabel[c - 1] + sizing.gap() + glyph / 2;
            }
            List<Placed> placed = new ArrayList<>();
            for (Pending node : pendingNodes) {
                placed.add(new Placed(node.node(), node.kind(), node.column(), x[node.column()], node.y(), node.collapsed()));
            }
            List<Run> runs = new ArrayList<>();
            for (PendingRun run : pendingRuns) {
                double y0 = pendingNodes.stream().filter(n -> n.node() == run.from()).mapToDouble(Pending::y).findFirst()
                        .orElse(run.y());
                double x0 = x[run.column()] + glyph / 2;
                double xBend = x0 + stub[run.column()];
                double x1 = x[run.column() + 1] - glyph / 2;
                runs.add(new Run(run.branch(), run.from(), run.to(), x0, y0, xBend, run.y(), x1));
            }
            double width = columns == 0 ? 2 * sizing.margin() : x[columns - 1] + glyph / 2 + stub[columns - 1] + sizing.margin();
            double height = 2 * sizing.margin() + Math.max(rows, 0) * sizing.rowHeight();
            return new DecisionTreeGeometry(placed, runs, width, height, rows, columns);
        }
    }
}
