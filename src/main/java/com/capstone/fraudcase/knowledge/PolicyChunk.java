package com.capstone.fraudcase.knowledge;

public record PolicyChunk(
        String documentId,
        String title,
        String version,
        String effectiveDate,
        String owner,
        String section,
        String chunkId,
        String text) {
}
