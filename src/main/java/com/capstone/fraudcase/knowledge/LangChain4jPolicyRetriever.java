package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.domain.dto.PolicyPassage;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Policy retrieval through the LangChain4j {@link EmbeddingStore} abstraction,
 * indexed with the same deterministic embedder for parity with other modes.
 */
@Component
public class LangChain4jPolicyRetriever implements PolicyRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    public LangChain4jPolicyRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                      dev.langchain4j.model.embedding.EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public String mode() {
        return "langchain4j";
    }

    @Override
    public void index(List<PolicyChunk> chunks) {
        for (PolicyChunk c : chunks) {
            Map<String, String> meta = new HashMap<>();
            meta.put("documentId", c.documentId());
            meta.put("title", c.title());
            meta.put("version", c.version());
            meta.put("effectiveDate", c.effectiveDate());
            meta.put("section", c.section());
            meta.put("chunkId", c.chunkId());
            TextSegment segment = TextSegment.from(c.text(), dev.langchain4j.data.document.Metadata.from(meta));
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
    }

    @Override
    public List<PolicyPassage> retrieve(String query, int topK, double similarityThreshold) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(topK)
                        .minScore(similarityThreshold)
                        .build())
                .matches();
        return matches.stream().map(this::toPassage).toList();
    }

    private PolicyPassage toPassage(EmbeddingMatch<TextSegment> match) {
        var meta = match.embedded().metadata();
        return new PolicyPassage(
                meta.getString("documentId"), meta.getString("title"), meta.getString("version"),
                meta.getString("effectiveDate"), meta.getString("section"), meta.getString("chunkId"),
                match.score(), match.embedded().text());
    }
}
