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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The calculator and calculate blocks in the documentation build: the Pooled Cohort Equations
 * read store-free and evaluate to the published shares of the reference implementation; a missing
 * input under the plausible policy widens the result; and a decision tree later in the document
 * reads the result as a parameter, so the statin decision is left open while the risk straddles
 * its break-even.
 */
class CalculatorBlockRenderingTest {

    static final String PCE = """
            [calculator]
            ----
            calculator: k:AscvdRisk10[10-year ASCVD risk] @id=pce source=goff2013
              output = k:AscvdRiskScore[10-year ASCVD risk] @id=ascvd-risk in k:PctPce2013[% over 10 years, PCE 2013] @id=pct-pce-2013 domain [0,100] scale=100
              input: k:Age[Age] @id=age in k:Year[year] @id=year domain [40,79] required
              input: k:Sex[Sex] @id=sex values=male;female required
              input: k:Race[Race] @id=race values=white;african-american when-missing=assumed
                missing = k:White[White] @id=white
              input: k:Sbp[Systolic blood pressure] @id=sbp in k:MmHg[mmHg] @id=mmhg domain [90,200] required
                missing = [100,160]
              input: k:Treated[Treated hypertension] @id=treated in k:Presence[presence] @id=presence domain [0,1] when-missing=assumed
                missing = [0,0]
              input: k:TotalChol[Total cholesterol] @id=tc in k:MgPerDl[mg/dL] @id=mg-per-dl domain [130,320] required
                conversion = [38.67,38.67] k:MmolPerL[mmol/L] @id=mmol-per-l
              input: k:Hdl[HDL cholesterol] @id=hdl in k:MgPerDl[mg/dL] @id=mg-per-dl domain [20,100] when-missing=plausible
                missing = [35,70]
              input: k:Diabetes[Diabetes mellitus] @id=diabetes in k:Presence[presence] @id=presence domain [0,1] required
              input: k:Smoker[Current smoker] @id=smoker in k:Presence[presence] @id=presence domain [0,1] when-missing=plausible
                missing = [0,1]
              term = k:LnAge[ln age] @id=ln-age from NaturalLogarithm of k:Age[] @id=age
              term = k:LnAgeSq[ln age squared] @id=ln-age-sq from Product of k:LnAge[] @id=ln-age k:LnAge[] @id=ln-age
              term = k:LnTc[ln total cholesterol] @id=ln-tc from NaturalLogarithm of k:TotalChol[] @id=tc
              term = k:LnAgeLnTc[ln age x ln TC] @id=ln-age-ln-tc from Product of k:LnAge[] @id=ln-age k:LnTc[] @id=ln-tc
              term = k:LnHdl[ln HDL] @id=ln-hdl from NaturalLogarithm of k:Hdl[] @id=hdl
              term = k:LnAgeLnHdl[ln age x ln HDL] @id=ln-age-ln-hdl from Product of k:LnAge[] @id=ln-age k:LnHdl[] @id=ln-hdl
              term = k:LnSbp[ln SBP] @id=ln-sbp from NaturalLogarithm of k:Sbp[] @id=sbp
              term = k:Untreated[untreated] @id=untreated from Complement of k:Treated[] @id=treated
              term = k:LnTreatedSbp[ln treated SBP] @id=ln-treated-sbp from Product of k:LnSbp[] @id=ln-sbp k:Treated[] @id=treated
              term = k:LnUntreatedSbp[ln untreated SBP] @id=ln-untreated-sbp from Product of k:LnSbp[] @id=ln-sbp k:Untreated[] @id=untreated
              term = k:LnAgeLnTreatedSbp[ln age x ln treated SBP] @id=ln-age-ln-treated-sbp from Product of k:LnAge[] @id=ln-age k:LnTreatedSbp[] @id=ln-treated-sbp
              term = k:LnAgeLnUntreatedSbp[ln age x ln untreated SBP] @id=ln-age-ln-untreated-sbp from Product of k:LnAge[] @id=ln-age k:LnUntreatedSbp[] @id=ln-untreated-sbp
              term = k:SmokerTerm[smoker] @id=smoker-term from k:Smoker[] @id=smoker
              term = k:LnAgeSmoker[ln age x smoker] @id=ln-age-smoker from Product of k:LnAge[] @id=ln-age k:Smoker[] @id=smoker
              term = k:DiabetesTerm[diabetes] @id=diabetes-term from k:Diabetes[] @id=diabetes
              stratum: k:WhiteFemale[White female] @id=white-female
                when = k:Sex[] @id=sex k:Female[Female] @id=female
                when = k:Race[] @id=race k:White[White] @id=white
                coefficient = [-29.799,-29.799] k:LnAge[] @id=ln-age basis=cited source=goff2013
                coefficient = [4.884,4.884] k:LnAgeSq[] @id=ln-age-sq basis=cited source=goff2013
                coefficient = [13.54,13.54] k:LnTc[] @id=ln-tc basis=cited source=goff2013
                coefficient = [-3.114,-3.114] k:LnAgeLnTc[] @id=ln-age-ln-tc basis=cited source=goff2013
                coefficient = [-13.578,-13.578] k:LnHdl[] @id=ln-hdl basis=cited source=goff2013
                coefficient = [3.149,3.149] k:LnAgeLnHdl[] @id=ln-age-ln-hdl basis=cited source=goff2013
                coefficient = [2.019,2.019] k:LnTreatedSbp[] @id=ln-treated-sbp basis=cited source=goff2013
                coefficient = [1.957,1.957] k:LnUntreatedSbp[] @id=ln-untreated-sbp basis=cited source=goff2013
                coefficient = [7.574,7.574] k:SmokerTerm[] @id=smoker-term basis=cited source=goff2013
                coefficient = [-1.665,-1.665] k:LnAgeSmoker[] @id=ln-age-smoker basis=cited source=goff2013
                coefficient = [0.661,0.661] k:DiabetesTerm[] @id=diabetes-term basis=cited source=goff2013
                coefficient = [0.96652,0.96652] k:S0[baseline survival] @id=s0 basis=cited source=goff2013
                coefficient = [-29.1817,-29.1817] k:Mean[mean of the terms] @id=mean basis=cited source=goff2013
              stratum: k:AaFemale[African American female] @id=aa-female
                when = k:Sex[] @id=sex k:Female[Female] @id=female
                when = k:Race[] @id=race k:AfricanAmerican[African American] @id=african-american
                coefficient = [17.1141,17.1141] k:LnAge[] @id=ln-age basis=cited source=goff2013
                coefficient = [0.9396,0.9396] k:LnTc[] @id=ln-tc basis=cited source=goff2013
                coefficient = [-18.9196,-18.9196] k:LnHdl[] @id=ln-hdl basis=cited source=goff2013
                coefficient = [4.4748,4.4748] k:LnAgeLnHdl[] @id=ln-age-ln-hdl basis=cited source=goff2013
                coefficient = [29.2907,29.2907] k:LnTreatedSbp[] @id=ln-treated-sbp basis=cited source=goff2013
                coefficient = [-6.4321,-6.4321] k:LnAgeLnTreatedSbp[] @id=ln-age-ln-treated-sbp basis=cited source=goff2013
                coefficient = [27.8197,27.8197] k:LnUntreatedSbp[] @id=ln-untreated-sbp basis=cited source=goff2013
                coefficient = [-6.0873,-6.0873] k:LnAgeLnUntreatedSbp[] @id=ln-age-ln-untreated-sbp basis=cited source=goff2013
                coefficient = [0.6908,0.6908] k:SmokerTerm[] @id=smoker-term basis=cited source=goff2013
                coefficient = [0.8738,0.8738] k:DiabetesTerm[] @id=diabetes-term basis=cited source=goff2013
                coefficient = [0.95334,0.95334] k:S0[] @id=s0 basis=cited source=goff2013
                coefficient = [86.6081,86.6081] k:Mean[] @id=mean basis=cited source=goff2013
              stratum: k:WhiteMale[White male] @id=white-male
                when = k:Sex[] @id=sex k:Male[Male] @id=male
                when = k:Race[] @id=race k:White[White] @id=white
                coefficient = [12.344,12.344] k:LnAge[] @id=ln-age basis=cited source=goff2013
                coefficient = [11.853,11.853] k:LnTc[] @id=ln-tc basis=cited source=goff2013
                coefficient = [-2.664,-2.664] k:LnAgeLnTc[] @id=ln-age-ln-tc basis=cited source=goff2013
                coefficient = [-7.99,-7.99] k:LnHdl[] @id=ln-hdl basis=cited source=goff2013
                coefficient = [1.769,1.769] k:LnAgeLnHdl[] @id=ln-age-ln-hdl basis=cited source=goff2013
                coefficient = [1.797,1.797] k:LnTreatedSbp[] @id=ln-treated-sbp basis=cited source=goff2013
                coefficient = [1.764,1.764] k:LnUntreatedSbp[] @id=ln-untreated-sbp basis=cited source=goff2013
                coefficient = [7.837,7.837] k:SmokerTerm[] @id=smoker-term basis=cited source=goff2013
                coefficient = [-1.795,-1.795] k:LnAgeSmoker[] @id=ln-age-smoker basis=cited source=goff2013
                coefficient = [0.658,0.658] k:DiabetesTerm[] @id=diabetes-term basis=cited source=goff2013
                coefficient = [0.91436,0.91436] k:S0[] @id=s0 basis=cited source=goff2013
                coefficient = [61.1816,61.1816] k:Mean[] @id=mean basis=cited source=goff2013
              stratum: k:AaMale[African American male] @id=aa-male
                when = k:Sex[] @id=sex k:Male[Male] @id=male
                when = k:Race[] @id=race k:AfricanAmerican[African American] @id=african-american
                coefficient = [2.469,2.469] k:LnAge[] @id=ln-age basis=cited source=goff2013
                coefficient = [0.302,0.302] k:LnTc[] @id=ln-tc basis=cited source=goff2013
                coefficient = [-0.307,-0.307] k:LnHdl[] @id=ln-hdl basis=cited source=goff2013
                coefficient = [1.916,1.916] k:LnTreatedSbp[] @id=ln-treated-sbp basis=cited source=goff2013
                coefficient = [1.809,1.809] k:LnUntreatedSbp[] @id=ln-untreated-sbp basis=cited source=goff2013
                coefficient = [0.549,0.549] k:SmokerTerm[] @id=smoker-term basis=cited source=goff2013
                coefficient = [0.645,0.645] k:DiabetesTerm[] @id=diabetes-term basis=cited source=goff2013
                coefficient = [0.89536,0.89536] k:S0[] @id=s0 basis=cited source=goff2013
                coefficient = [19.5425,19.5425] k:Mean[] @id=mean basis=cited source=goff2013
              formula = from CoxSurvival of k:S0[] @id=s0 k:Mean[] @id=mean
              band: k:Low[low] @id=low when [0,5) source=grundy2018
              band: k:Borderline[borderline] @id=borderline when [5,7.5) source=grundy2018
              band: k:Intermediate[intermediate] @id=intermediate when [7.5,20) source=grundy2018
              band: k:High[high] @id=high when [20,100] source=grundy2018
            ----
            """;

