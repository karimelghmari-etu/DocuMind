package org.example.documind.service.interfaces;

import org.example.documind.service.llm.ChatTurn;

import java.util.List;

public interface LLMService {
    String generate(String systemPrompt, List<ChatTurn> conversation);
}
