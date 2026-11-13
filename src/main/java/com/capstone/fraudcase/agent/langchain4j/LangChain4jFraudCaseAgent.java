package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.ModelOutputNormalizer;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * LangChain4j implementation of the {@link FraudCaseProcessor} contract via a
 * declarative AI Service returning the typed recommendation schema.
 */
@Component
public class LangChain4jFraudCaseAgent implements FraudCaseProcessor {

    private final FraudInvestigationAiService service;
    private final RecommendationValidator validator;
    private final ObjectMapper objectMapper;
    private final Resource userPrompt;

    public LangChain4jFraudCaseAgent(FraudInvestigationAiService service,
                                     RecommendationValidator validator,
                                     ObjectMapper objectMapper,
                                     @Value("classpath:prompts/fraud-user.txt") Resource userPrompt) {
        this.service = service;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.userPrompt = userPrompt;
    }

    @Override
    public String mode() {
        return "langchain4j";
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext ctx) {
        try {
            FraudCaseRecommendation model = service.analyze(renderUserPrompt(ctx));
            return validator.requireValid(ModelOutputNormalizer.normalize(ctx, model));
        } catch (Exception e) {
            throw new LangChain4jProcessingException("langchain4j analysis failed", e);
        }
    }

    @Override
    public String ask(FraudCaseContext ctx, String question) {
        try {
            return service.chat(ctx.caseId(),
                    "FOLLOW_UP_QUESTION caseId=" + ctx.caseId()
                            + "\ncontext=" + ctx.conversationContext()
                            + "\nevidence=" + ctx.toolEvidence()
                            + "\nquestion=" + question);
        } catch (Exception e) {
            throw new LangChain4jProcessingException("langchain4j follow-up failed", e);
        }
    }

    private String renderUserPrompt(FraudCaseContext ctx) throws Exception {
        String template = userPrompt.getContentAsString(StandardCharsets.UTF_8);
        return template
                .replace("{caseId}", ctx.caseId())
                .replace("{caseSummary}", objectMapper.writeValueAsString(ctx.intake()))
                .replace("{investigationGoal}", ctx.investigationGoal())
                .replace("{toolEvidence}", objectMapper.writeValueAsString(ctx.toolEvidence()))
                .replace("{policyContext}", objectMapper.writeValueAsString(ctx.policyContext()))
                .replace("{policyPassageCount}", String.valueOf(ctx.policyContext().size()))
                .replace("{conversationContext}", ctx.conversationContext() == null ? "" : ctx.conversationContext());
    }

    static class LangChain4jProcessingException extends RuntimeException {
        LangChain4jProcessingException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
