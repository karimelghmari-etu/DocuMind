package org.example.documind.llm;

import org.example.documind.service.interfaces.LLMService;
import org.example.documind.service.llm.ChatTurn;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "fake")
public class FakeLLMService implements LLMService {
    @Override
    public String generate(String systemPrompt, List<ChatTurn> conversation) {
        return "Fake answer [1].";
    }
}
