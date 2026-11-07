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
import network.ike.docs.koncept.DecisionTreeSource.Interval;
import network.ike.docs.koncept.DecisionTreeSource.Measure;
import network.ike.docs.koncept.DecisionTreeSource.Node;
import network.ike.docs.koncept.DecisionTreeSource.Parsed;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The site inspection decision of {@code rda-site-inspection-tree} as the three blocks the live
 * surface draws ({@code dev-decision-point-knowledge}, settled 2026-11-06): six ANF statements, a
 * {@code [graphlet-model]} whose parameters read them, and a {@code [decision-tree]} and a
 * {@code [tornado]} that render it. The build lands where the bare block did, $250,625 for
 * inspecting against $750,000 for not; the prior runs at 15% while its statement determines 0.5% to
 * 40%, and the tornado sweeps it over that range, where the choice turns at about 1.9%.
 */
class GraphletModelBlockRenderingTest {

    /** The six parameter statements: five points and the prior's range, on a share's scale or in dollars. */
    static final String STATEMENTS = """
            [anf,type=request,id=req-inspection]
            --
            topic::   candidate[clinical site inspection] disp=PENDING
            purpose:: candidate[verification of the site's clinical data] disp=PENDING
            --

            [anf,type=performance,id=perf-problem-found]
            --
            topic::   candidate[material problem in the site's clinical data] disp=PENDING
            result::  [1,1] k:Presence[presence] @id=705057003
            method::  candidate[clinical site inspection] disp=PENDING
            --

            [anf,type=performance,id=est-material-problem]
            --
            topic::   candidate[material problem in the site's clinical data] disp=PENDING
            subject_of_information:: candidate[submissions with a conduct concern] disp=PENDING
            result::  [0.005,0.4] candidate[share of the subject group] disp=PENDING
            method::  k:Estimated[Estimated basis] @id=96420d3b-09c4-5ebc-a292-7d0a0423a6de
            --

            [anf,type=performance,id=est-inspection-sensitivity]
            --
            topic::   candidate[material finding on clinical site inspection] disp=PENDING
            subject_of_information:: candidate[clinical sites with a material problem in their data] disp=PENDING
            result::  [0.8,0.8] candidate[share of the subject group] disp=PENDING
            --

            [anf,type=performance,id=est-inspection-false-alarm]
            --
            topic::   candidate[material finding on clinical site inspection] disp=PENDING
            subject_of_information:: candidate[clinical sites with sound data] disp=PENDING
            result::  [0.05,0.05] candidate[share of the subject group] disp=PENDING
            --

            [anf,type=performance,id=est-inspection-cost]
            --
            topic::   candidate[clinical site inspection] disp=PENDING
            subject_of_information:: candidate[submissions with a conduct concern] disp=PENDING
            result::  [60000,60000] candidate[US dollar] disp=PENDING
            --

            [anf,type=performance,id=est-reanalysis-cost]
            --
            topic::   candidate[exclusion of the site's data and re-analysis] disp=PENDING
            subject_of_information:: candidate[submissions with a conduct concern] disp=PENDING
            result::  [250000,250000] candidate[US dollar] disp=PENDING
            --

            [anf,type=performance,id=est-bad-approval-cost]
            --
            topic::   candidate[approval on unreliable data] disp=PENDING
            subject_of_information:: candidate[submissions with a conduct concern] disp=PENDING
            result::  [5000000,5000000] candidate[US dollar] disp=PENDING
            --

            """;

