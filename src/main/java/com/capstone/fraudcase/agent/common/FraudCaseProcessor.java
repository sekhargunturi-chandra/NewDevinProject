package com.capstone.fraudcase.agent.common;

import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;

/**
 * Framework-neutral processor contract. The domain, tools, persistence and
 * output schema are identical for every implementation.
 */
public interface FraudCaseProcessor {

    /** rules | spring-ai | langchain4j */
    String mode();

    FraudCaseRecommendation analyze(FraudCaseContext context);

    /** Case-scoped follow-up. Must not bypass the recommendation contract. */
    String ask(FraudCaseContext context, String question);
}
