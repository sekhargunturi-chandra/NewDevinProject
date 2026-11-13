package com.capstone.fraudcase.agent.springai;

import com.capstone.fraudcase.agent.common.DemoRecommendationHeuristics;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.tools.EvidenceToolset;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Spring AI wiring. The demo {@link ChatModel} returns deterministic JSON so
 * the spring-ai mode runs fully offline; it also exercises the real tool-call
 * loop through {@link ToolCallingManager} — first turn requests
 * {@code getDeviceSignals}, the manager executes the {@code @Tool} method, and
 * the second turn produces the recommendation. Replace the ChatModel bean with
 * a provider model for production-like runs.
 */
@Configuration
public class SpringAiConfig {

    @Bean
    ChatModel demoSpringChatModel(EvidenceToolset evidenceTools) {
        ToolCallback[] callbacks = ToolCallbacks.from(evidenceTools);
        return new ChatModel() {
            private final ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                    .toolCallbackResolver(name -> Arrays.stream(callbacks)
                            .filter(cb -> cb.getToolDefinition().name().equals(name))
                            .findFirst().orElse(null))
                    .build();

            @Override
            public ChatResponse call(Prompt prompt) {
                ChatResponse response = generate(prompt.getInstructions());
                // Mirror provider ChatModel behaviour: execute tool calls and
                // re-invoke with the tool response appended to the history.
                int guard = 0;
                while (response.hasToolCalls() && guard++ < 5) {
                    ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);
                    if (result.returnDirect()) {
                        return ChatResponse.builder()
                                .generations(ToolExecutionResult.buildGenerations(result))
                                .build();
                    }
                    prompt = new Prompt(result.conversationHistory(), prompt.getOptions());
                    response = generate(prompt.getInstructions());
                }
                return response;
            }

            private ChatResponse generate(List<Message> instructions) {
                boolean toolExecuted = instructions.stream()
                        .anyMatch(m -> m instanceof ToolResponseMessage);
                String text = instructions.stream()
                        .map(Message::getText)
                        .reduce("", (a, b) -> a + "\n" + b);
                if (text.contains("FOLLOW_UP_QUESTION")) {
                    return response(DemoRecommendationHeuristics.followUpAnswer(text));
                }
                if (toolExecuted) {
                    return response(DemoRecommendationHeuristics.recommendationJson(text));
                }
                // First turn: ask for evidence through the real tool-calling path.
                AssistantMessage toolCall = new AssistantMessage("", Map.of(),
                        List.of(new AssistantMessage.ToolCall("tc-1", "function", "getDeviceSignals",
                                "{\"maskedCustomerRef\":\"CUST-DEMO\",\"transactionRef\":\"TXN-DEMO\"}")));
                return new ChatResponse(List.of(new Generation(toolCall)));
            }
        };
    }

    private static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Bean
    ChatClient fraudChatClient(ChatModel demoSpringChatModel) {
        return ChatClient.builder(demoSpringChatModel).build();
    }

    /** Case-keyed, bounded framework memory for follow-up conversations. */
    @Bean
    ChatMemory caseChatMemory(CapstoneProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(properties.memory().maxMessages())
                .build();
    }
}
