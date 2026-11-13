package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.RecommendedRoute;
import com.capstone.fraudcase.domain.RiskLevel;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.dto.PolicyCitation;
import org.springframework.stereotype.Component;

/**
 * Application-level contract validation applied to every recommendation
 * (rules or model produced) before it is persisted or displayed.
 */
@Component
public class RecommendationValidator {

    public FraudCaseRecommendation requireValid(FraudCaseRecommendation r) {
        if (r == null) {
            throw new RecommendationValidationException("recommendation is null");
        }
        if (r.getCaseId() == null || r.getCaseId().isBlank()) {
            throw new RecommendationValidationException("caseId is required");
        }
        requireEnum(r.getClassification(), Classification.class, "classification");
        requireEnum(r.getRiskLevel(), RiskLevel.class, "riskLevel");
        requireEnum(r.getRecommendedRoute(), RecommendedRoute.class, "recommendedRoute");
        if (r.getConfidence() < 0.0 || r.getConfidence() > 1.0) {
            throw new RecommendationValidationException("confidence must be within 0..1");
        }
        if (!r.isHumanReviewRequired()) {
            throw new RecommendationValidationException("humanReviewRequired must be true");
        }
        boolean insufficient = Classification.INSUFFICIENT_EVIDENCE.name().equals(r.getClassification());
        if (r.getEvidenceIds().isEmpty() && !insufficient) {
            throw new RecommendationValidationException(
                    "at least one evidence item is required unless classification is INSUFFICIENT_EVIDENCE");
        }
        for (PolicyCitation c : r.getPolicyCitations()) {
            if (c.documentId() == null || c.documentId().isBlank()
                    || c.chunkId() == null || c.chunkId().isBlank()
                    || c.score() < 0.0 || c.score() > 1.0) {
                throw new RecommendationValidationException("malformed policy citation");
            }
        }
        if (r.getRationale() == null || r.getRationale().isBlank()) {
            throw new RecommendationValidationException("rationale is required");
        }
        return r;
    }

    private <E extends Enum<E>> void requireEnum(String value, Class<E> type, String field) {
        try {
            Enum.valueOf(type, value == null ? "" : value);
        } catch (IllegalArgumentException e) {
            throw new RecommendationValidationException("unknown " + field + ": " + value);
        }
    }
}
