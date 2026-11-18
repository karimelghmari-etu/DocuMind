package org.example.documind.llm;

import org.example.documind.dto.chat.RagAnswer;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.service.impl.RAGServiceImpl;
import org.example.documind.service.interfaces.LLMService;
import org.example.documind.service.interfaces.RAGService;
import org.example.documind.service.interfaces.SearchService;
import org.example.documind.service.llm.GroqLLMService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Manual check of the whole RAG flow against the real Groq API.
 * Search is faked (the handbook is returned as one big "chunk"), so no database is needed.
 * Skipped when GROQ_API_KEY is not set, so normal test runs never call Groq.
 */
class RAGServiceManualTest {

    @Test
    void answersWithCitationsAndRefusesUnknownQuestions() throws IOException {
        String apiKey = System.getenv("GROQ_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GROQ_API_KEY not set, skipping");

        String handbook = Files.readString(Path.of("src/test/resources/eval/employee-handbook.txt"));

        // SearchService has a single method, so a lambda can play the role of the real search.
        SearchService fakeSearch = (query, limit, ownerId) -> List.of(
                new SearchResultDTO(1L, 1L, "Employee Handbook", 0, handbook, 0.8));

        LLMService groq = new GroqLLMService(
                "https://api.groq.com/openai/v1/chat/completions",
                "qwen/qwen3.8-27b",
                apiKey);

        RAGService rag = new RAGServiceImpl(fakeSearch, groq, 5, 0.3);

        // 1. The answer is in the handbook: expect a cited answer.
        RagAnswer known = rag.answer("How many vacation days do I get?", List.of(), 1L);
        System.out.println("KNOWN:   " + known.answer());
        System.out.println("SOURCES: " + known.sources().stream().map(s -> s.number()).toList());

        // 2. The answer is NOT in the handbook: expect the "could not find" sentence.
        RagAnswer unknown = rag.answer("What is the CEO's favourite colour?", List.of(), 1L);
        System.out.println("UNKNOWN: " + unknown.answer());
        System.out.println("SOURCES: " + unknown.sources().stream().map(s -> s.number()).toList());

        assertFalse(known.sources().isEmpty(), "the known answer should cite at least one source");
        assertTrue(known.answer().contains("25"), "the handbook says 25 days");
        assertFalse(known.answer().contains("<think>"), "reasoning text leaked into the answer");
        assertTrue(unknown.sources().isEmpty(), "an unknown answer should cite nothing");
    }
}
