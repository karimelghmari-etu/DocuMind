package org.example.documind.llm;

import org.example.documind.domain.enums.MessageRole;
import org.example.documind.service.interfaces.LLMService;
import org.example.documind.service.llm.ChatTurn;
import org.example.documind.service.llm.OllamaLLMService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OllamaLLMServiceManualTest {

    @Test
    void ollamaAnswers() {
        assumeTrue(ollamaIsRunning(), "Ollama not running, skipping");

        LLMService ollama = new OllamaLLMService(
                "http://localhost:11434/v1/chat/completions",
                "qwen2.5:7b-instruct");
        String answer = ollama.generate("Answer in one short sentence.",
                List.of(new ChatTurn(MessageRole.USER, "What is the capital of Morocco?")));

        System.out.println(answer);
        assertFalse(answer.isBlank());
    }

    private boolean ollamaIsRunning() {
        try {
            RestClient.create().get().uri("http://localhost:11434/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}