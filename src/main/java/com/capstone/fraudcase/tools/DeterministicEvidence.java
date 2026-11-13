package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import com.capstone.fraudcase.util.Hashes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic synthetic fixtures. Every signal is derived from a stable hash
 * of the masked references plus the configured seed, so identical intake
 * produces identical tool results for tests and demonstrations.
 */
@Component
public class DeterministicEvidence {

    private final String seed;

    public DeterministicEvidence(@Value("${capstone.tools.deterministic-seed:fraud-training-v1}") String seed) {
        this.seed = seed;
    }

    private long key(String... parts) {
        return Hashes.stableLong(seed + "|" + String.join("|", parts));
    }

    public Map<String, Object> getTransactionDetails(CaseIntakeRequest intake) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean duplicateFingerprint = intake.transactionRef().contains("DUP")
                || key("dup", intake.transactionRef()) % 7 == 0;
        boolean deviceUnknown = !deviceKnown(intake);
        String authOutcome = deviceUnknown ? "UNAVAILABLE"
                : switch ((int) (key("auth", intake.transactionRef()) % 3)) {
                    case 0 -> "FRICTIONLESS";
                    case 1 -> "CHALLENGE_PASSED";
                    default -> "CHALLENGE_FAILED";
                };
        result.put("found", true);
        result.put("status", intake.status().name());
        result.put("timestamp", intake.occurredAt().toString());
        result.put("amount", intake.amount());
        result.put("channel", intake.channel().name());
        result.put("authOutcome", authOutcome);
        result.put("maskedMerchant", intake.maskedMerchant());
        result.put("duplicateFingerprint", duplicateFingerprint);
        return result;
    }

    private boolean deviceKnown(CaseIntakeRequest intake) {
        // "NEW"-style customer refs always show a novel device for the demo
        if (intake.maskedCustomerRef().contains("NEWDEV")) {
            return false;
        }
        return key("device", intake.maskedCustomerRef(), intake.transactionRef()) % 10 >= 4;
    }

    public Map<String, Object> getDeviceSignals(CaseIntakeRequest intake) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean known = deviceKnown(intake);
        long firstSeenDaysAgo = known ? 30 + key("seen", intake.maskedCustomerRef()) % 300 : 0;
        result.put("deviceKnown", known);
        // Anchored to the transaction time so identical input gives identical output.
        result.put("firstSeenAt", intake.occurredAt().toInstant()
                .minus(firstSeenDaysAgo, ChronoUnit.DAYS).toString());
        result.put("trustLevel", known ? (firstSeenDaysAgo > 180 ? "HIGH" : "MEDIUM") : "LOW");
        result.put("observations", known
                ? List.of("device previously associated with trusted activity")
                : List.of("newly observed device", "no prior trusted sessions"));
        return result;
    }

    public Map<String, Object> getLocationComparison(CaseIntakeRequest intake) {
        Map<String, Object> result = new LinkedHashMap<>();
        long distanceKm = key("dist", intake.maskedCustomerRef()) % 3000;
        long timeDeltaMinutes = 15 + key("time", intake.maskedCustomerRef()) % 600;
        // Large distance with a recognised merchant still stays anomalous.
        String anomaly = distanceKm > 1500 && timeDeltaMinutes < 180 ? "HIGH"
                : distanceKm > 500 ? "MEDIUM" : "LOW";
        result.put("distanceKm", distanceKm);
        result.put("timeDeltaMinutes", timeDeltaMinutes);
        result.put("anomalyLevel", anomaly);
        result.put("observations", anomaly.equals("HIGH")
                ? List.of("location inconsistent with recent trusted activity")
                : anomaly.equals("MEDIUM")
                        ? List.of("moderate distance from last trusted location")
                        : List.of("location consistent with recent activity"));
        return result;
    }

    public Map<String, Object> getMerchantRiskProfile(CaseIntakeRequest intake) {
        Map<String, Object> result = new LinkedHashMap<>();
        String merchant = intake.maskedMerchant().toUpperCase();
        boolean elevated = merchant.contains("NORTHSTAR") || merchant.contains("RISKY")
                || key("mrc", merchant) % 10 < 3;
        boolean blocklist = merchant.contains("BLOCKLIST") || key("bl", merchant) % 10 == 0;
        result.put("riskBand", blocklist || elevated ? "HIGH" : key("mrc2", merchant) % 10 < 5 ? "MEDIUM" : "LOW");
        result.put("blocklistMatch", blocklist);
        result.put("category", merchant.contains("DIGITAL") ? "DIGITAL_GOODS" : "RETAIL");
        result.put("observations", blocklist
                ? List.of("merchant on training blocklist")
                : elevated ? List.of("elevated merchant risk band") : List.of("no adverse merchant indicators"));
        return result;
    }

    public Map<String, Object> getPriorDisputes(CaseIntakeRequest intake) {
        Map<String, Object> result = new LinkedHashMap<>();
        long count = key("hist", intake.maskedCustomerRef()) % 4;
        long similar = Math.min(count, key("hist2", intake.maskedCustomerRef()) % 3);
        result.put("count", count);
        result.put("similarCount", similar);
        result.put("lookbackDays", 365);
        result.put("observations", count == 0
                ? List.of("no prior disputes in lookback window")
                : List.of(count + " prior dispute(s), " + similar + " similar"));
        return result;
    }

    public Map<String, Object> calculateCaseSla(String route, Instant anchor) {
        Map<String, Object> result = new LinkedHashMap<>();
        int hours = switch (route) {
            case "FRAUD_INVESTIGATION" -> 4;
            case "DISPUTE_OPERATIONS" -> 24;
            case "PAYMENT_OPERATIONS" -> 48;
            default -> 12;
        };
        result.put("queue", route);
        result.put("priority", hours <= 4 ? "HIGH" : hours <= 24 ? "MEDIUM" : "STANDARD");
        result.put("targetAt", anchor.plus(hours, ChronoUnit.HOURS).toString());
        result.put("explanation", "training SLA target: " + hours + "h for " + route);
        return result;
    }
}
