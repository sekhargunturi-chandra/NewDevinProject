package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.agent.rules.RulesFraudCaseAgent;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.dto.PolicyPassage;
import com.capstone.fraudcase.domain.dto.ToolEvidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RulesFraudCaseAgentTest {

    private final RulesFraudCaseAgent agent = new RulesFraudCaseAgent(new RecommendationValidator());

    private ToolEvidence tool(String name, Map<String, Object> result) {
        return new ToolEvidence("EV-" + name, "TR-" + name, name, "OK", 5, result, Instant.now());
    }

    private List<ToolEvidence> standardTools() {
        List<ToolEvidence> tools = new ArrayList<>();
        Map<String, Object> txn = new HashMap<>();
        txn.put("found", true);
        txn.put("authOutcome", "FRICTIONLESS");
        txn.put("duplicateFingerprint", false);
        tools.add(tool("getTransactionDetails", txn));

        Map<String, Object> device = new HashMap<>();
        device.put("deviceKnown", true);
        device.put("trustLevel", "HIGH");
        tools.add(tool("getDeviceSignals", device));

        Map<String, Object> loc = new HashMap<>();
        loc.put("anomalyLevel", "LOW");
        loc.put("distanceKm", 10L);
        tools.add(tool("compareRecentLocations", loc));

        Map<String, Object> merch = new HashMap<>();
        merch.put("riskBand", "LOW");
        merch.put("blocklistMatch", false);
        tools.add(tool("getMerchantRiskProfile", merch));

        Map<String, Object> hist = new HashMap<>();
        hist.put("count", 0L);
        hist.put("similarCount", 0L);
        tools.add(tool("getPriorDisputes", hist));
        return tools;
    }

    private PolicyPassage passage() {
        return new PolicyPassage("POL-CNP-017", "CNP procedure", "3.2", "2026-10-01",
                "3.2 Initial fraud routing", "pol-cnp-017-c1", 0.85, "route to fraud investigation");
    }

    private FraudCaseContext ctx(CaseIntakeRequest intake, List<ToolEvidence> tools,
                               List<PolicyPassage> passages) {
        return new FraudCaseContext("CASE-T1", intake, null, "analyst", "goal",
                tools, passages, "");
    }

    private void set(List<ToolEvidence> tools, String name, String key, Object value) {
        tools.stream().filter(t -> t.toolName().equals(name)).findFirst()
                .ifPresent(t -> t.result().put(key, value));
    }

    @Test
    void t01_unrecognisedMerchantWithAnomaliesRoutesToFraud() {
        var tools = standardTools();
        set(tools, "getTransactionDetails", "authOutcome", "UNAVAILABLE");
        set(tools, "getDeviceSignals", "deviceKnown", false);
        set(tools, "compareRecentLocations", "anomalyLevel", "HIGH");

        FraudCaseRecommendation r = agent.analyze(
                ctx(TestFixtures.unrecognisedMerchantCase(), tools, List.of(passage())));

        assertThat(r.getClassification()).isEqualTo("UNAUTHORISED_TRANSACTION");
        assertThat(r.getRecommendedRoute()).isEqualTo("FRAUD_INVESTIGATION");
        assertThat(r.getRiskLevel()).isEqualTo("HIGH");
        assertThat(r.isHumanReviewRequired()).isTrue();
        assertThat(r.getMissingInformation()).contains("authentication challenge outcome");
        assertThat(r.getRuleIds()).contains("R3-MULTI-HIGH-ANOMALY");
    }

    @Test
    void t02_recognisedMerchantFulfilmentRoutesToDisputeOps() {
        FraudCaseRecommendation r = agent.analyze(
                ctx(TestFixtures.merchantDisputeCase(), standardTools(), List.of(passage())));

        assertThat(r.getClassification()).isEqualTo("MERCHANT_DISPUTE");
        assertThat(r.getRecommendedRoute()).isEqualTo("DISPUTE_OPERATIONS");
        assertThat(r.getRuleIds()).contains("R4-MERCHANT-RECOGNISED-DISPUTE");
    }

    @Test
    void t03_duplicateFingerprintRoutesToPaymentOps() {
        var tools = standardTools();
        set(tools, "getTransactionDetails", "duplicateFingerprint", true);

        FraudCaseRecommendation r = agent.analyze(
                ctx(TestFixtures.duplicateCase(), tools, List.of(passage())));

        assertThat(r.getClassification()).isEqualTo("DUPLICATE_PROCESSING");
        assertThat(r.getRecommendedRoute()).isEqualTo("PAYMENT_OPERATIONS");
        assertThat(r.getRuleIds()).contains("R2-DUPLICATE-FINGERPRINT");
    }

    @Test
    void t05_noPolicyPassageAbstainsToManualReview() {
        FraudCaseRecommendation r = agent.analyze(
                ctx(TestFixtures.merchantDisputeCase(), standardTools(), List.of()));

        assertThat(r.getRecommendedRoute()).isEqualTo("MANUAL_REVIEW");
        assertThat(r.getMissingInformation())
                .contains("no policy passage met the retrieval threshold");
        assertThat(r.getRuleIds()).contains("R5-NO-POLICY-PASSAGE");
    }

    @Test
    void t08_unavailableToolRecordedAsMissing() {
        var tools = standardTools();
        tools.add(new ToolEvidence("EV-x", "TR-x", "getDeviceSignals",
                "TIMEOUT", 2500, Map.of("error", "timeout"), Instant.now()));

        FraudCaseRecommendation r = agent.analyze(
                ctx(TestFixtures.merchantDisputeCase(), tools, List.of(passage())));

        assertThat(r.getMissingInformation())
                .anyMatch(m -> m.contains("getDeviceSignals"));
    }

    @Test
    void sameInputProducesSameRecommendation() {
        var ctx = ctx(TestFixtures.unrecognisedMerchantCase(), standardTools(), List.of(passage()));
        FraudCaseRecommendation a = agent.analyze(ctx);
        FraudCaseRecommendation b = agent.analyze(ctx);
        assertThat(a.getClassification()).isEqualTo(b.getClassification());
        assertThat(a.getRecommendedRoute()).isEqualTo(b.getRecommendedRoute());
        assertThat(a.getConfidence()).isEqualTo(b.getConfidence());
    }
}
