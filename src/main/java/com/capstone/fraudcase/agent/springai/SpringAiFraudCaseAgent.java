package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.agent.common.ModelOutputNormalizer;
import com.capstone.fraudcase.agent.common.RecommendationValidator;
import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.tools.EvidenceToolset;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Spring AI implementation of the {@link FraudCaseProcessor} contract:
 * ChatClient + prompt templates + typed structured output, then application
 * validation. Tool results and citations are supplied by the orchestrator and
 * re-attached by {@link ModelOutputNormalizer} so they cannot be invented.
 */
@Component
public class SpringAiFraudCaseAgent implements FraudCaseProcessor {

    private final ChatClient chatClient;
    private final RecommendationValidator validator;
    private final ObjectMapper objectMapper;
    private final EvidenceToolset evidenceTools;
    private final ChatMemory caseChatMemory;
    private final Resource systemPrompt;
    private final Resource userPrompt;

    public SpringAiFraudCaseAgent(ChatClient fraudChatClient,
                                RecommendationValidator validator,
                                ObjectMapper objectMapper,
                                EvidenceToolset evidenceTools,
                                ChatMemory caseChatMemory,
                                @Value("classpath:prompts/fraud-system.txt") Resource systemPrompt,
                                @Value("classpath:prompts/fraud-user.txt") Resource userPrompt) {
        this.chatClient = fraudChatClient;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.evidenceTools = evidenceTools;
        this.caseChatMemory = caseChatMemory;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    @Override
    public String mode() {
        return "spring-ai";
    }

    @Override
    public FraudCaseRecommendation analyze(FraudCaseContext ctx) {
        try {
            String system = systemPrompt.getContentAsString(StandardCharsets.UTF_8);
            String user = userPrompt.getContentAsString(StandardCharsets.UTF_8);
            Map<String, Object> params = promptParams(ctx);
            FraudCaseRecommendation model = chatClient.prompt()
                    .system(system)
                    .user(u -> u.text(user).params(params))
                    .tools(evidenceTools)
                    .call()
                    .entity(FraudCaseRecommendation.class);
            return validator.requireValid(ModelOutputNormalizer.normalize(ctx, model));
        } catch (Exception e) {
            throw new SpringAiProcessingException("spring-ai analysis failed", e);
        }
    }

    @Override
    public String ask(FraudCaseContext ctx, String question) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                    .advisors(MessageChatMemoryAdvisor.builder(caseChatMemory).build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ctx.caseId()))
                    .user("FOLLOW_UP_QUESTION caseId=" + ctx.caseId()
                            + "\ncontext=" + ctx.conversationContext()
                            + "\nevidence=" + ctx.toolEvidence()
                            + "\nquestion=" + question)
                    .call()
                    .content();
        } catch (Exception e) {
            throw new SpringAiProcessingException("spring-ai follow-up failed", e);
        }
    }

    private Map<String, Object> promptParams(FraudCaseContext ctx) throws Exception {
        return Map.of(
                "caseId", ctx.caseId(),
                "caseSummary", objectMapper.writeValueAsString(ctx.intake()),
                "investigationGoal", ctx.investigationGoal(),
                "toolEvidence", objectMapper.writeValueAsString(ctx.toolEvidence()),
                "policyContext", objectMapper.writeValueAsString(ctx.policyContext()),
                "policyPassageCount", String.valueOf(ctx.policyContext().size()),
                "conversationContext", ctx.conversationContext() == null ? "" : ctx.conversationContext());
    }

    static class SpringAiProcessingException extends RuntimeException {
        SpringAiProcessingException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
