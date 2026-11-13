package com.capstone.fraudcase;

import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionStatus;
import com.capstone.fraudcase.domain.TxnChannel;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static CaseIntakeRequest unrecognisedMerchantCase() {
        return new CaseIntakeRequest(
                "CUST-NEWDEV-4821", "IN-MH",
                "TXN-2026-784102", new BigDecimal("48750.00"), "INR",
                OffsetDateTime.parse("2026-11-06T08:42:00+05:30"),
                TransactionStatus.POSTED, TxnChannel.CARD_NOT_PRESENT,
                "NORTHSTAR DIGITAL ••••73",
                IssueType.MERCHANT_NOT_RECOGNISED,
                "I do not recognise this purchase and still have my card.",
                true, false, true, true);
    }

    public static CaseIntakeRequest merchantDisputeCase() {
        return new CaseIntakeRequest(
                "CUST-••••-1111", "IN-DL",
                "TXN-2026-000111", new BigDecimal("2500.00"), "INR",
                OffsetDateTime.parse("2026-11-01T10:00:00+05:30"),
                TransactionStatus.POSTED, TxnChannel.ONLINE,
                "WELLKNOWN SHOP ••••11",
                IssueType.FULFILMENT_ISSUE,
                "I ordered goods that never arrived and want a refund.",
                true, true, true, true);
    }

    public static CaseIntakeRequest duplicateCase() {
        return new CaseIntakeRequest(
                "CUST-••••-2222", "IN-KA",
                "TXN-DUP-900001", new BigDecimal("999.00"), "INR",
                OffsetDateTime.parse("2026-11-02T12:00:00+05:30"),
                TransactionStatus.POSTED, TxnChannel.CARD_PRESENT,
                "LOCAL MART ••••22",
                IssueType.DUPLICATE_CHARGE,
                "I was charged twice for the same purchase.",
                true, true, true, true);
    }
}
