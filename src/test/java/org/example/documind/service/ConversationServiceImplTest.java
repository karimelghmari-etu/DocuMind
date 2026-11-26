package org.example.documind.service;

import org.example.documind.dao.*;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.entity.Message;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.domain.enums.MessageRole;
import org.example.documind.dto.chat.*;
import org.example.documind.llm.FakeLLMService;
import org.example.documind.service.interfaces.ConversationService;
import org.example.documind.service.interfaces.EmbeddingService;
import org.example.documind.service.llm.ChatTurn;
import org.example.documind.web.exception.ConversationNotFoundException;
import org.example.documind.web.exception.LLMException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: real Spring context, real H2 database, real embeddings and real search.
 * Only the LLM is fake (app.llm.provider=fake), so no test ever calls Groq.
 *
 * Not @Transactional on purpose: sendMessage manages its own short transactions,
 * and the tests must see exactly what was committed. Everything is cleaned up in @AfterEach.
 */
@SpringBootTest
class ConversationServiceImplTest {

    private static final String NO_SOURCES = "Sources:\nnone";
    private static final String VACATION =
            "Every full-time employee receives 25 days of paid vacation each calendar year.";
    private static final String CARRY_OVER =
            "You may carry over a maximum of five vacation days to the next year.";

    @Autowired private ConversationService conversationService;
    @Autowired private FakeLLMService llm;
    @Autowired private EmbeddingService embeddingService;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private UserRepository userRepository;

    @Value("${app.chat.history-size}")
    private int historySize;

    private User alice;   // has the handbook
    private User bob;     // has no documents
    private Document handbook;

    @BeforeEach
    void setUp() {
        llm.reset();
        alice = createUser("alice");
        bob = createUser("bob");
        handbook = createDocument(alice, "Employee Handbook", VACATION, CARRY_OVER);
    }

    @AfterEach
    void cleanUp() {
        conversationRepository.deleteAll();   // cascades to messages and source references
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll(List.of(alice, bob));
        llm.reset();
    }

    // ---------- helpers ----------

