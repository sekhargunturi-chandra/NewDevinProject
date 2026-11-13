package com.capstone.fraudcase.agent.rules;

import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.RecommendedRoute;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.dto.PolicyCitation;
import com.capstone.fraudcase.domain.dto.ToolEvidence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic fallback processor - no model dependency. Every result
 * carries the rule IDs and facts used, per the fallback contract.
 */
@Component
public class RulesFraudCaseAgent implements FraudCaseProcessor {

    private static final Set<IssueType> MERCHANT_DISPUTE_ISSUES = Set.of(
            IssueType.FULFILMENT_ISSUE, IssueType.CANCELLATION_ISSUE, IssueType.AMOUNT_DISPUTE);

    private final RecommendationValidator validator;

    public RulesFraudCaseAgent(RecommendationValidator validator) {
        this.validator = validator;
    }

    @Override
    public String mode() {
        return "rules";
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext ctx) {
        FraudCaseRecommendation rec = new FraudCaseRecommendation();
        rec.setCaseId(ctx.caseId());
        rec.setHumanReviewRequired(true);
        rec.setEvidenceIds(ctx.toolEvidence().stream().map(ToolEvidence::evidenceId).toList());
        rec.setToolTraceIds(ctx.toolEvidence().stream().map(ToolEvidence::traceId).toList());
        rec.setPolicyCitations(ctx.policyContext().stream().map(PolicyCitation::from).toList());

        List<String> rules = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Map<String, ToolEvidence> tools = toolMap(ctx);
        CaseIntakeRequest intake = ctx.intake();

        // Collect unavailable evidence first - never invent it.
        for (ToolEvidence ev : ctx.toolEvidence()) {
            if (!ev.ok()) {
                missing.add(ev.toolName() + " result " + ev.status().toLowerCase());
            }
        }
        Object auth = value(tools, "getTransactionDetails", "authOutcome");
        if ("UNAVAILABLE".equals(auth)) {
            missing.add("authentication challenge outcome");
            rules.add("R-AUTH-MISSING");
        }

        boolean txnFound = tools.containsKey("getTransactionDetails")
                && tools.get("getTransactionDetails").ok()
                && Boolean.TRUE.equals(value(tools, "getTransactionDetails", "found"));
        boolean duplicate = Boolean.TRUE.equals(value(tools, "getTransactionDetails", "duplicateFingerprint"));
        boolean deviceUnknown = Boolean.FALSE.equals(value(tools, "getDeviceSignals", "deviceKnown"));
        boolean locationHigh = "HIGH".equals(value(tools, "compareRecentLocations", "anomalyLevel"));
        boolean merchantRisky = "HIGH".equals(value(tools, "getMerchantRiskProfile", "riskBand"))
                || Boolean.TRUE.equals(value(tools, "getMerchantRiskProfile", "blocklistMatch"));
        Number similar = (Number) value(tools, "getPriorDisputes", "similarCount");
        boolean repeatHistory = similar != null && similar.longValue() > 0;
        long highAnomalies = List.of(deviceUnknown, locationHigh, merchantRisky, repeatHistory)
                .stream().filter(Boolean::booleanValue).count();

        String rationale;
        if (!txnFound || missing.size() >= 3) {
            // R1 - mandatory evidence missing
            rules.add("R1-MISSING-MANDATORY-EVIDENCE");
            if (!txnFound) {
                missing.add("transaction details not found");
            }
            apply(rec, Classification.INSUFFICIENT_EVIDENCE, RiskLevel.MEDIUM,
                    RecommendedRoute.MANUAL_REVIEW, 0.2);
            rationale = "Required evidence is missing or unavailable; routed to manual review. No automated action taken.";
        } else if (duplicate) {
            // R2 - confirmed duplicate fingerprint
            rules.add("R2-DUPLICATE-FINGERPRINT");
            apply(rec, Classification.DUPLICATE_PROCESSING, RiskLevel.MEDIUM,
                    RecommendedRoute.PAYMENT_OPERATIONS, 0.9);
            rationale = "Duplicate transaction fingerprint confirmed by getTransactionDetails; route to payment operations for reviewer confirmation.";
        } else if (Boolean.TRUE.equals(intake.merchantRecognised()) && MERCHANT_DISPUTE_ISSUES.contains(intake.issueType())) {
            // R4 - recognised merchant, service-type complaint
            rules.add("R4-MERCHANT-RECOGNISED-DISPUTE");
            apply(rec, Classification.MERCHANT_DISPUTE, RiskLevel.LOW,
                    RecommendedRoute.DISPUTE_OPERATIONS, 0.8);
            rationale = "Customer recognises the merchant and disputes fulfilment/cancellation/amount; route to dispute operations.";
        } else if (highAnomalies >= 2) {
            // R3 - multiple independent high-severity anomalies
            rules.add("R3-MULTI-HIGH-ANOMALY");
            boolean unauthorised = !Boolean.TRUE.equals(intake.merchantRecognised())
                    && intake.issueType() == IssueType.MERCHANT_NOT_RECOGNISED;
            apply(rec,
                    unauthorised ? Classification.UNAUTHORISED_TRANSACTION : Classification.SUSPICIOUS_TRANSACTION,
                    RiskLevel.HIGH, RecommendedRoute.FRAUD_INVESTIGATION, 0.85);
            rationale = "Multiple independently sourced high-severity anomalies present; route for human fraud review. No automated action taken.";
        } else if (highAnomalies == 1 || !missing.isEmpty()) {
            // R6 - partial anomaly or gaps: reduce confidence
            rules.add("R6-PARTIAL-ANOMALY-OR-GAP");
            apply(rec, Classification.SUSPICIOUS_TRANSACTION, RiskLevel.MEDIUM,
                    RecommendedRoute.FRAUD_INVESTIGATION, 0.55);
            rationale = "Isolated anomaly or missing evidence; reduced confidence and routed for review.";
        } else {
            // R7 - no adverse signals
            rules.add("R7-NO-ADVERSE-SIGNALS");
            apply(rec, Classification.GENUINE_TRANSACTION, RiskLevel.LOW,
                    RecommendedRoute.MANUAL_REVIEW, 0.55);
            rationale = "Evidence supports customer-authorised activity; still requires reviewer confirmation.";
        }

        // R5 - abstain when no policy passage clears the threshold
        if (ctx.policyContext().isEmpty()) {
            rules.add("R5-NO-POLICY-PASSAGE");
            missing.add("no policy passage met the retrieval threshold");
            rec.setRecommendedRoute(RecommendedRoute.MANUAL_REVIEW.name());
            rec.setRationale(rec.getRationale() + " No policy citation available; abstaining on procedure and routing to manual review.");
        } else {
            rec.setRationale(rationale);
        }

        rec.setMissingInformation(missing);
        rec.setRuleIds(rules);
        return validator.requireValid(rec);
    }

