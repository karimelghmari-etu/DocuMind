package org.example.documind.dto.chat;

import jakarta.validation.constraints.Size;

public record ConversationRequestDTO(
        @Size(max = 255) String title
) {}
