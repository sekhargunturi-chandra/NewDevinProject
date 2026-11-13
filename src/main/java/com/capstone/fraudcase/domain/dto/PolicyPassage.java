package com.capstone.fraudcase.domain.dto;

public record PolicyPassage(
        String documentId,
        String title,
        String version,
        String effectiveDate,
        String section,
        String chunkId,
        double score,
        String passage) {
}
