package com.capstone.fraudcase.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * LangChain4j embedding-store wiring backed by the deterministic hashing
 * embedder. Replace {@link HashingLc4jEmbeddingModel} with a provider
 * embedding model for production-like runs.
 */
@Configuration
public class LangChain4jEmbeddingConfig {

    @Bean
    EmbeddingModel lc4jEmbeddingModel(HashingEmbedder embedder) {
        return new HashingLc4jEmbeddingModel(embedder);
    }

    @Bean
    EmbeddingStore<TextSegment> lc4jEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    static class HashingLc4jEmbeddingModel implements EmbeddingModel {

        private final HashingEmbedder embedder;

        HashingLc4jEmbeddingModel(HashingEmbedder embedder) {
            this.embedder = embedder;
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            List<Embedding> embeddings = textSegments.stream()
                    .map(s -> Embedding.from(embedder.embed(s.text())))
                    .toList();
            return Response.from(embeddings);
        }
    }
}
