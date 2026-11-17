package org.example.documind.service;

import org.example.documind.service.impl.EmbeddingServiceImpl;
import org.example.documind.service.interfaces.EmbeddingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain unit tests: no Spring context, the real local model is used.
 * The model is loaded once (static) because loading it takes about a second.
 */
class EmbeddingServiceImplTest {

    private static final EmbeddingService service = new EmbeddingServiceImpl();

    /** all-MiniLM-L6-v2 always produces 384 numbers. */
    private static final int DIMENSIONS = 384;

    /** Cosine similarity: 1 = same meaning, around 0 = unrelated. */
    private static double similarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ---------- shape ----------

    @Test
    void embedReturns384Numbers() {
        assertEquals(DIMENSIONS, service.embed("hello world").length);
    }

    @Test
    void sameTextGivesSameVector() {
        assertArrayEquals(service.embed("the same sentence"), service.embed("the same sentence"), 1e-6f);
    }

    // ---------- meaning ----------

    @Test
    void similarMeaningIsCloserThanUnrelatedText() {
        float[] question = service.embed("How do I reset my password?");
        float[] related = service.embed("Steps to recover access to your account when you forgot your login credentials.");
        float[] unrelated = service.embed("Preheat the oven and mix the bananas with flour to make banana bread.");

        double relatedScore = similarity(question, related);
        double unrelatedScore = similarity(question, unrelated);

        assertTrue(relatedScore > unrelatedScore,
                "related=" + relatedScore + " should be higher than unrelated=" + unrelatedScore);
    }

    @Test
    void identicalTextHasSimilarityOfOne() {
        float[] v = service.embed("DocuMind stores documents");

        assertEquals(1.0, similarity(v, v), 1e-5);
    }

    // ---------- embedAll ----------

    @Test
    void embedAllReturnsOneVectorPerTextInTheSameOrder() {
        List<String> texts = List.of("first chunk about cats", "second chunk about databases", "third chunk about music");

        List<float[]> vectors = service.embedAll(texts);

        assertEquals(texts.size(), vectors.size());
        for (int i = 0; i < texts.size(); i++) {
            assertEquals(DIMENSIONS, vectors.get(i).length);
            // batch result must match embedding the text on its own, so the order is right
            assertArrayEquals(service.embed(texts.get(i)), vectors.get(i), 1e-4f, "vector " + i + " is out of order");
        }
    }

    @Test
    void embedAllWithEmptyListReturnsEmptyList() {
        assertTrue(service.embedAll(List.of()).isEmpty());
    }

    @Test
    void embedAllWithNullReturnsEmptyList() {
        assertTrue(service.embedAll(null).isEmpty());
    }
}
