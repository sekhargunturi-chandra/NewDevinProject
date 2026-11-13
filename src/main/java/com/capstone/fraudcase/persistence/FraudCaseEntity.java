package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.CaseStatus;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.TransactionStatus;
import com.capstone.fraudcase.domain.TxnChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "fraud_case")
public class FraudCaseEntity {

    @Id
    private String caseId;

    @Column(nullable = false)
    private String maskedCustomerRef;

    private String homeRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status = CaseStatus.NEW;

    @Column(nullable = false)
    private Instant reportedAt;

    @Column(nullable = false)
    private String analystId;

    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // Transaction claim (masked/synthetic values only)
    private String transactionRef;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    @Enumerated(EnumType.STRING)
    private TxnChannel transactionChannel;

    private String maskedMerchant;

    // Claim
    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @Column(length = 2000)
    private String customerStatement;

    private Boolean cardPossession;
    private Boolean merchantRecognised;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getMaskedCustomerRef() {
        return maskedCustomerRef;
    }

    public void setMaskedCustomerRef(String maskedCustomerRef) {
        this.maskedCustomerRef = maskedCustomerRef;
    }

    public String getHomeRegion() {
        return homeRegion;
    }

    public void setHomeRegion(String homeRegion) {
        this.homeRegion = homeRegion;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getAnalystId() {
        return analystId;
    }

    public void setAnalystId(String analystId) {
        this.analystId = analystId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public TxnChannel getTransactionChannel() {
        return transactionChannel;
    }

    public void setTransactionChannel(TxnChannel transactionChannel) {
        this.transactionChannel = transactionChannel;
    }

    public String getMaskedMerchant() {
        return maskedMerchant;
    }

    public void setMaskedMerchant(String maskedMerchant) {
        this.maskedMerchant = maskedMerchant;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public String getCustomerStatement() {
        return customerStatement;
    }

    public void setCustomerStatement(String customerStatement) {
        this.customerStatement = customerStatement;
    }

    public Boolean getCardPossession() {
        return cardPossession;
    }

    public void setCardPossession(Boolean cardPossession) {
        this.cardPossession = cardPossession;
    }

    public Boolean getMerchantRecognised() {
        return merchantRecognised;
    }

    public void setMerchantRecognised(Boolean merchantRecognised) {
        this.merchantRecognised = merchantRecognised;
    }
}
