package org.example.documind.service.llm;

import org.example.documind.domain.enums.MessageRole;

public record ChatTurn(
        MessageRole role,
        String content
) {}