    static String patient(String sex, String race) {
        return """
                calculate: k:AscvdRisk10[] @id=pce
                  bind = k:Age[] @id=age [55,55] k:Year[] @id=year
                  bind = k:Sex[] @id=sex k:Sex[] @id=%s
                  bind = k:Race[] @id=race k:Race[] @id=%s
                  bind = k:Sbp[] @id=sbp [120,120] k:MmHg[] @id=mmhg
                  bind = k:Treated[] @id=treated [0,0] k:Presence[] @id=presence
                  bind = k:TotalChol[] @id=tc [213,213] k:MgPerDl[] @id=mg-per-dl
                  bind = k:Hdl[] @id=hdl [50,50] k:MgPerDl[] @id=mg-per-dl
                  bind = k:Diabetes[] @id=diabetes [0,0] k:Presence[] @id=presence
                  bind = k:Smoker[] @id=smoker [0,0] k:Presence[] @id=presence
                """.formatted(sex, race);
    }

    private static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().block(CalculatorBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().block(CalculateBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().block(DecisionTreeBlockProcessor.class);
        asciidoctor.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
    }

    private static CalculatorSource.Calculator pce() {
        CalculatorSource.Parsed parsed = CalculatorSource.parse(List.of(PCE.split("\n")));
        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        return parsed.calculator();
    }

    @Test
    @DisplayName("The four reference patients reproduce the published shares")
    void referencePatients() {
        CalculatorSource.Calculator calculator = pce();
        assertShare(calculator, "female", "white", 0.02054450);
        assertShare(calculator, "female", "african-american", 0.02998196);
        assertShare(calculator, "male", "white", 0.05378606);
        assertShare(calculator, "male", "african-american", 0.06061116);
    }

    private static void assertShare(CalculatorSource.Calculator calculator, String sex, String race, double expected) {
        CalculatorSource.Calculation calculation = CalculatorSource.parseCalculation(List.of(patient(sex, race).split("\n")));
        assertTrue(calculation.problems().isEmpty(), () -> "problems: " + calculation.problems());
        CalculatorEvaluation evaluation = CalculatorEvaluation.of(calculator, calculation);
        assertTrue(evaluation.determinable(), () -> "findings: " + evaluation.findings());
        assertEquals(expected, evaluation.share().orElseThrow().lower(), 5e-8);
        assertFalse(evaluation.interim());
    }

    @Test
    @DisplayName("A missing HDL under the plausible policy widens the result to what the equation gives at its corners")
    void missingHdl() {
        CalculatorSource.Calculator calculator = pce();
        String text = patient("male", "white").replace("  bind = k:Hdl[] @id=hdl [50,50] k:MgPerDl[] @id=mg-per-dl\n", "  bind = k:Hdl[] @id=hdl missing\n");
        CalculatorEvaluation evaluation = CalculatorEvaluation.of(calculator, CalculatorSource.parseCalculation(List.of(text.split("\n"))));
        assertTrue(evaluation.determinable(), () -> "findings: " + evaluation.findings());
        assertTrue(evaluation.interim());
        assertEquals(CalculatorEvaluation.Status.PLAUSIBLE, evaluation.input("hdl").orElseThrow().status());
        DecisionTreeSource.Interval share = evaluation.share().orElseThrow();
        assertTrue(share.lower() < 0.05378606 && share.upper() > 0.05378606, share.toString());
        assertTrue(share.upper() - share.lower() < 0.1, share.toString());
        assertFalse(evaluation.rows().isEmpty());
        assertTrue(evaluation.rows().stream().anyMatch(row -> row.input().key().equals("hdl") && row.solid()));
    }

    @Test
    @DisplayName("Rendered, the record shows its rail, its result and band, and a tree after it reads the risk")
    void renders() {
        String document = """
                = A note

                %s

                [calculate]
                ----
                calculate: k:AscvdRisk10[] @id=pce
                  bind = k:Age[] @id=age [62,62] k:Year[] @id=year
                  bind = k:Sex[] @id=sex k:Male[] @id=male
                  bind = k:Sbp[] @id=sbp [120,120] k:MmHg[] @id=mmhg
                  bind = k:TotalChol[] @id=tc [5.5,5.5] k:MmolPerL[] @id=mmol-per-l
                  bind = k:Hdl[] @id=hdl missing
                  bind = k:Diabetes[] @id=diabetes [0,0] k:Presence[] @id=presence
                  bind = k:Smoker[] @id=smoker clarify "smoking status is not in the note"
                ----

                [decision-tree, health-price=100000]
                ----
                decision: k:StartStatin[Start a statin?] @id=start-statin
                  parameter = [0.25,0.25] k:Rrr[Relative risk reduction] @id=rrr basis=cited
                  parameter = k:RiskLeft[Risk left] @id=risk-left from Complement of k:Rrr[] @id=rrr
                  parameter = k:RiskOnStatin[Risk on a statin] @id=risk-on-statin from Product of k:RiskLeft[] @id=risk-left k:AscvdRisk10[] @id=pce
                  alternative: k:Statin[statin] @id=statin
                    money = [1500,1500] k:Usd[US dollar] @id=usd basis=estimated
                    health = [0.05,0.05] k:Qaly[QALY] @id=qaly basis=estimated
                    chance: k:Event[ASCVD event] @id=event
                      event: k:Occurs[event] @id=occurs
                        p = from k:RiskOnStatin[] @id=risk-on-statin
                        money = [60000,60000] k:Usd[US dollar] @id=usd basis=estimated
                        health = [2,2] k:Qaly[QALY] @id=qaly basis=estimated
                      residual: k:NoEvent[no event] @id=no-event
                        outcome: k:Well[well] @id=well
                  alternative: k:NoStatin[no statin] @id=no-statin
                    chance: k:Event[ASCVD event] @id=event-2
                      event: k:Occurs[event] @id=occurs-2
                        p = from k:AscvdRisk10[10-year ASCVD risk] @id=pce
                        money = [60000,60000] k:Usd[US dollar] @id=usd basis=estimated
                        health = [2,2] k:Qaly[QALY] @id=qaly basis=estimated
                      residual: k:NoEvent[no event] @id=no-event-2
                        outcome: k:Well[well] @id=well-2
                ----
                """.formatted(PCE);
        String html = asciidoctor.convert(document, Options.builder().safe(SafeMode.UNSAFE).backend("html5")
                .attributes(org.asciidoctor.Attributes.builder().attribute("figure-caption", "Figure").build()).build());
        assertTrue(html.contains("9 inputs, 4 strata, 4 bands"), html);
        assertTrue(html.contains("What binds to each input"), html);
        assertTrue(html.contains("plausible range"), html);
        assertTrue(html.contains("clarify: smoking status is not in the note"), html);
        assertTrue(html.contains("intermediate"), html);
        assertTrue(html.contains("in progress, interim result"), html);
        assertTrue(html.contains("<svg"), "the tornado is drawn: " + html.length());
        // the tree read the risk: its assumptions table carries the injected parameter and no gap about it
        assertTrue(html.contains("computed by the document"), html);
        assertFalse(html.contains("no parameter for"), html);
        assertTrue(html.contains("Model assumptions and default values"), html);
    }

    @Test
    @DisplayName("A calculate block with no calculator before it says so")
    void noCalculator() {
        String html = asciidoctor.convert("""
                [calculate]
                ----
                calculate: k:Other[] @id=other
                  bind = k:Age[] @id=age [55,55] k:Year[] @id=year
                ----
                """, Options.builder().safe(SafeMode.UNSAFE).backend("html5").build());
        assertTrue(html.contains("No calculator block earlier in this document defines"), html);
    }

    static Map<String, Object> none() {
        return Map.of();
    }
}
