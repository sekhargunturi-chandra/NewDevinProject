package com.capstone.fraudcase.domain.dto;

import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionStatus;
import com.capstone.fraudcase.domain.TxnChannel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CaseIntakeRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9*•\\-]{4,40}$",
                message = "letters, digits, •, * or - only, e.g. CUST-••••-4821")
        String maskedCustomerRef,

        @NotBlank @Size(max = 60) String homeRegion,

        @NotBlank @Pattern(regexp = "^[A-Za-z0-9\\-]{4,40}$",
                message = "letters, digits or - only, e.g. TXN-2026-784102") String transactionRef,

        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,

        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "ISO-4217 currency") String currency,

        @NotNull OffsetDateTime occurredAt,

        @NotNull TransactionStatus status,

        @NotNull TxnChannel channel,

        @NotBlank @Size(max = 80) @Pattern(regexp = "^[^\\d]*\\d{0,4}$|.*•.*",
                message = "masked merchant only — use • or at most 4 digits, e.g. NORTHSTAR ••••73") String maskedMerchant,

        @NotNull IssueType issueType,

        @NotBlank @Size(max = 1000)
        @Pattern(regexp = "^[^<>]*$", message = "markup/scripts are not allowed")
        String customerStatement,

        @NotNull Boolean cardPossession,

        @NotNull Boolean merchantRecognised,

        @AssertTrue(message = "synthetic data confirmation is required")
        Boolean syntheticDataConfirmed,

        @AssertTrue(message = "human review must be required")
        Boolean reviewRequired) {
}
