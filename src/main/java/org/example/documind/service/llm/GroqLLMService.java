package org.example.documind.service.llm;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "groq")
public class GroqLLMService extends OpenAiCompatibleLLMService {
    public GroqLLMService(@Value("${app.llm.groq.url}") String url,
                          @Value("${app.llm.groq.model}") String model,
                          @Value("${app.llm.groq.api-key}") String apiKey) {
        super("Groq", url, model, apiKey);
    }
}
