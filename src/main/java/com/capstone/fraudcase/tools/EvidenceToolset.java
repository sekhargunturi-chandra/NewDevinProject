package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionStatus;
import com.capstone.fraudcase.domain.TxnChannel;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import dev.langchain4j.agent.tool.P;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Read-only allow-listed tools exposed to the model in both frameworks. Each
 * method carries the Spring AI {@link Tool} and LangChain4j
 * {@link dev.langchain4j.agent.tool.Tool} annotations on the same code, so
 * behaviour stays identical across modes. Results come from the deterministic
 * fixtures — a model can request evidence but can never fabricate it.
 */
@Component
public class EvidenceToolset {

    private final DeterministicEvidence fixtures;

    public EvidenceToolset(DeterministicEvidence fixtures) {
        this.fixtures = fixtures;
    }

    @Tool(name = "getDeviceSignals", description = "Check device novelty and trust indicators for a masked customer")
    @dev.langchain4j.agent.tool.Tool("Check device novelty and trust indicators for a masked customer")
    public Map<String, Object> getDeviceSignals(
            @ToolParam(description = "masked customer reference") @P("masked customer reference") String maskedCustomerRef,
            @ToolParam(description = "transaction reference") @P("transaction reference") String transactionRef) {
        return fixtures.getDeviceSignals(minimal(maskedCustomerRef, transactionRef, "MERCHANT ••••00"));
    }

    @Tool(name = "compareRecentLocations", description = "Compare current and recent trusted locations for a masked customer")
    @dev.langchain4j.agent.tool.Tool("Compare current and recent trusted locations for a masked customer")
    public Map<String, Object> compareRecentLocations(
            @ToolParam(description = "masked customer reference") @P("masked customer reference") String maskedCustomerRef) {
        return fixtures.getLocationComparison(minimal(maskedCustomerRef, "TXN-REF", "MERCHANT ••••00"));
    }

    @Tool(name = "getMerchantRiskProfile", description = "Return training merchant risk characteristics")
    @dev.langchain4j.agent.tool.Tool("Return training merchant risk characteristics")
    public Map<String, Object> getMerchantRiskProfile(
            @ToolParam(description = "masked merchant name") @P("masked merchant name") String maskedMerchant) {
        return fixtures.getMerchantRiskProfile(minimal("CUST-REF", "TXN-REF", maskedMerchant));
    }

    @Tool(name = "getPriorDisputes", description = "Summarise earlier disputes for a masked customer")
    @dev.langchain4j.agent.tool.Tool("Summarise earlier disputes for a masked customer")
    public Map<String, Object> getPriorDisputes(
            @ToolParam(description = "masked customer reference") @P("masked customer reference") String maskedCustomerRef) {
        return fixtures.getPriorDisputes(minimal(maskedCustomerRef, "TXN-REF", "MERCHANT ••••00"));
    }

    private CaseIntakeRequest minimal(String maskedCustomerRef, String transactionRef, String maskedMerchant) {
        return new CaseIntakeRequest(
                maskedCustomerRef, "N/A", transactionRef, BigDecimal.ONE, "INR",
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                TransactionStatus.POSTED, TxnChannel.CARD_NOT_PRESENT,
                maskedMerchant, IssueType.OTHER, "model tool call",
                true, false, true, true);
    }
}