    private User createUser(String prefix) {
        String name = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .username(name)
                .email(name + "@example.com")
                .password("irrelevant")
                .build());
    }

    private Document createDocument(User owner, String title, String... texts) {
        Document document = documentRepository.save(Document.builder()
                .title(title)
                .filename(title + ".txt")
                .filePath("/not/used/" + title + ".txt")
                .contentType("text/plain")
                .fileSize(1L)
                .status(DocumentStatus.INDEXED)
                .owner(owner)
                .build());

        List<float[]> vectors = embeddingService.embedAll(List.of(texts));
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            chunks.add(DocumentChunk.builder()
                    .document(document)
                    .chunkIndex(i)
                    .content(texts[i])
                    .embedding(vectors.get(i))
                    .build());
        }
        chunkRepository.saveAll(chunks);
        return document;
    }

    private Long newConversation(User user) {
        return conversationService.createConversation(null, user.getId()).id();
    }

    private List<Message> savedMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByIdAsc(conversationId);
    }

    // ---------------------------------------------------------------- create / list

    @Test
    void createWithoutTitleUsesTheDefaultTitle() {
        ConversationDTO dto = conversationService.createConversation(null, alice.getId());

        assertNotNull(dto.id());
        assertEquals("New conversation", dto.title());
        assertNotNull(dto.createdAt());
    }

    @Test
    void createWithBlankTitleUsesTheDefaultTitle() {
        ConversationDTO dto = conversationService.createConversation(new ConversationRequestDTO("   "), alice.getId());

        assertEquals("New conversation", dto.title());
    }

    @Test
    void createTrimsTheGivenTitle() {
        ConversationDTO dto = conversationService.createConversation(
                new ConversationRequestDTO("  Vacation questions  "), alice.getId());

        assertEquals("Vacation questions", dto.title());
    }

    @Test
    void listShowsOnlyMyConversationsNewestFirst() throws InterruptedException {
        Long older = newConversation(alice);
        Thread.sleep(10);   // make sure the two createdAt values differ
        Long newer = newConversation(alice);
        newConversation(bob);

        List<Long> ids = conversationService.getConversations(alice.getId()).stream()
                .map(ConversationDTO::id).toList();

        assertEquals(List.of(newer, older), ids);
    }

    // ---------------------------------------------------------------- ownership

    @Test
    void anotherUserCannotOpenMyConversation() {
        Long id = newConversation(alice);

        assertThrows(ConversationNotFoundException.class,
                () -> conversationService.getConversation(id, bob.getId()));
    }

    @Test
    void anotherUserCannotPostInMyConversationAndTheLlmIsNotCalled() {
        Long id = newConversation(alice);

        assertThrows(ConversationNotFoundException.class,
                () -> conversationService.sendMessage(id, "How many vacation days do I get?", bob.getId()));

        assertTrue(llm.calls().isEmpty(), "ownership is checked before spending an LLM call");
        assertTrue(savedMessages(id).isEmpty());
    }

    @Test
    void anotherUserCannotDeleteMyConversation() {
        Long id = newConversation(alice);

        assertThrows(ConversationNotFoundException.class,
                () -> conversationService.deleteConversation(id, bob.getId()));

        assertTrue(conversationRepository.findById(id).isPresent());
    }

    @Test
    void unknownConversationIsNotFound() {
        assertThrows(ConversationNotFoundException.class,
                () -> conversationService.getConversation(999_999L, alice.getId()));
        assertThrows(ConversationNotFoundException.class,
                () -> conversationService.sendMessage(999_999L, "Question?", alice.getId()));
    }

    // ---------------------------------------------------------------- sendMessage

    @Test
    void sendMessageReturnsTheAnswerWithItsCitedSource() {
        Long id = newConversation(alice);
        llm.willReply("You get 25 days of paid vacation [1].");

        MessageDTO answer = conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        assertNotNull(answer.id());
        assertEquals(MessageRole.ASSISTANT, answer.role());
        assertEquals("You get 25 days of paid vacation [1].", answer.content());
        assertEquals(1, answer.sources().size());

        SourceDTO source = answer.sources().get(0);
        assertEquals(1, source.number());
        assertEquals(handbook.getId(), source.documentId());
        assertEquals("Employee Handbook", source.documentTitle());
        assertEquals(VACATION, source.content(), "the vacation chunk is the best match, so it is source [1]");
        assertTrue(source.score() >= 0.3);
    }

    @Test
    void sendMessageSavesTheQuestionThenTheAnswer() {
        Long id = newConversation(alice);
        llm.willReply("You get 25 days [1].");

        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        ConversationDetailDTO conversation = conversationService.getConversation(id, alice.getId());
        assertEquals(2, conversation.messages().size());

        MessageDTO question = conversation.messages().get(0);
        MessageDTO answer = conversation.messages().get(1);
        assertEquals(MessageRole.USER, question.role());
        assertEquals("How many vacation days do I get?", question.content());
        assertTrue(question.sources().isEmpty());
        assertEquals(MessageRole.ASSISTANT, answer.role());
        assertEquals(1, answer.sources().size(), "citations are saved and loaded back");
        assertTrue(answer.id() > question.id());
    }

    @Test
    void savedQuestionIsThePlainQuestionNotThePromptWithSources() {
        Long id = newConversation(alice);

        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        assertEquals("How many vacation days do I get?", savedMessages(id).get(0).getContent());
    }

    @Test
    void sourcesAreSortedByCitationNumber() {
        Long id = newConversation(alice);
        llm.willReply("Carry over five days [2], and you get 25 days [1].");

        MessageDTO answer = conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        assertEquals(List.of(1, 2), answer.sources().stream().map(SourceDTO::number).toList());
    }

    @Test
    void searchOnlyUsesMyOwnDocuments() {
        Long id = newConversation(bob);   // bob has no documents, alice has the handbook

        MessageDTO answer = conversationService.sendMessage(id, "How many vacation days do I get?", bob.getId());

        String prompt = lastUserTurn();
        assertTrue(prompt.startsWith(NO_SOURCES), "bob has no documents, so no sources are sent");
        assertFalse(prompt.contains(VACATION), "alice's handbook must never reach bob's prompt");
        assertTrue(answer.sources().isEmpty(), "the fake reply cites [1], but there is no source 1");
    }

    @Test
    void answersWithoutSourcesAreSavedToo() {
        Long id = newConversation(bob);
        llm.willReply("I couldn't find that in your documents.");

        MessageDTO answer = conversationService.sendMessage(id, "How many vacation days do I get?", bob.getId());

        List<Message> saved = savedMessages(id);
        assertEquals(2, saved.size());
        assertEquals("I couldn't find that in your documents.", saved.get(1).getContent());
        assertTrue(answer.sources().isEmpty());
    }

    @Test
    void greetingGetsAnAnswerWithoutSources() {
        Long id = newConversation(alice);
        llm.willReply("Hello! Ask me anything about your documents.");

        MessageDTO answer = conversationService.sendMessage(id, "hello", alice.getId());

        assertEquals(1, llm.calls().size(), "the LLM answers greetings too");
        assertEquals("Hello! Ask me anything about your documents.", answer.content());
        assertTrue(answer.sources().isEmpty());
    }

    private String lastUserTurn() {
        List<ChatTurn> conversation = llm.lastCall().conversation();
        return conversation.get(conversation.size() - 1).content();
    }

    @Test
    void whenTheLlmFailsNothingIsSaved() {
        Long id = newConversation(alice);
        llm.willFail(new LLMException("Groq rate limit reached"));

        assertThrows(LLMException.class,
                () -> conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId()));

        assertTrue(savedMessages(id).isEmpty(), "no question without an answer");
        assertEquals("New conversation", conversationRepository.findById(id).orElseThrow().getTitle(),
                "the title is not changed either");
    }

    // ---------------------------------------------------------------- history

    @Test
    void firstMessageIsSentWithoutHistory() {
        Long id = newConversation(alice);

        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        assertEquals(1, llm.lastCall().conversation().size(), "only the new question");
    }

    @Test
    void followUpQuestionsReceiveTheEarlierTurns() {
        Long id = newConversation(alice);
        llm.willReply("You get 25 days [1].");
        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        llm.willReply("Five days [1].");
        conversationService.sendMessage(id, "How many vacation days can I carry over?", alice.getId());

        List<ChatTurn> sent = llm.lastCall().conversation();
        assertEquals(3, sent.size());
        assertEquals(new ChatTurn(MessageRole.USER, "How many vacation days do I get?"), sent.get(0));
        assertEquals(new ChatTurn(MessageRole.ASSISTANT, "You get 25 days [1]."), sent.get(1));
        assertEquals(MessageRole.USER, sent.get(2).role());
        assertTrue(sent.get(2).content().endsWith("Question: How many vacation days can I carry over?"));
    }

    @Test
    void historyIsLimitedToTheLastMessages() {
        Long id = newConversation(alice);
        for (int i = 1; i <= 4; i++) {
            llm.willReply("Answer " + i + " [1].");
            conversationService.sendMessage(id, "Vacation question " + i + ": how many paid vacation days?", alice.getId());
        }

        conversationService.sendMessage(id, "Vacation question 5: how many paid vacation days?", alice.getId());

        // 8 messages exist before the 5th question; only the last historySize (6) are sent, plus the new question.
        List<ChatTurn> sent = llm.lastCall().conversation();
        assertEquals(historySize + 1, sent.size());
        assertEquals("Vacation question 2: how many paid vacation days?", sent.get(0).content(),
                "question 1 and answer 1 were dropped");
        assertEquals("Answer 4 [1].", sent.get(sent.size() - 2).content());
    }

    // ---------------------------------------------------------------- title

    @Test
    void firstQuestionBecomesTheTitle() {
        Long id = newConversation(alice);

        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        assertEquals("How many vacation days do I get?", conversationService.getConversation(id, alice.getId()).title());
    }

    @Test
    void laterQuestionsDoNotChangeTheTitle() {
        Long id = newConversation(alice);
        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        conversationService.sendMessage(id, "Can I carry vacation days over?", alice.getId());

        assertEquals("How many vacation days do I get?", conversationService.getConversation(id, alice.getId()).title());
    }

    @Test
    void aTitleChosenByTheUserIsKept() {
        Long id = conversationService.createConversation(new ConversationRequestDTO("My HR questions"), alice.getId()).id();

        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        assertEquals("My HR questions", conversationService.getConversation(id, alice.getId()).title());
    }

    @Test
    void longFirstQuestionIsShortenedForTheTitle() {
        Long id = newConversation(bob);
        String question = "a".repeat(100);

        conversationService.sendMessage(id, question, bob.getId());

        assertEquals("a".repeat(60) + "...", conversationService.getConversation(id, bob.getId()).title());
    }

    // ---------------------------------------------------------------- delete

    @Test
    void deleteRemovesTheConversationAndItsMessages() {
        Long id = newConversation(alice);
        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        conversationService.deleteConversation(id, alice.getId());

        assertTrue(conversationRepository.findById(id).isEmpty());
        assertTrue(savedMessages(id).isEmpty());
    }

    @Test
    void deletingTheCitedChunksKeepsTheConversationButRemovesTheCitations() {
        // Documents the current behaviour: SourceReference.chunk uses @OnDelete(CASCADE),
        // so reprocessing or deleting a document silently removes the citations that pointed to it.
        Long id = newConversation(alice);
        llm.willReply("You get 25 days [1].");
        conversationService.sendMessage(id, "How many vacation days do I get?", alice.getId());

        chunkRepository.deleteAll();

        ConversationDetailDTO conversation = conversationService.getConversation(id, alice.getId());
        assertEquals(2, conversation.messages().size());
        assertEquals("You get 25 days [1].", conversation.messages().get(1).content(), "the answer text is kept");
        assertTrue(conversation.messages().get(1).sources().isEmpty(), "but its citations are gone");
    }
}
