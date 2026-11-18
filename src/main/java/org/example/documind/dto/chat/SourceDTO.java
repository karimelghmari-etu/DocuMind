package org.example.documind.dto.chat;

public record SourceDTO(
        int number,
        Long documentId,
        String documentTitle,
        int chunkIndex,
        String content,
        double score
) {}
