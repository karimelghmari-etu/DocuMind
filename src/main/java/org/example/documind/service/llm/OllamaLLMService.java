package org.example.documind.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "ollama")
public class OllamaLLMService extends OpenAiCompatibleLLMService {
    public OllamaLLMService(@Value("${app.llm.ollama.url}") String url,
                          @Value("${app.llm.ollama.model}") String model) {
        super("Ollama", url, model, null);
    }
}
