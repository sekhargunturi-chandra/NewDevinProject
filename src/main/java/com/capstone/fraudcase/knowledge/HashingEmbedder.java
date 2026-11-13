package com.capstone.fraudcase.knowledge;

import com.capstone.fraudcase.util.Hashes;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Deterministic, dependency-free embedding used for the training corpus so the
 * whole pipeline runs offline and retrieval is reproducible. Swapping in a
 * real provider embedding model only requires replacing the adapter beans.
 */
@Component
public class HashingEmbedder {

    public static final int DIMENSIONS = 256;

    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text != null) {
            for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (token.isBlank()) {
                    continue;
                }
                int idx = (int) (Hashes.stableLong("tok:" + token) % DIMENSIONS);
                vector[idx] += 1.0f;
                // character-trigram-ish signal for partial matches
                if (token.length() > 4) {
                    vector[(int) (Hashes.stableLong("tri:" + token.substring(0, 4)) % DIMENSIONS)] += 0.5f;
                }
            }
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }
        return vector;
    }

    public static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
        }
        return dot; // vectors are L2-normalised
    }
}
