package com.capstone.fraudcase.domain.dto;

public record PolicyCitation(
        String documentId,
        String version,
        String section,
        String chunkId,
        double score) {

    public static PolicyCitation from(PolicyPassage p) {
        return new PolicyCitation(p.documentId(), p.version(), p.section(), p.chunkId(), p.score());
    }
}