    /** The site model, as the plugin's corpus {@code bimo-site-model.adoc} holds it. */
    static final String MODEL_TEXT = """
            decision: k:SiteInspectionDecision[Inspect the site?] @id=bimo-site-decision
              parameter = [0.15,0.15] anf:est-material-problem
              parameter = [0.8,0.8] anf:est-inspection-sensitivity
              parameter = [0.05,0.05] anf:est-inspection-false-alarm
              parameter = [60000,60000] anf:est-inspection-cost
              parameter = [250000,250000] anf:est-reanalysis-cost
              parameter = [5000000,5000000] anf:est-bad-approval-cost
              alternative: k:Inspect[inspect] @id=bimo-inspect statement=anf:req-inspection
                money = from anf:est-inspection-cost
                count = [1,1] k:Inspection[inspections] @id=bimo-inspection
                chance: k:SiteData[Site data] @id=bimo-data-inspected
                  event: k:Unreliable[unreliable] @id=bimo-unreliable-inspected
                    p = from anf:est-material-problem
                    chance: k:InspectionFinds[Inspection finds it?] @id=bimo-finds
                      event: k:Found[found] @id=bimo-found statement=anf:perf-problem-found
                        p = from anf:est-inspection-sensitivity
                        money = from anf:est-reanalysis-cost
                        count = [1,1] k:ProblemFound[problems found] @id=bimo-problem-found
                        outcome: k:SoundDecision[decision on sound data] @id=bimo-sound-after-finding
                      residual: k:Missed[missed] @id=bimo-missed
                        money = from anf:est-bad-approval-cost
                        count = [1,1] k:BadApproval[approvals on unreliable data] @id=bimo-bad-approval
                        outcome: k:BadDecision[approval on unreliable data] @id=bimo-bad-inspected
                  residual: k:Sound[sound] @id=bimo-sound-inspected
                    chance: k:FalseFinding[Inspection raises a finding anyway?] @id=bimo-false-finding
                      event: k:Alarm[false finding] @id=bimo-alarm
                        p = from anf:est-inspection-false-alarm
                        money = from anf:est-reanalysis-cost
                        count = [1,1] k:NeedlessReanalysis[needless re-analyses] @id=bimo-needless-reanalysis
                        outcome: k:SoundDecision[decision on sound data] @id=bimo-sound-after-alarm
                      residual: k:Clear[clear] @id=bimo-clear
                        outcome: k:SoundDecision[decision on sound data] @id=bimo-sound-clear
              alternative: k:NoInspection[do not inspect] @id=bimo-no-inspection
                chance: k:SiteData[Site data] @id=bimo-data-uninspected
                  event: k:Unreliable[unreliable] @id=bimo-unreliable-uninspected
                    p = from anf:est-material-problem
                    money = from anf:est-bad-approval-cost
                    count = [1,1] k:BadApproval[approvals on unreliable data] @id=bimo-bad-approval
                    outcome: k:BadDecision[approval on unreliable data] @id=bimo-bad-uninspected
                  residual: k:Sound[sound] @id=bimo-sound-uninspected
                    outcome: k:SoundDecision[decision on sound data] @id=bimo-sound-uninspected-outcome
            """;

    static final String MODEL = """
            .The site inspection model: its structure and the values it runs at
            [graphlet-model, id=bimo-site-model]
            ----
            %s----

            """.formatted(MODEL_TEXT);

    static final String TREE = """
            [decision-tree, model=bimo-site-model, cohort=100, title="Inspect the site?"]
            ----
            ----

            """;

    static final String TORNADO = """
            [tornado, model=bimo-site-model, title="How far each value swings the net value of inspecting"]
            ----
            ----

            """;

    /** The statements as the seam hands them to the reader, for the store-free tests that need no document. */
    static final StatementMeasures MEASURES = name -> Optional.ofNullable(switch (name) {
        case "est-material-problem" -> share(0.005, 0.4, "material problem in the site's clinical data among submissions with a conduct concern");
        case "est-inspection-sensitivity" -> share(0.8, 0.8, "material finding among sites with a problem");
        case "est-inspection-false-alarm" -> share(0.05, 0.05, "material finding among sound sites");
        case "est-inspection-cost" -> dollars(60_000, "clinical site inspection (US dollar)");
        case "est-reanalysis-cost" -> dollars(250_000, "re-analysis (US dollar)");
        case "est-bad-approval-cost" -> dollars(5_000_000, "approval on unreliable data (US dollar)");
        default -> null;
    });

    private static StatementMeasures.Stated share(double lower, double upper, String label) {
        return new StatementMeasures.Stated(new Interval(lower, upper, false, false), null, label);
    }

