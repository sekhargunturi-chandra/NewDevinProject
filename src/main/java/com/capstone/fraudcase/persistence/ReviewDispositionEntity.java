package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.RecommendedRoute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "review_disposition")
public class ReviewDispositionEntity {

    @Id
    private String reviewId;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disposition disposition;

    @Enumerated(EnumType.STRING)
    private RecommendedRoute rerouteTo;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private Instant recordedAt = Instant.now();

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public void setDisposition(Disposition disposition) {
        this.disposition = disposition;
    }

    public RecommendedRoute getRerouteTo() {
        return rerouteTo;
    }

    public void setRerouteTo(RecommendedRoute rerouteTo) {
        this.rerouteTo = rerouteTo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
