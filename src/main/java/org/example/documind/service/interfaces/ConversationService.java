package org.example.documind.service.interfaces;

import org.example.documind.dto.chat.ConversationDTO;
import org.example.documind.dto.chat.ConversationDetailDTO;
import org.example.documind.dto.chat.ConversationRequestDTO;
import org.example.documind.dto.chat.MessageDTO;

import java.util.List;

public interface ConversationService {
    ConversationDTO createConversation(ConversationRequestDTO request, Long userId);
    List<ConversationDTO> getConversations(Long userId);
    ConversationDetailDTO getConversation(Long conversationId, Long userId);
    MessageDTO sendMessage(Long conversationId, String question, Long userId);
    void deleteConversation(Long conversationId, Long userId);
}
