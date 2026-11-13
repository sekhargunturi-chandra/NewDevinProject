package com.capstone.fraudcase.agent.common;

/**
 * Deterministic heuristic used by the offline demo chat models so the Spring
 * AI and LangChain4j modes are fully demonstrable without provider
 * credentials. Produces the same JSON schema a real model would emit; the
 * adapters still run schema validation afterwards.
 */
public final class DemoRecommendationHeuristics {

    private DemoRecommendationHeuristics() {
    }

    public static String recommendationJson(String prompt) {
        String p = prompt == null ? "" : prompt;
        boolean duplicate = p.contains("\"duplicateFingerprint\":true") || p.contains("duplicateFingerprint=true");
        boolean deviceUnknown = p.contains("\"deviceKnown\":false") || p.contains("deviceKnown=false");
        boolean locationHigh = p.contains("\"anomalyLevel\":\"HIGH\"") || p.contains("anomalyLevel=HIGH");
        boolean merchantRisky = p.contains("\"riskBand\":\"HIGH\"") || p.contains("riskBand=HIGH")
                || p.contains("\"blocklistMatch\":true") || p.contains("blocklistMatch=true");
        boolean merchantRecognised = p.contains("\"merchantRecognised\":true") || p.contains("merchantRecognised=true");
        boolean merchantDisputeIssue = p.contains("FULFILMENT_ISSUE") || p.contains("CANCELLATION_ISSUE")
                || p.contains("AMOUNT_DISPUTE");
        boolean noPolicy = p.contains("POLICY_PASSAGE_COUNT: 0") || p.contains("POLICY_PASSAGE_COUNT:0");
        boolean authMissing = p.contains("\"authOutcome\":\"UNAVAILABLE\"") || p.contains("authOutcome=UNAVAILABLE");

        long anomalies = (deviceUnknown ? 1 : 0) + (locationHigh ? 1 : 0) + (merchantRisky ? 1 : 0);

        String classification;
        String route;
        String risk;
        double confidence;
        StringBuilder rationale = new StringBuilder();
        StringBuilder missing = new StringBuilder();

        if (duplicate) {
            classification = "DUPLICATE_PROCESSING";
            route = "PAYMENT_OPERATIONS";
            risk = "MEDIUM";
            confidence = 0.9;
            rationale.append("Duplicate transaction fingerprint observed; payment-operations review recommended. ");
        } else if (merchantRecognised && merchantDisputeIssue) {
            classification = "MERCHANT_DISPUTE";
            route = "DISPUTE_OPERATIONS";
            risk = "LOW";
            confidence = 0.8;
            rationale.append("Customer recognises the merchant and disputes fulfilment or amount; dispute-operations route recommended. ");
        } else if (anomalies >= 2) {
            classification = merchantRecognised ? "SUSPICIOUS_TRANSACTION" : "UNAUTHORISED_TRANSACTION";
            route = "FRAUD_INVESTIGATION";
            risk = "HIGH";
            confidence = 0.85;
            rationale.append("Multiple independent high-severity anomalies (device, location or merchant risk). ");
        } else if (anomalies == 1 || authMissing) {
            classification = "SUSPICIOUS_TRANSACTION";
            route = "FRAUD_INVESTIGATION";
            risk = "MEDIUM";
            confidence = 0.55;
            rationale.append("Isolated anomaly or unavailable evidence; reduced confidence. ");
        } else {
            classification = "GENUINE_TRANSACTION";
            route = "MANUAL_REVIEW";
            risk = "LOW";
            confidence = 0.55;
            rationale.append("No adverse signals detected; reviewer confirmation still required. ");
        }

        if (authMissing) {
            missing.append("\"authentication challenge outcome\",");
        }
        if (noPolicy) {
            route = "MANUAL_REVIEW";
            missing.append("\"no policy passage met the retrieval threshold\",");
            rationale.append("No policy citation available; abstaining on procedure. ");
        }
        rationale.append("Advisory recommendation only; a human reviewer must record the final disposition.");

        String missingJson = missing.length() == 0 ? ""
                : missing.substring(0, missing.length() - 1);
        return """
                {"classification":"%s","riskLevel":"%s","recommendedRoute":"%s",
                 "confidence":%s,"humanReviewRequired":true,
                 "missingInformation":[%s],
                 "rationale":"%s"}
                """.formatted(classification, risk, route, confidence, missingJson,
                rationale.toString().replace("\"", "'"));
    }

    public static String followUpAnswer(String prompt) {
        return "Demo-model answer (offline): the recommendation is advisory. "
                + "Evidence and policy citations shown on this case are the authoritative inputs; "
                + "only a human reviewer can change the disposition.";
    }
}
