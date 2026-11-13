package com.capstone.fraudcase;

import com.capstone.fraudcase.agent.common.RecommendationValidationException;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.dto.PolicyCitation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationValidatorTest {

    private final RecommendationValidator validator = new RecommendationValidator();

    private FraudCaseRecommendation valid() {
        FraudCaseRecommendation r = new FraudCaseRecommendation();
        r.setCaseId("CASE-1");
        r.setClassification("UNAUTHORISED_TRANSACTION");
        r.setRiskLevel("HIGH");
        r.setRecommendedRoute("FRAUD_INVESTIGATION");
        r.setConfidence(0.8);
        r.setHumanReviewRequired(true);
        r.setEvidenceIds(List.of("EV-1"));
        r.setRationale("test");
        return r;
    }

    @Test
    void rejectsUnknownEnum() {
        FraudCaseRecommendation r = valid();
        r.setClassification("INVENTED_VALUE");
        assertThatThrownBy(() -> validator.requireValid(r))
                .isInstanceOf(RecommendationValidationException.class);
    }

    @Test
    void rejectsConfidenceOutOfRange() {
        FraudCaseRecommendation r = valid();
        r.setConfidence(1.7);
        assertThatThrownBy(() -> validator.requireValid(r))
                .isInstanceOf(RecommendationValidationException.class);
    }

    @Test
    void rejectsMissingCaseId() {
        FraudCaseRecommendation r = valid();
        r.setCaseId(null);
        assertThatThrownBy(() -> validator.requireValid(r))
                .isInstanceOf(RecommendationValidationException.class);
    }

    @Test
    void rejectsBypassOfHumanReview() {
        FraudCaseRecommendation r = valid();
        r.setHumanReviewRequired(false);
        assertThatThrownBy(() -> validator.requireValid(r))
                .isInstanceOf(RecommendationValidationException.class);
    }

    @Test
    void rejectsMalformedCitation() {
        FraudCaseRecommendation r = valid();
        r.setPolicyCitations(List.of(new PolicyCitation("", "1", "s", "c", 0.5)));
        assertThatThrownBy(() -> validator.requireValid(r))
                .isInstanceOf(RecommendationValidationException.class);
    }

    @Test
    void rejectsEmptyEvidenceUnlessInsufficient() {
        FraudCaseRecommendation r = valid();
        r.setEvidenceIds(List.of());
        assertThatThrownBy(() -> validator.requireValid(r))
                .isInstanceOf(RecommendationValidationException.class);

        r.setClassification("INSUFFICIENT_EVIDENCE");
        validator.requireValid(r); // must pass with the explicit outcome
    }
}
