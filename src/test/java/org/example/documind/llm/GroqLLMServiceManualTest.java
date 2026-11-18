package org.example.documind.llm;

import org.example.documind.domain.enums.MessageRole;
import org.example.documind.service.interfaces.LLMService;
import org.example.documind.service.llm.ChatTurn;
import org.example.documind.service.llm.GroqLLMService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
class GroqLLMServiceManualTest {

    @Test
    void groqAnswers() {
        LLMService groq = new GroqLLMService(
                "https://api.groq.com/openai/v1/chat/completions",
                "qwen/qwen3.8-27b",
                System.getenv("GROQ_API_KEY"));

        String answer = groq.generate("Answer in one short sentence.",
                List.of(new ChatTurn(MessageRole.USER, "What is the number of atoms in the universe ?")));
        System.out.println(answer);
        assertFalse(answer.isBlank());
    }
}