package com.capstone.fraudcase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "capstone")
public record CapstoneProperties(
        String processorMode,
        boolean humanReviewRequired,
        boolean allowFinancialActions,
        boolean allowCustomerContact,
        Llm llm,
        Retrieval retrieval,
        Memory memory,
        Tools tools) {

    public CapstoneProperties {
        if (llm == null) llm = new Llm("demo", "");
        if (retrieval == null) retrieval = new Retrieval(4, 0.25);
        if (memory == null) memory = new Memory(12);
        if (tools == null) tools = new Tools(2500, "fraud-training-v1");
    }

    public record Llm(String provider, String apiKey) {
    }

    public record Retrieval(int topK, double similarityThreshold) {
    }

    public record Memory(int maxMessages) {
    }

    public record Tools(long timeoutMs, String deterministicSeed) {
    }
}
