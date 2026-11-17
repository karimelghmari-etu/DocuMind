package org.example.documind.service.util;

public final class VectorMath {
    private VectorMath(){}

    public static double cosineSimilarity(float[] a, float[] b){
        if (a.length != b.length){
            throw new IllegalArgumentException("Vectors have different sizes");
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
