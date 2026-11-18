package org.example.documind.service.impl;

import org.example.documind.dao.ConversationRepository;
import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.dao.MessageRepository;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.*;
import org.example.documind.domain.enums.MessageRole;
import org.example.documind.dto.chat.*;
import org.example.documind.service.interfaces.ConversationService;
import org.example.documind.service.interfaces.RAGService;
import org.example.documind.service.llm.ChatTurn;
import org.example.documind.web.exception.ConversationNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final UserRepository userRepository;
    private final RAGService ragService;
    private final TransactionTemplate transactionTemplate;
    private final int historySize;

    private static final String DEFAULT_TITLE = "New conversation";

    public ConversationServiceImpl(@Value("${app.chat.history-size}") int historySize, TransactionTemplate transactionTemplate, RAGService ragService, UserRepository userRepository, DocumentChunkRepository documentChunkRepository, MessageRepository messageRepository, ConversationRepository conversationRepository) {
        this.historySize = historySize;
        this.transactionTemplate = transactionTemplate;
        this.ragService = ragService;
        this.userRepository = userRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    private ConversationDTO toConversationDTO(Conversation c) {
        return new ConversationDTO(c.getId(), c.getTitle(), c.getCreatedAt());
    }

    private SourceDTO toSourceDTO(SourceReference ref) {
        DocumentChunk chunk = ref.getChunk();
        return new SourceDTO(ref.getCitationNumber(), chunk.getDocument().getId(),
                chunk.getDocument().getTitle(), chunk.getChunkIndex(),
                chunk.getContent(), ref.getRelevanceScore());
    }

    private MessageDTO toMessageDTO(Message m) {
        List<SourceDTO> sources = m.getSourceReferences().stream()
                .map(this::toSourceDTO)
                .sorted(Comparator.comparingInt(SourceDTO::number))
                .toList();
        return new MessageDTO(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt(), sources);
    }

    @Override
    @Transactional
    public ConversationDTO createConversation(ConversationRequestDTO request, Long userId) {
        String title;
        User user = userRepository.getReferenceById(userId);

        if (request == null || request.title() == null || request.title().isBlank()){
            title = DEFAULT_TITLE;
        } else {
            title = request.title().strip();
        }

        Conversation conversation = Conversation.builder().title(title).user(user).build();
        conversationRepository.save(conversation);

        return toConversationDTO(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDTO> getConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toConversationDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailDTO getConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId).orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));
        List<MessageDTO> messages = messageRepository.findByConversationIdOrderByIdAsc(conversationId).stream().map(this::toMessageDTO).toList();
        return new ConversationDetailDTO(conversationId, conversation.getTitle(), conversation.getCreatedAt(), messages);
    }

    @Override
    public MessageDTO sendMessage(Long conversationId, String question, Long userId) {
        List<ChatTurn> history = transactionTemplate.execute(status -> {
            conversationRepository.findByIdAndUserId(conversationId, userId).orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));
            List<Message> previous = messageRepository.findByConversationIdOrderByIdAsc(conversationId);
            int from = Math.max(0, previous.size() - historySize);
            return previous.subList(from, previous.size()).stream()
                    .map(m -> new ChatTurn(m.getRole(), m.getContent()))
                    .toList();
        });

        RagAnswer answer = ragService.answer(question, history, userId);

        return transactionTemplate.execute(status -> {
            Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId).orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));

            Message userMessage = Message.builder()
                    .conversation(conversation).role(MessageRole.USER).content(question).build();
            Message assistantMessage = Message.builder()
                    .conversation(conversation).role(MessageRole.ASSISTANT).content(answer.answer()).build();

            for (CitedSource cited : answer.sources()) {
                assistantMessage.getSourceReferences().add(SourceReference.builder()
                        .message(assistantMessage)
                        .chunk(documentChunkRepository.getReferenceById(cited.source().chunkId()))
                        .citationNumber(cited.number())
                        .relevanceScore(cited.source().score())
                        .build());
            }

            if (history.isEmpty() && DEFAULT_TITLE.equals(conversation.getTitle())) {
                conversation.setTitle(question.length() > 60 ? question.substring(0, 60) + "..." : question);
            }
            messageRepository.save(userMessage);
            messageRepository.save(assistantMessage);
            return toMessageDTO(assistantMessage);
        });
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId).orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));
        conversationRepository.delete(conversation);
    }
}
