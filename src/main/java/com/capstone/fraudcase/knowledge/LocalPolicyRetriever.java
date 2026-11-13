package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.domain.dto.PolicyPassage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Always-available deterministic retriever (used by rules mode and as a safe
 * fallback). Same thresholded citation contract as the framework retrievers.
 */
@Component
public class LocalPolicyRetriever implements PolicyRetriever {

    private final HashingEmbedder embedder;
    private final List<Indexed> index = new CopyOnWriteArrayList<>();

    private record Indexed(PolicyChunk chunk, float[] vector) {
    }

    public LocalPolicyRetriever(HashingEmbedder embedder) {
        this.embedder = embedder;
    }

    @Override
    public String mode() {
        return "local";
    }

    public void index(List<PolicyChunk> chunks) {
        index.clear();
        for (PolicyChunk c : chunks) {
            index.add(new Indexed(c, embedder.embed(c.section() + " " + c.text())));
        }
    }

    @Override
    public List<PolicyPassage> retrieve(String query, int topK, double similarityThreshold) {
        float[] q = embedder.embed(query);
        List<PolicyPassage> results = new ArrayList<>();
        for (Indexed idx : index) {
            double score = HashingEmbedder.cosine(q, idx.vector());
            if (score >= similarityThreshold) {
                PolicyChunk c = idx.chunk();
                results.add(new PolicyPassage(c.documentId(), c.title(), c.version(),
                        c.effectiveDate(), c.section(), c.chunkId(), score, c.text()));
            }
        }
        results.sort(Comparator.comparingDouble(PolicyPassage::score).reversed());
        return results.stream().limit(topK).toList();
    }
}
