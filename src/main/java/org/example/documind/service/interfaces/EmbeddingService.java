package org.example.documind.service.interfaces;

import java.util.List;

public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> embedAll(List<String> texts);
}
