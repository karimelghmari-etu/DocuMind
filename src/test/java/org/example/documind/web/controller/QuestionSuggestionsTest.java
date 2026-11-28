package org.example.documind.web.controller;

import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionSuggestionsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);

    private static DocumentDTO doc(long id, String title, DocumentStatus status, int daysAgo) {
        return new DocumentDTO(id, title, status, NOW.minusDays(daysAgo));
    }

    @Test
    void noDocumentsGivesNoSuggestions() {
        assertTrue(QuestionSuggestions.from(List.of()).isEmpty());
    }

    @Test
    void onlyIndexedDocumentsAreUsed() {
        List<String> questions = QuestionSuggestions.from(List.of(
                doc(1, "Ready", DocumentStatus.INDEXED, 1),
                doc(2, "Still processing", DocumentStatus.PROCESSING, 0),
                doc(3, "Broken", DocumentStatus.FAILED, 0)));

        assertEquals(List.of("Summarize “Ready”"), questions);
    }

    @Test
    void newestFirstAtMostThreeWithDifferentWordings() {
        List<String> questions = QuestionSuggestions.from(List.of(
                doc(1, "Oldest", DocumentStatus.INDEXED, 9),
                doc(2, "Newest", DocumentStatus.INDEXED, 0),
                doc(3, "Middle", DocumentStatus.INDEXED, 3),
                doc(4, "Second", DocumentStatus.INDEXED, 1)));

        assertEquals(List.of(
                "Summarize “Newest”",
                "What are the key ideas in “Second”?",
                "Explain the main terms used in “Middle”"), questions);
    }

    @Test
    void blankTitlesAreSkippedAndTitlesAreTrimmed() {
        List<String> questions = QuestionSuggestions.from(List.of(
                doc(1, "   ", DocumentStatus.INDEXED, 0),
                doc(2, "  Neural networks ", DocumentStatus.INDEXED, 1)));

        assertEquals(List.of("Summarize “Neural networks”"), questions);
    }
}
