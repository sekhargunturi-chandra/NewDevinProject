package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.EvidenceType;
import com.capstone.fraudcase.domain.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "evidence_item")
public class EvidenceItemEntity {

    @Id
    private String evidenceId;

    @Column(nullable = false)
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.INFO;

    @Column(length = 2000)
    private String valueSummary;

    @Column(nullable = false)
    private String provenance; // INTAKE | TOOL:<name> | POLICY | MODEL | REVIEWER

    private String toolTraceId;

    @Column(nullable = false)
    private Instant observedAt = Instant.now();

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public EvidenceType getType() {
        return type;
    }

    public void setType(EvidenceType type) {
        this.type = type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getValueSummary() {
        return valueSummary;
    }

    public void setValueSummary(String valueSummary) {
        this.valueSummary = valueSummary;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getToolTraceId() {
        return toolTraceId;
    }

    public void setToolTraceId(String toolTraceId) {
        this.toolTraceId = toolTraceId;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }
}
