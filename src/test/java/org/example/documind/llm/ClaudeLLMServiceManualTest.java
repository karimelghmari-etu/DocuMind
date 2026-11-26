package org.example.documind.llm;

import org.example.documind.domain.enums.MessageRole;
import org.example.documind.service.interfaces.LLMService;
import org.example.documind.service.llm.ChatTurn;
import org.example.documind.service.llm.ClaudeLLMService;
import org.example.documind.web.exception.LLMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Calls the real Claude API. Only for learning how the Anthropic SDK works:
 * skipped unless ANTHROPIC_API_KEY is set, and each run costs a little money.
 */
class ClaudeLLMServiceManualTest {

    private static final String MODEL = "claude-opus-5-5";

    private String apiKey;

    @BeforeEach
    void requireApiKey() {
        apiKey = System.getenv("ANTHROPIC_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "ANTHROPIC_API_KEY is not set");
    }

    @Test
    void claudeAnswers() {
        LLMService claude = new ClaudeLLMService(MODEL, apiKey);

        String answer = claude.generate("Answer in one short sentence.",
                List.of(new ChatTurn(MessageRole.USER, "What is the capital of Morocco?")));

        System.out.println(answer);
        assertTrue(answer.contains("Rabat"));
    }

    @Test
    void claudeUsesTheHistory() {
        LLMService claude = new ClaudeLLMService(MODEL, apiKey);

        String answer = claude.generate("Answer in one short sentence.", List.of(
                new ChatTurn(MessageRole.USER, "My favourite city is Fes. Remember it."),
                new ChatTurn(MessageRole.ASSISTANT, "Got it, your favourite city is Fes."),
                new ChatTurn(MessageRole.USER, "What is my favourite city?")));

        System.out.println(answer);
        assertTrue(answer.contains("Fes"));
    }

    @Test
    void wrongModelBecomesLlmException() {
        LLMService claude = new ClaudeLLMService("claude-does-not-exist", apiKey);

        LLMException e = assertThrows(LLMException.class, () -> claude.generate("Answer briefly.",
                List.of(new ChatTurn(MessageRole.USER, "Hello"))));

        System.out.println(e.getMessage() + " <- caused by: " + e.getCause());
        assertEquals("Claude request failed", e.getMessage());
        assertNotNull(e.getCause(), "the real error must be kept for the log");
    }
}
