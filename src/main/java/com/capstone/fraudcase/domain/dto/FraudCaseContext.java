package com.capstone.fraudcase.domain.dto;

import java.util.List;
import java.util.Map;

/**
 * Framework-neutral input to every {@code FraudCaseProcessor} implementation.
 * Contains only validated intake facts, structured tool evidence, retrieved
 * policy passages and case-scoped conversation context.
 */
public record FraudCaseContext(
        String caseId,
        CaseIntakeRequest intake,
        String reportedAt,
        String analystId,
        String investigationGoal,
        List<ToolEvidence> toolEvidence,
        List<PolicyPassage> policyContext,
        String conversationContext) {

    public Map<String, Object> promptVariables() {
        return Map.of(
                "caseId", caseId,
                "caseSummary", intake == null ? "" : intake.toString(),
                "investigationGoal", investigationGoal == null ? "" : investigationGoal,
                "toolEvidence", toolEvidence == null ? "" : toolEvidence.toString(),
                "policyContext", policyContext == null ? "" : policyContext.toString(),
                "conversationContext", conversationContext == null ? "" : conversationContext);
    }
}
