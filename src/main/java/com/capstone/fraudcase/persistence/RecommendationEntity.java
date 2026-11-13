package com.capstone.fraudcase.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "recommendation")
public class RecommendationEntity {

    @Id
    private String recommendationId;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String mode; // rules | spring-ai | langchain4j

    @Column(nullable = false)
    private String classification;

    @Column(nullable = false)
    private String recommendedRoute;

    @Column(nullable = false)
    private String riskLevel;

    private double confidence;

    @Column(nullable = false)
    private boolean humanReviewRequired = true;

    @Column(length = 2000)
    private String rationale;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String evidenceIdsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String missingInformationJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ruleIdsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String toolTraceIdsJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getRecommendedRoute() {
        return recommendedRoute;
    }

    public void setRecommendedRoute(String recommendedRoute) {
        this.recommendedRoute = recommendedRoute;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getEvidenceIdsJson() {
        return evidenceIdsJson;
    }

    public void setEvidenceIdsJson(String evidenceIdsJson) {
        this.evidenceIdsJson = evidenceIdsJson;
    }

    public String getMissingInformationJson() {
        return missingInformationJson;
    }

    public void setMissingInformationJson(String missingInformationJson) {
        this.missingInformationJson = missingInformationJson;
    }

    public String getRuleIdsJson() {
        return ruleIdsJson;
    }

    public void setRuleIdsJson(String ruleIdsJson) {
        this.ruleIdsJson = ruleIdsJson;
    }

    public String getToolTraceIdsJson() {
        return toolTraceIdsJson;
    }

    public void setToolTraceIdsJson(String toolTraceIdsJson) {
        this.toolTraceIdsJson = toolTraceIdsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
