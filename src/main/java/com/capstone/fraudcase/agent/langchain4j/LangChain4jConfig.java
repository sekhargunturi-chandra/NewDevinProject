package com.capstone.fraudcase.agent.langchain4j;

import com.capstone.fraudcase.agent.common.DemoRecommendationHeuristics;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.tools.EvidenceToolset;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LangChain4j wiring. The demo {@link ChatModel} returns deterministic JSON so
 * the langchain4j mode runs fully offline; it also exercises the real tool-call
 * loop (first turn emits a {@link ToolExecutionRequest}, the AI Service executes
 * the {@code @Tool} method and re-invokes the model). Replace the ChatModel
 * bean with a provider model for production-like runs.
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    ChatModel demoLc4jChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse chat(ChatRequest chatRequest) {
                String text = chatRequest.messages().stream()
                        .map(LangChain4jConfig::messageText)
                        .collect(Collectors.joining("\n"));
                boolean toolExecuted = chatRequest.messages().stream()
                        .anyMatch(m -> m instanceof ToolExecutionResultMessage);
                if (text.contains("FOLLOW_UP_QUESTION")) {
                    return respond(DemoRecommendationHeuristics.followUpAnswer(text));
                }
                if (toolExecuted) {
                    return respond(DemoRecommendationHeuristics.recommendationJson(text));
                }
                // First turn: request evidence through the real tool-call loop.
                AiMessage toolRequest = AiMessage.builder()
                        .toolExecutionRequests(List.of(ToolExecutionRequest.builder()
                                .id("tc-1")
                                .name("getDeviceSignals")
                                .arguments("{\"maskedCustomerRef\":\"CUST-DEMO\",\"transactionRef\":\"TXN-DEMO\"}")
                                .build()))
                        .build();
                return ChatResponse.builder().aiMessage(toolRequest).build();
            }
        };
    }

    private static ChatResponse respond(String text) {
        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }

    private static String messageText(ChatMessage message) {
        if (message instanceof UserMessage u) {
            return u.singleText();
        }
        if (message instanceof SystemMessage s) {
            return s.text();
        }
        if (message instanceof ToolExecutionResultMessage t) {
            return t.text();
        }
        return message.toString();
    }

    @Bean
    FraudInvestigationAiService fraudInvestigationAiService(ChatModel demoLc4jChatModel,
                                                          EvidenceToolset evidenceToolset,
                                                          CapstoneProperties properties) {
        return AiServices.builder(FraudInvestigationAiService.class)
                .chatModel(demoLc4jChatModel)
                .tools(evidenceToolset)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(properties.memory().maxMessages())
                        .build())
                .build();
    }
}
