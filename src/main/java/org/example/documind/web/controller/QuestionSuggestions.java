package org.example.documind.web.controller;

import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Example questions shown on the Dashboard and in an empty Assistant chat,
 * built from the user's own indexed documents so they always have an answer.
 */
final class QuestionSuggestions {

    static final int MAX = 3;

    private static final String[] TEMPLATES = {
            "Summarize “%s”",
            "What are the key ideas in “%s”?",
            "Explain the main terms used in “%s”",
    };

    private QuestionSuggestions() {
    }

    /** Newest indexed documents first; one question each, with a different wording for each. */
    static List<String> from(List<DocumentDTO> documents) {
        List<DocumentDTO> indexed = documents.stream()
                .filter(d -> d.getStatus() == DocumentStatus.INDEXED && d.getTitle() != null && !d.getTitle().isBlank())
                .sorted(Comparator.comparing(DocumentDTO::getUploadedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .limit(MAX)
                .toList();

        List<String> questions = new ArrayList<>();
        for (int i = 0; i < indexed.size(); i++) {
            questions.add(String.format(TEMPLATES[i % TEMPLATES.length], indexed.get(i).getTitle().strip()));
        }
        return questions;
    }
}
