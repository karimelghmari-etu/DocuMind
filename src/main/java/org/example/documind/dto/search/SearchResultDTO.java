package org.example.documind.dto.search;

public record SearchResultDTO(
        Long documentId,
        String documentTitle,
        int chunkIndex,
        String content,
        double score
) {}
