package org.example.documind.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.example.documind.service.interfaces.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    private final EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();

    @Override
    public float[] embed(String text) {
        return model.embed(text).content().vector();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()){
            return List.of();
        }
        List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
        return model.embedAll(segments).content().stream().map(Embedding::vector).toList();
    }
}
