package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.dto.PolicyPassage;
import com.capstone.fraudcase.domain.dto.ToolEvidence;
import com.capstone.fraudcase.tools.DeterministicEvidence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Framework parity: every FraudCaseProcessor implementation must return the
 * same schema and invariants on identical input.
 */
@SpringBootTest
class ProcessorParityTest {

    @Autowired
    List<FraudCaseProcessor> processors;

    @Autowired
    DeterministicEvidence fixtures;

    @Test
    void allProcessorsSatisfyTheContract() {
        assertThat(processors).hasSizeGreaterThanOrEqualTo(3);

        var intake = TestFixtures.unrecognisedMerchantCase();
        List<ToolEvidence> tools = List.of(
                new ToolEvidence("EV-1", "TR-1", "getTransactionDetails", "OK", 5,
                        fixtures.getTransactionDetails(intake), Instant.now()),
                new ToolEvidence("EV-2", "TR-2", "getDeviceSignals", "OK", 5,
                        fixtures.getDeviceSignals(intake), Instant.now()),
                new ToolEvidence("EV-3", "TR-3", "compareRecentLocations", "OK", 5,
                        fixtures.getLocationComparison(intake), Instant.now()),
                new ToolEvidence("EV-4", "TR-4", "getMerchantRiskProfile", "OK", 5,
                        fixtures.getMerchantRiskProfile(intake), Instant.now()));
        PolicyPassage passage = new PolicyPassage("POL-CNP-017", "CNP", "3.2",
                "2026-10-01", "3.2 Initial fraud routing", "pol-cnp-017-c1", 0.9,
                "route unrecognised merchant disputes to fraud investigation");
        FraudCaseContext ctx = new FraudCaseContext("CASE-PARITY", intake, null, "analyst",
                "classify and recommend route", tools, List.of(passage), "");

        for (FraudCaseProcessor p : processors) {
            FraudCaseRecommendation r = p.analyze(ctx);
            assertThat(r.getCaseId()).as(p.mode()).isEqualTo("CASE-PARITY");
            assertThat(r.isHumanReviewRequired()).as(p.mode()).isTrue();
            assertThat(r.getClassification()).as(p.mode()).isNotBlank();
            assertThat(r.getRecommendedRoute()).as(p.mode()).isNotBlank();
            assertThat(r.getConfidence()).as(p.mode()).isBetween(0.0, 1.0);
            assertThat(r.getEvidenceIds()).as(p.mode()).isNotEmpty();
            assertThat(r.getPolicyCitations()).as(p.mode()).isNotEmpty();
            assertThat(r.getRationale()).as(p.mode()).isNotBlank();
        }
    }
}
