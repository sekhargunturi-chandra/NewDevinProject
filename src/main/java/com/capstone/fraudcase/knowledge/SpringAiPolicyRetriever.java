package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.domain.dto.PolicyPassage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Policy retrieval through the Spring AI {@link VectorStore} abstraction.
 * Uses the same deterministic embedding as the other retrievers so all modes
 * return identical citations.
 */
@Component
public class SpringAiPolicyRetriever implements PolicyRetriever {

    private final VectorStore vectorStore;

    public SpringAiPolicyRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String mode() {
        return "spring-ai";
    }

    @Override
    public void index(List<PolicyChunk> chunks) {
        List<Document> docs = chunks.stream()
                .map(c -> Document.builder()
                        .id(c.chunkId())
                        .text(c.text())
                        .metadata(metadata(c))
                        .build())
                .toList();
        vectorStore.add(docs);
    }

    @Override
    public List<PolicyPassage> retrieve(String query, int topK, double similarityThreshold) {
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build());
        return docs.stream().map(this::toPassage).toList();
    }

    private PolicyPassage toPassage(Document doc) {
        Map<String, Object> m = doc.getMetadata();
        double score = doc.getScore() == null ? 0.0 : doc.getScore();
        return new PolicyPassage(
                str(m, "documentId"), str(m, "title"), str(m, "version"),
                str(m, "effectiveDate"), str(m, "section"), str(m, "chunkId"),
                score, doc.getText());
    }

    private Map<String, Object> metadata(PolicyChunk c) {
        Map<String, Object> m = new HashMap<>();
        m.put("documentId", c.documentId());
        m.put("title", c.title());
        m.put("version", c.version());
        m.put("effectiveDate", c.effectiveDate());
        m.put("section", c.section());
        m.put("chunkId", c.chunkId());
        return m;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }
}
