package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects the active processor and policy retriever from configuration.
 * Rules mode is always available and never depends on a model.
 */
@Component
public class ProcessorSelector {

    private final CapstoneProperties properties;
    private final Map<String, FraudCaseProcessor> processors;
    private final Map<String, PolicyRetriever> retrievers;

    public ProcessorSelector(CapstoneProperties properties,
                             List<FraudCaseProcessor> processors,
                             List<PolicyRetriever> retrievers) {
        this.properties = properties;
        this.processors = processors.stream()
                .collect(Collectors.toMap(FraudCaseProcessor::mode, Function.identity()));
        this.retrievers = retrievers.stream()
                .collect(Collectors.toMap(PolicyRetriever::mode, Function.identity()));
    }

    public String activeMode() {
        String mode = properties.processorMode();
        return processors.containsKey(mode) ? mode : "rules";
    }

    public FraudCaseProcessor processor() {
        return processors.getOrDefault(activeMode(), processors.get("rules"));
    }

    public FraudCaseProcessor rulesProcessor() {
        return processors.get("rules");
    }

    public PolicyRetriever retriever() {
        return switch (activeMode()) {
            case "spring-ai" -> retrievers.get("spring-ai");
            case "langchain4j" -> retrievers.get("langchain4j");
            default -> retrievers.get("local");
        };
    }
}