    private static StatementMeasures.Stated dollars(double amount, String label) {
        return new StatementMeasures.Stated(Interval.point(amount), new Concept("US dollar", "US dollar", null), label);
    }

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(AnfBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().block(GraphletModelBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().block(TornadoBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    @Test
    @DisplayName("The reader takes each parameter's range and scale from its statement, and the rollback lands on $250,625 against $750,000")
    void readsStatementsAndRollsBack() {
        Parsed parsed = DecisionTreeSource.parse(List.of(MODEL_TEXT.split("\n")), MEASURES);

        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        Node root = parsed.root();
        List<Measure> parameters = root.parameters();
        assertEquals(6, parameters.size());
        Measure prior = parameters.get(0);
        assertEquals("est-material-problem", prior.statement());
        assertEquals(0.15, prior.bounds().lower(), 1e-9, "the model's value, not the range's midpoint");
        assertTrue(prior.hasStatedRange(), "the statement's range travels with the parameter");
        assertEquals(0.005, prior.range().lower(), 1e-9);
        assertEquals(0.4, prior.range().upper(), 1e-9);
        assertTrue(prior.semantic() == null, "a share of the subject group is the share at a node");
        assertTrue(prior.name().startsWith("material problem in the site"), "named by the statement's topic: " + prior.name());
        Measure inspectionCost = parameters.get(3);
        assertEquals("US dollar", inspectionCost.semantic().shown(), "a cost's scale is the statement's own");
        Measure inspect = root.children.get(0).costs().get(0);
        assertTrue(inspect.readsAParameter(), "money = from anf:… reads the parameter as it is");
        assertEquals("US dollar", inspect.semantic().shown(), "on the statement's scale, with no unit written after in");

        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, 100_000);
        assertTrue(rollback.findings().isEmpty(), () -> "findings: " + rollback.findings());
        assertEquals(250_625, rollback.value(root.children.get(0)).orElseThrow().lower(), 0.5);
        assertEquals(750_000, rollback.value(root.children.get(1)).orElseThrow().lower(), 0.5);
        assertEquals(root.children.get(0), rollback.chosen(root).orElseThrow(), "inspecting is chosen");
    }

    @Test
    @DisplayName("A parameter with no value runs at its statement's midpoint, and a statement the document lacks is a problem")
    void midpointAndMissing() {
        String midpoint = MODEL_TEXT.replace("parameter = [0.15,0.15] anf:est-material-problem", "parameter = anf:est-material-problem");
        Parsed parsed = DecisionTreeSource.parse(List.of(midpoint.split("\n")), MEASURES);
        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        Measure prior = parsed.root().parameters().get(0);
        assertEquals(0.2025, prior.bounds().lower(), 1e-9, "the midpoint of 0.5% to 40%, computed and never written");
        DecisionTreeRollback rollback = DecisionTreeRollback.of(parsed.root(), 100_000);
        assertEquals(312_968.75, rollback.value(parsed.root().children.get(0)).orElseThrow().lower(), 0.5, "the midpoint moves the numbers");

        Parsed alone = DecisionTreeSource.parse(List.of(MODEL_TEXT.split("\n")));
        assertTrue(alone.problems().stream().anyMatch(p -> p.contains("anf:est-material-problem, which no anf block before it states")),
                () -> "problems: " + alone.problems());
        assertTrue(alone.problems().stream().anyMatch(p -> p.contains("a cost reads anf:est-inspection-cost, which no anf block")),
                () -> "problems: " + alone.problems());
    }

    @Test
    @DisplayName("The sweep swings the prior over the statement's range, as stated, from the model's value, and the choice turns at about 1.9%")
    void sweepsOverTheStatementsRange() {
        Node root = DecisionTreeSource.parse(List.of(MODEL_TEXT.split("\n")), MEASURES).root();
        DecisionTreeSweep sweep = DecisionTreeSweep.of(root, 100_000, DecisionTreeSweep.DEFAULT_STEPS);

        assertEquals(250_625, sweep.baseRoot(), 0.5);
        DecisionTreeSweep.Swing swing = sweep.swing(root.parameters().get(0)).orElseThrow();
        assertTrue(swing.stated(), "the range is the statement's, not assumed");
        assertEquals(0.005, swing.range().lower(), 1e-9);
        assertEquals(0.4, swing.range().upper(), 1e-9);
        assertEquals(0.15, swing.base(), 1e-9, "the tree runs at the model's value");
        List<DecisionTreeSweep.Flip> flips = swing.flipsAt(root);
        assertEquals(1, flips.size(), () -> "one flip: " + flips);
        assertEquals(0.019, flips.get(0).at(), 0.001);
        DecisionTreeSweep.Swing cost = sweep.swing(root.parameters().get(3)).orElseThrow();
        assertFalse(cost.stated(), "a point statement is swept over an assumed range");
        assertEquals(30_000, cost.range().lower(), 1e-9);
    }

    @Test
    @DisplayName("The model block tables its parameters at the values the tree runs at, with the statement's range")
    void modelBlockTablesTheParameters() {
        String html = convert(STATEMENTS + MODEL);

        assertTrue(html.contains("The site inspection model: its structure and the values it runs at"), "the block's title is the table's");
        assertTrue(html.contains("class=\"tableblock frame-all grid-all stretch graphlet-model\"")
                || html.contains("graphlet-model"), "the table carries the model's role");
        assertTrue(html.contains(">15%<"), "the prior runs at the model's value");
        assertTrue(html.contains(">0.5% to 40%<"), "beside the range its statement determined");
        assertTrue(html.contains("material problem in the site&#8217;s clinical data among submissions with a conduct concern")
                || html.contains("material problem in the site’s clinical data among submissions with a conduct concern"),
                "named by the topic and the group: " + html);
        assertTrue(html.contains(">60,000 US dollar<"), "a cost on the statement's own scale");
        assertTrue(html.contains("statement <code>est-material-problem</code>"), "and where it comes from");
        assertFalse(html.contains("The model has gaps"), () -> html);
        assertTrue(html.contains("id=\"graphlet-model-bimo-site-model\""), "the model is an anchor");
    }

    @Test
    @DisplayName("A tree rendering draws the model's tree, ledger and sentences, and no tornado and no assumptions")
    void treeRenderingDrawsTheModel() {
        String html = convert(STATEMENTS + MODEL + TREE);

        assertTrue(html.contains("class=\"decision-tree\""), "the tree is drawn");
        assertTrue(html.contains("⇒ $250,625"), "inspecting costs $250,625 in expectation");
        assertTrue(html.contains("⇒ $750,000"), "not inspecting $750,000");
        assertTrue(html.contains("Figure 1. Inspect the site?"), "the rendering's title is the figure's");
        assertTrue(html.contains("Per 100 who take each alternative"), "the ledger is counted out for the cohort");
        assertTrue(html.contains("A square is a choice"), "the legend follows");
        assertTrue(html.contains("request <code>req-inspection</code>"), "the statements the nodes hold are listed");
        assertTrue(html.contains("Where each number on the tree comes from"), "the worked table follows");
        assertFalse(html.contains("decision-tornado"), "no tornado beneath a rendering");
        assertFalse(html.contains("Model assumptions and default values"), "the model block tabled the parameters");
        assertFalse(html.contains("The tree has gaps"), () -> html);
    }

    @Test
    @DisplayName("A tornado rendering sweeps the prior's bar across 0.5% to 40%, turning at 1.9%")
    void tornadoRenderingSweepsTheModel() {
        String html = convert(STATEMENTS + MODEL + TREE + TORNADO);

        int tree = html.indexOf("class=\"decision-tree\"");
        int tornado = html.indexOf("class=\"decision-tornado\"");
        assertTrue(tree > 0 && tornado > tree, "the tornado is drawn after the tree, in its own block");
        assertTrue(html.contains("Figure 2. How far each value swings the net value of inspecting"), "with the block's title");
        assertTrue(html.contains("flips at 1.9%"), "the choice turns where the lab says");
        assertTrue(html.contains(">0.5%<") || html.contains("0.5%"), "the bar runs from the statement's lower bound");
        assertTrue(html.contains(">40%<") || html.contains("40%"), "to its upper");
        assertTrue(html.contains("Net value of “inspect”"), "on the net value of inspecting");
        assertTrue(html.contains("$60,000"), "a cost parameter prints as money");
    }

    @Test
    @DisplayName("A rendering that names no model the document has says so, and a bare body still reads as before")
    void unknownModelAndBareBody() {
        String html = convert("[decision-tree, model=nowhere]\n----\n----\n\n[tornado, model=nowhere]\n----\n----\n");
        assertEquals(2, count(html, "No graphlet-model block earlier in this document is named <code>nowhere</code>"), html);

        String bare = convert(STATEMENTS + "[decision-tree, title=\"Inspect the site?\"]\n----\n" + MODEL_TEXT + "----\n");
        assertTrue(bare.contains("⇒ $250,625"), "a body of its own reads the statements too");
        assertTrue(bare.contains("decision-tornado"), "with its tornado beneath, as before");
        assertTrue(bare.contains("Model assumptions and default values"), "and its assumptions");
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + part.length())) {
            count++;
        }
        return count;
    }

    private static String convert(String adoc) {
        return asciidoctor.convert(adoc, Options.builder().safe(SafeMode.UNSAFE).backend("html5").build());
    }
}
