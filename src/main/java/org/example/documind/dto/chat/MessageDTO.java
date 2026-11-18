package org.example.documind.dto.chat;

import org.example.documind.domain.enums.MessageRole;

import java.time.LocalDateTime;
import java.util.List;

public record MessageDTO(
        Long id,
        MessageRole role,
        String content,
        LocalDateTime createdAt,
        List<SourceDTO> sources
) {}
