package org.example.documind.dto.chat;

import java.util.List;

public record RagAnswer(
        String answer,
        List<CitedSource> sources
) {}
