package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Declarative LangChain4j AI Service. Structured output is mapped onto the
 * shared {@link FraudCaseRecommendation} contract; follow-up chat memory is
 * keyed by {@code caseId} via {@link MemoryId}.
 */
public interface FraudInvestigationAiService {

    @SystemMessage(fromResource = "prompts/fraud-system.txt")
    FraudCaseRecommendation analyze(@UserMessage String casePrompt);

    @SystemMessage(fromResource = "prompts/fraud-system.txt")
    String chat(@MemoryId String caseId, @UserMessage String question);
}
