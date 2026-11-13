package com.capstone.fraudcase;

import com.capstone.fraudcase.application.CaseMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T10 - a fact introduced in Case A must never be recalled in Case B.
 */
@SpringBootTest
class MemoryIsolationTest {

    @Autowired
    CaseMemoryService memoryService;

    @Test
    void caseMemoryIsIsolatedByCaseId() {
        memoryService.append("CASE-A", "USER", "secret marker alpha-123");
        memoryService.append("CASE-A", "ASSISTANT", "acknowledged");

        assertThat(memoryService.renderContext("CASE-A")).contains("alpha-123");
        assertThat(memoryService.renderContext("CASE-B")).doesNotContain("alpha-123");
        assertThat(memoryService.history("CASE-B")).isEmpty();
    }

    @Test
    void windowIsBounded() {
        for (int i = 0; i < 20; i++) {
            memoryService.append("CASE-C", "USER", "message " + i);
        }
        assertThat(memoryService.history("CASE-C")).hasSizeLessThanOrEqualTo(12);
    }
}
