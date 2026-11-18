package org.example.documind.dto.chat;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationDetailDTO(
        Long id,
        String title,
        LocalDateTime createdAt,
        List<MessageDTO> messages
) {}
