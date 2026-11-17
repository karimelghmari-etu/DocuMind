package org.example.documind.service.impl;

import lombok.AllArgsConstructor;
import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.service.interfaces.EmbeddingService;
import org.example.documind.service.interfaces.SearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    @Override
    @Transactional(readOnly = true)
    public List<SearchResultDTO> search(String query, int limit, Long ownerId) {
        float[] queryVector = embeddingService.embed(query);

        List<DocumentChunk> chunks = documentChunkRepository.findSearchableChunks(ownerId, DocumentStatus.INDEXED);

        return chunks.stream().map(chunk -> new SearchResultDTO(
                chunk.getDocument().getId(),
                chunk.getDocument().getTitle(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                cosineSimilarity(queryVector, chunk.getEmbedding())))
                .sorted(Comparator.comparingDouble(SearchResultDTO::score).reversed())
                .limit(limit).toList();
    }

    private static double cosineSimilarity(float[] a, float[] b){
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
