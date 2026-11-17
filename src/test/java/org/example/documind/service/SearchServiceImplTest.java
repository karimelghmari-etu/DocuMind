package org.example.documind.service;

import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.service.interfaces.EmbeddingService;
import org.example.documind.service.interfaces.SearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: real Spring context, real H2 database, real embedding model.
 *
 * Documents and chunks are saved directly (no file upload, no scheduler) so each test
 * controls exactly what is searchable. Not @Transactional, to match how search runs in the app;
 * everything is cleaned up in @AfterEach.
 */
@SpringBootTest
class SearchServiceImplTest {

    @Autowired private SearchService searchService;
    @Autowired private EmbeddingService embeddingService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private UserRepository userRepository;

    private User alice;
    private User bob;

    private static final String COOKING =
            "Knead the dough for ten minutes, let it rise for an hour, then bake the bread in a hot oven until golden.";
    private static final String DATABASES =
            "An index on a database column makes SQL queries faster because the engine does not have to scan every row.";
    private static final String SPACE =
            "The rocket reached orbit and the astronauts docked with the international space station.";

    @BeforeEach
    void createUsers() {
        alice = createUser("alice");
        bob = createUser("bob");
    }

    @AfterEach
    void cleanUp() {
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll(List.of(alice, bob));
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

    /** Saves a document with the given status and one chunk per text, each with its real embedding. */
    private Document createDocument(User owner, String title, DocumentStatus status, String... texts) {
        Document document = documentRepository.save(Document.builder()
                .title(title)
                .filename(title + ".txt")
                .filePath("/not/used/" + title + ".txt")
                .contentType("text/plain")
                .fileSize(1L)
                .status(status)
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

    /** Alice's usual library: three documents on three unrelated topics. */
    private void createAliceLibrary() {
        createDocument(alice, "Baking", DocumentStatus.INDEXED, COOKING);
        createDocument(alice, "Databases", DocumentStatus.INDEXED, DATABASES);
        createDocument(alice, "Space", DocumentStatus.INDEXED, SPACE);
    }

    private static List<String> titles(List<SearchResultDTO> results) {
        return results.stream().map(SearchResultDTO::documentTitle).toList();
    }

    // ---------- ranking ----------

    @Test
    void mostRelevantDocumentComesFirst() {
        createAliceLibrary();

        assertEquals("Baking", searchService.search("how do I make homemade bread?", 5, alice.getId()).get(0).documentTitle());
        assertEquals("Databases", searchService.search("why are my SQL queries slow?", 5, alice.getId()).get(0).documentTitle());
        assertEquals("Space", searchService.search("astronaut missions to orbit", 5, alice.getId()).get(0).documentTitle());
    }

    @Test
    void findsTheRightChunkInsideADocument() {
        createDocument(alice, "Mixed notes", DocumentStatus.INDEXED, COOKING, DATABASES, SPACE);

        SearchResultDTO best = searchService.search("speed up database queries with an index", 5, alice.getId()).get(0);

        assertEquals(1, best.chunkIndex(), "the databases text is chunk 1");
        assertEquals(DATABASES, best.content());
    }

    @Test
    void resultsAreSortedHighestScoreFirst() {
        createAliceLibrary();

        List<SearchResultDTO> results = searchService.search("bread", 5, alice.getId());

        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).score() >= results.get(i).score(),
                    "result " + (i - 1) + " should score at least as high as result " + i);
        }
    }

    @Test
    void resultCarriesTheDocumentDetailsAndAScore() {
        Document baking = createDocument(alice, "Baking", DocumentStatus.INDEXED, COOKING);

        SearchResultDTO result = searchService.search("bake bread", 5, alice.getId()).get(0);

        assertEquals(baking.getId(), result.documentId());
        assertEquals("Baking", result.documentTitle());
        assertEquals(0, result.chunkIndex());
        assertEquals(COOKING, result.content());
        assertTrue(result.score() > 0 && result.score() <= 1.0001, "cosine score should be in (0, 1], got " + result.score());
    }

    // ---------- limit ----------

    @Test
    void limitIsRespected() {
        createAliceLibrary();

        assertEquals(2, searchService.search("anything", 2, alice.getId()).size());
        assertEquals(1, searchService.search("anything", 1, alice.getId()).size());
    }

    @Test
    void limitLargerThanChunkCountReturnsEverything() {
        createAliceLibrary();

        assertEquals(3, searchService.search("anything", 20, alice.getId()).size());
    }

    // ---------- what may be searched ----------

    @Test
    void otherUsersDocumentsNeverAppear() {
        createAliceLibrary();
        // Bob owns a document that matches the question perfectly
        createDocument(bob, "Bob's secret recipe", DocumentStatus.INDEXED, "How do I make homemade bread? Bob's secret recipe.");

        List<SearchResultDTO> results = searchService.search("how do I make homemade bread?", 20, alice.getId());

        assertFalse(titles(results).contains("Bob's secret recipe"), "Alice must never see Bob's chunks");
        assertEquals(3, results.size());
    }

    @Test
    void onlyIndexedDocumentsAreSearched() {
        createDocument(alice, "Done", DocumentStatus.INDEXED, COOKING);
        createDocument(alice, "Waiting", DocumentStatus.PENDING, COOKING);
        createDocument(alice, "Running", DocumentStatus.PROCESSING, COOKING);
        createDocument(alice, "Broken", DocumentStatus.FAILED, COOKING);

        List<SearchResultDTO> results = searchService.search("bake bread", 20, alice.getId());

        assertEquals(List.of("Done"), titles(results));
    }

    @Test
    void userWithNoDocumentsGetsAnEmptyList() {
        assertTrue(searchService.search("anything", 5, alice.getId()).isEmpty());
    }
}
