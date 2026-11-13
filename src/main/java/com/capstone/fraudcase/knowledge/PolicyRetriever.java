package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.domain.dto.PolicyPassage;

import java.util.List;

/**
 * Framework-neutral policy retrieval contract. Implementations exist for the
 * local deterministic store, the Spring AI vector store and the LangChain4j
 * embedding store; the active one is chosen by {@code capstone.processor-mode}.
 */
public interface PolicyRetriever {

    String mode();

    /** Indexes the shared policy chunks into this retriever's store. */
    void index(List<PolicyChunk> chunks);

    /**
     * Returns passages at or above the configured similarity threshold.
     * An empty list means abstain - never fabricate policy guidance.
     */
    List<PolicyPassage> retrieve(String query, int topK, double similarityThreshold);
}
