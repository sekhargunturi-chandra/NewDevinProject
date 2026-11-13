package com.capstone.fraudcase.domain.dto;

import java.time.Instant;
import java.util.Map;

/**
 * One allow-listed tool outcome with provenance and a redacted result summary.
 */
public record ToolEvidence(
        String evidenceId,
        String traceId,
        String toolName,
        String status, // OK | UNAVAILABLE | NOT_FOUND | TIMEOUT
        long durationMs,
        Map<String, Object> result,
        Instant observedAt) {

    public boolean ok() {
        return "OK".equals(status);
    }
}
