package org.example.documind.service.impl;

import lombok.AllArgsConstructor;
import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.service.interfaces.EmbeddingService;
import org.example.documind.service.interfaces.SearchService;
import org.example.documind.service.util.VectorMath;
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
                chunk.getContent(), VectorMath.cosineSimilarity(queryVector, chunk.getEmbedding())))
                .sorted(Comparator.comparingDouble(SearchResultDTO::score).reversed())
                .limit(limit).toList();
    }
}
