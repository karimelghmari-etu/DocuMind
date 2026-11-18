package org.example.documind.service.interfaces;

import org.example.documind.dto.chat.RagAnswer;
import org.example.documind.service.llm.ChatTurn;

import java.util.List;

public interface RAGService {
    RagAnswer answer(String question, List<ChatTurn> history, Long ownerId);
}