    private void apply(FraudCaseRecommendation rec, Classification c, RiskLevel risk,
                       RecommendedRoute route, double confidence) {
        rec.setClassification(c.name());
        rec.setRiskLevel(risk.name());
        rec.setRecommendedRoute(route.name());
        rec.setConfidence(confidence);
    }

    private Map<String, ToolEvidence> toolMap(FraudCaseContext ctx) {
        Map<String, ToolEvidence> map = new java.util.LinkedHashMap<>();
        for (ToolEvidence ev : ctx.toolEvidence()) {
            map.putIfAbsent(ev.toolName(), ev);
        }
        return map;
    }

    private Object value(Map<String, ToolEvidence> tools, String tool, String key) {
        ToolEvidence ev = tools.get(tool);
        return ev == null ? null : ev.result().get(key);
    }

    @Override
    public String ask(FraudCaseContext ctx, String question) {
        StringBuilder sb = new StringBuilder("Rules-mode answer (deterministic, no model): ");
        sb.append("case ").append(ctx.caseId()).append(" has ")
                .append(ctx.toolEvidence().size()).append(" evidence item(s) and ")
                .append(ctx.policyContext().size()).append(" policy citation(s). ");
        ctx.toolEvidence().stream()
                .filter(e -> !e.ok())
                .forEach(e -> sb.append("Unavailable: ").append(e.toolName()).append(". "));
        sb.append("Only a human reviewer can change the case disposition.");
        return sb.toString();
    }
}
