package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.dto.PolicyCitation;
import com.capstone.fraudcase.domain.dto.ToolEvidence;

/**
 * Merges raw model output with authoritative context data. Citations,
 * evidence IDs and trace IDs always come from the retriever and tool layer -
 * never from model output - so they cannot be hallucinated.
 */
public final class ModelOutputNormalizer {

    private ModelOutputNormalizer() {
    }

    public static FraudCaseRecommendation normalize(FraudCaseContext ctx, FraudCaseRecommendation model) {
        FraudCaseRecommendation out = model == null ? new FraudCaseRecommendation() : model;
        out.setCaseId(ctx.caseId());
        out.setHumanReviewRequired(true);
        out.setEvidenceIds(ctx.toolEvidence().stream().map(ToolEvidence::evidenceId).toList());
        out.setToolTraceIds(ctx.toolEvidence().stream().map(ToolEvidence::traceId).toList());
        out.setPolicyCitations(ctx.policyContext().stream().map(PolicyCitation::from).toList());
        if (out.getRationale() == null || out.getRationale().isBlank()) {
            out.setRationale("Model returned no rationale; routed for manual review.");
        }
        return out;
    }
}
