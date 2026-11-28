package org.example.documind.web;

import org.example.documind.dao.CollectionRepository;
import org.example.documind.dao.ConversationRepository;
import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.ProcessingJobRepository;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.EmbeddingService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Shared setup for the browser-page tests: real Spring context, real security, real H2.
 * Each test creates its own users; everything is deleted afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class WebTestSupport {

    static final String PASSWORD = "correct-horse-1";

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected DocumentRepository documentRepository;
    @Autowired protected DocumentChunkRepository chunkRepository;
    @Autowired protected ProcessingJobRepository jobRepository;
    @Autowired protected CollectionRepository collectionRepository;
    @Autowired protected ConversationRepository conversationRepository;
    @Autowired protected EmbeddingService embeddingService;
    @Autowired protected PasswordEncoder passwordEncoder;

    private final List<User> createdUsers = new ArrayList<>();

    @AfterEach
    void deleteEverything() {
        conversationRepository.deleteAll();
        collectionRepository.deleteAll();
        jobRepository.deleteAll();
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll(createdUsers);
        createdUsers.clear();
    }

    protected User createUser(String prefix) {
        String name = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .username(name)
                .email(name + "@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());
        createdUsers.add(user);
        return user;
    }

    protected Document createDocument(User owner, String title, String... chunkTexts) {
        Document document = documentRepository.save(Document.builder()
                .title(title)
                .filename(title + ".txt")
                .filePath("/not/used/" + title + ".txt")
                .contentType("text/plain")
                .fileSize(1L)
                .status(DocumentStatus.INDEXED)
                .owner(owner)
                .build());

        if (chunkTexts.length > 0) {
            List<float[]> vectors = embeddingService.embedAll(List.of(chunkTexts));
            List<DocumentChunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunkTexts.length; i++) {
                chunks.add(DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(i)
                        .content(chunkTexts[i])
                        .embedding(vectors.get(i))
                        .build());
            }
            chunkRepository.saveAll(chunks);
        }
        return document;
    }

    protected static RequestPostProcessor as(User user) {
        return user(new CustomUserDetails(user));
    }
}
