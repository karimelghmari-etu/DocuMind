package org.example.documind.dto.chat;

import java.time.LocalDateTime;

public record ConversationDTO(
        Long id,
        String title,
        LocalDateTime createdAt
) {}
