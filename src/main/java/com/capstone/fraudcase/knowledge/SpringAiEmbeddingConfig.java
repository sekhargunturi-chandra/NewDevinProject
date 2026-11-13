package com.capstone.fraudcase.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI vector-store wiring backed by the deterministic hashing embedder.
 * Replace {@link HashingSpringAiEmbeddingModel} with a provider embedding
 * model for production-like runs - nothing else changes.
 */
@Configuration
public class SpringAiEmbeddingConfig {

    @Bean
    EmbeddingModel embeddingModel(HashingEmbedder embedder) {
        return new HashingSpringAiEmbeddingModel(embedder);
    }

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    static class HashingSpringAiEmbeddingModel implements EmbeddingModel {

        private final HashingEmbedder embedder;

        HashingSpringAiEmbeddingModel(HashingEmbedder embedder) {
            this.embedder = embedder;
        }

        @Override
        public float[] embed(Document document) {
            return embedder.embed(document.getText());
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            int i = 0;
            for (String instruction : request.getInstructions()) {
                embeddings.add(new Embedding(embedder.embed(instruction), i++));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public int dimensions() {
            return HashingEmbedder.DIMENSIONS;
        }
    }
}
