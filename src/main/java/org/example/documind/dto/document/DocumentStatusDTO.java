package org.example.documind.dto.document;

import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.domain.enums.ProcessingStatus;

import java.time.LocalDateTime;

public record DocumentStatusDTO(
        Long documentId,
        DocumentStatus documentStatus,
        ProcessingStatus jobStatus,
        String errorMessage,
        long chunkCount,
        LocalDateTime updatedAt
    ) {}
