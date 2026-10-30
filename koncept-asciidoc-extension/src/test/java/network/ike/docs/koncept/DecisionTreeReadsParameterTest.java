/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.koncept;

import network.ike.docs.koncept.DecisionTreeSource.Node;
import network.ike.docs.koncept.DecisionTreeSource.Parsed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A measure written {@code from} a quantity alone, with nothing after {@code of}, takes that
 * parameter's value as it is — the form the regulatory topic's no-scan arm uses so that one
 * reopening hazard drives both arms, as the live surface reads it.
 */
class DecisionTreeReadsParameterTest {

    private static final String TREE = """
            decision: k:Fix[Fix now?] @id=fix-now
              parameter = [0.3,0.3] k:Hazard[Failure hazard] @id=hazard
              parameter = [150000,150000] k:FailureCost[Cost of a failure] @id=failure-cost in k:Usd[US dollar] @id=usd
              alternative: k:Wait[wait] @id=wait
                chance: k:Fails[Fails?] @id=fails
                  event: k:Failure[failure] @id=failure
                    p = from k:Hazard[] @id=hazard
                    outcome: k:Broken[broken] @id=broken
                      money = from k:FailureCost[] @id=failure-cost in k:Usd[US dollar] @id=usd
                  residual: k:Holds[holds] @id=holds
                    outcome: k:Fine[fine] @id=fine
              alternative: k:Repair[repair] @id=repair
                money = [20000,20000] k:Usd[US dollar] @id=usd
                outcome: k:Safe[safe] @id=safe
            """;

    @Test
    @DisplayName("A share and a cost read from a parameter take its value, with no problem and no worked step of their own")
    void readsTheParameter() {
        Parsed parsed = DecisionTreeSource.parse(List.of(TREE.split("\n")));

        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        Node root = parsed.root();
        DecisionTreeRollback rollback = DecisionTreeRollback.of(root, 100_000);
        assertTrue(rollback.findings().isEmpty(), () -> "findings: " + rollback.findings());
        assertEquals(0.3 * 150_000, rollback.value(root.children.get(0)).orElseThrow().lower(), 0.01);
        assertEquals(root.children.get(1), rollback.chosen(root).orElseThrow(), "repairing is chosen");
        assertTrue(rollback.steps().stream().noneMatch(step -> step.what().startsWith("% ")),
                () -> "a read share is worked by its parameter's own callout, not a step: " + rollback.steps());
    }

    @Test
    @DisplayName("Reading a quantity the tree has no parameter for is a finding")
    void missingParameter() {
        String tree = TREE.replace("  parameter = [0.3,0.3] k:Hazard[Failure hazard] @id=hazard\n", "");
        Parsed parsed = DecisionTreeSource.parse(List.of(tree.split("\n")));

        assertTrue(parsed.problems().isEmpty(), () -> "problems: " + parsed.problems());
        DecisionTreeRollback rollback = DecisionTreeRollback.of(parsed.root(), 100_000);
        assertTrue(rollback.findings().stream().anyMatch(f -> f.contains("no parameter for")), () -> "findings: " + rollback.findings());
    }
}
