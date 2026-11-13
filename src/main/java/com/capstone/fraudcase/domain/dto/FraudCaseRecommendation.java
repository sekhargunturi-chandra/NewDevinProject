package com.capstone.fraudcase.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * The single structured output contract shared by rules, Spring AI and
 * LangChain4j processors. Always advisory - a human reviewer makes the
 * final disposition.
 */
public class FraudCaseRecommendation {

    private String caseId;
    private String classification;
    private String riskLevel;
    private String recommendedRoute;
    private double confidence;
    private boolean humanReviewRequired = true;
    private List<String> evidenceIds = new ArrayList<>();
    private List<PolicyCitation> policyCitations = new ArrayList<>();
    private List<String> missingInformation = new ArrayList<>();
    private List<String> ruleIds = new ArrayList<>();
    private List<String> toolTraceIds = new ArrayList<>();
    private String rationale;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendedRoute() {
        return recommendedRoute;
    }

    public void setRecommendedRoute(String recommendedRoute) {
        this.recommendedRoute = recommendedRoute;
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

    public List<String> getEvidenceIds() {
        return evidenceIds;
    }

    public void setEvidenceIds(List<String> evidenceIds) {
        this.evidenceIds = evidenceIds == null ? new ArrayList<>() : evidenceIds;
    }

    public List<PolicyCitation> getPolicyCitations() {
        return policyCitations;
    }

    public void setPolicyCitations(List<PolicyCitation> policyCitations) {
        this.policyCitations = policyCitations == null ? new ArrayList<>() : policyCitations;
    }

    public List<String> getMissingInformation() {
        return missingInformation;
    }

    public void setMissingInformation(List<String> missingInformation) {
        this.missingInformation = missingInformation == null ? new ArrayList<>() : missingInformation;
    }

    public List<String> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<String> ruleIds) {
        this.ruleIds = ruleIds == null ? new ArrayList<>() : ruleIds;
    }

    public List<String> getToolTraceIds() {
        return toolTraceIds;
    }

    public void setToolTraceIds(List<String> toolTraceIds) {
        this.toolTraceIds = toolTraceIds == null ? new ArrayList<>() : toolTraceIds;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
