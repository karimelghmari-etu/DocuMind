package org.example.documind.service.impl;

import org.example.documind.domain.enums.MessageRole;
import org.example.documind.dto.chat.CitedSource;
import org.example.documind.dto.chat.RagAnswer;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.llm.FakeLLMService;
import org.example.documind.service.interfaces.SearchService;
import org.example.documind.service.llm.ChatTurn;
import org.example.documind.web.exception.LLMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RAGServiceImpl: no Spring, no database.
 * Search is a lambda returning fixed results, and the LLM is the FakeLLMService.
 * Lives in the same package as RAGServiceImpl so it can call the package-private extractCitations.
 */
class RAGServiceImplTest {

    private static final String NO_SOURCES = "Sources:\nnone";
    private static final int TOP_K = 5;
    private static final double MIN_SCORE = 0.3;
    private static final Long OWNER_ID = 42L;

    private FakeLLMService llm;
    private List<SearchResultDTO> searchResults;
    private final List<String> searchedQueries = new ArrayList<>();
    private final List<Integer> searchedLimits = new ArrayList<>();
    private final List<Long> searchedOwners = new ArrayList<>();
    private RAGServiceImpl rag;

    @BeforeEach
    void setUp() {
        llm = new FakeLLMService();
        searchResults = List.of();
        SearchService fakeSearch = (query, limit, ownerId) -> {
            searchedQueries.add(query);
            searchedLimits.add(limit);
            searchedOwners.add(ownerId);
            return searchResults;
        };
        rag = new RAGServiceImpl(fakeSearch, llm, TOP_K, MIN_SCORE);
    }

    private static SearchResultDTO result(long chunkId, String title, String content, double score) {
        return new SearchResultDTO(chunkId, chunkId * 10, title, 0, content, score);
    }

    /** Three results, sorted by score like the real search. */
    private void givenThreeResults() {
        searchResults = List.of(
                result(1, "Handbook", "Employees receive 25 days of paid vacation.", 0.80),
                result(2, "Security", "Passwords must be at least 14 characters.", 0.50),
                result(3, "Travel", "The hotel limit is 180 euros per night.", 0.40));
    }

    private String lastPrompt() {
        List<ChatTurn> conversation = llm.lastCall().conversation();
        return conversation.get(conversation.size() - 1).content();
    }

    // ---------------------------------------------------------------- retrieval

    @Nested
    class Retrieval {

        @Test
        void searchesWithTheQuestionTopKAndOwner() {
            givenThreeResults();

            rag.answer("How many vacation days?", List.of(), OWNER_ID);

            assertEquals(List.of("How many vacation days?"), searchedQueries);
            assertEquals(List.of(TOP_K), searchedLimits);
            assertEquals(List.of(OWNER_ID), searchedOwners);
        }

        @Test
        void noResultsStillCallsTheLlmWithNoSources() {
            searchResults = List.of();
            llm.willReply("Hello! How can I help with your documents?");

            RagAnswer answer = rag.answer("hello", List.of(), OWNER_ID);

            assertEquals(1, llm.calls().size(), "greetings and small talk are answered by the LLM");
            assertTrue(lastPrompt().startsWith(NO_SOURCES));
            assertTrue(lastPrompt().endsWith("Question: hello"));
            assertEquals("Hello! How can I help with your documents?", answer.answer());
            assertTrue(answer.sources().isEmpty());
        }

        @Test
        void bestScoreBelowMinScoreSendsNoSources() {
            searchResults = List.of(result(1, "Handbook", "Unrelated text.", 0.29));

            rag.answer("What is the CEO's favourite colour?", List.of(), OWNER_ID);

            assertTrue(lastPrompt().startsWith(NO_SOURCES));
            assertFalse(lastPrompt().contains("Unrelated text."), "weak chunks must not reach the LLM");
            assertFalse(lastPrompt().contains("[1]"));
        }

        @Test
        void withoutSourcesCitationsInTheReplyAreIgnored() {
            searchResults = List.of(result(1, "Handbook", "Unrelated text.", 0.29));
            llm.willReply("Something [1].");

            RagAnswer answer = rag.answer("Question?", List.of(), OWNER_ID);

            assertTrue(answer.sources().isEmpty(), "there is no source 1 to point to");
        }

        @Test
        void bestScoreExactlyAtMinScoreSendsTheSources() {
            searchResults = List.of(result(1, "Handbook", "Borderline text.", MIN_SCORE));

            rag.answer("Borderline question?", List.of(), OWNER_ID);

            assertTrue(lastPrompt().contains("[1] (Handbook)\nBorderline text."));
        }
    }

    // ---------------------------------------------------------------- prompt

    @Nested
    class Prompt {

        @Test
        void sendsTheSystemPromptWithTheRules() {
            givenThreeResults();

            rag.answer("Question?", List.of(), OWNER_ID);

            String systemPrompt = llm.lastCall().systemPrompt();
            assertTrue(systemPrompt.contains("[1]"), "the prompt explains the citation format");
            assertTrue(systemPrompt.contains("greeting"), "the prompt allows natural replies to small talk");
            assertTrue(systemPrompt.contains("could not find it"), "the prompt says what to do when the answer is missing");
            assertTrue(systemPrompt.contains("Never use outside knowledge"), "the prompt forbids outside knowledge");
        }

        @Test
        void numbersEverySourceWithItsTitleAndContent() {
            givenThreeResults();

            rag.answer("Question?", List.of(), OWNER_ID);

            String prompt = lastPrompt();
            assertTrue(prompt.contains("[1] (Handbook)\nEmployees receive 25 days of paid vacation."));
            assertTrue(prompt.contains("[2] (Security)\nPasswords must be at least 14 characters."));
            assertTrue(prompt.contains("[3] (Travel)\nThe hotel limit is 180 euros per night."));
            assertFalse(prompt.contains("[4]"));
        }

        @Test
        void questionComesAfterTheSources() {
            givenThreeResults();

            rag.answer("How many vacation days?", List.of(), OWNER_ID);

            String prompt = lastPrompt();
            assertTrue(prompt.startsWith("Sources:"));
            assertTrue(prompt.endsWith("Question: How many vacation days?"));
        }

        @Test
        void historyComesFirstAndTheNewQuestionIsTheLastUserTurn() {
            givenThreeResults();
            List<ChatTurn> history = List.of(
                    new ChatTurn(MessageRole.USER, "First question"),
                    new ChatTurn(MessageRole.ASSISTANT, "First answer [1]."));

            rag.answer("Follow-up?", history, OWNER_ID);

            List<ChatTurn> sent = llm.lastCall().conversation();
            assertEquals(3, sent.size());
            assertEquals(history.get(0), sent.get(0));
            assertEquals(history.get(1), sent.get(1));
            assertEquals(MessageRole.USER, sent.get(2).role());
            assertTrue(sent.get(2).content().endsWith("Question: Follow-up?"));
        }

        @Test
        void sourcesAreOnlyInTheLastTurnNotInTheHistory() {
            givenThreeResults();
            List<ChatTurn> history = List.of(
                    new ChatTurn(MessageRole.USER, "First question"),
                    new ChatTurn(MessageRole.ASSISTANT, "First answer."));

            rag.answer("Follow-up?", history, OWNER_ID);

            List<ChatTurn> sent = llm.lastCall().conversation();
            assertFalse(sent.get(0).content().contains("Sources:"));
            assertFalse(sent.get(1).content().contains("Sources:"));
        }

        @Test
        void doesNotModifyTheCallersHistory() {
            givenThreeResults();
            List<ChatTurn> history = new ArrayList<>(List.of(new ChatTurn(MessageRole.USER, "First question")));

            rag.answer("Follow-up?", history, OWNER_ID);

            assertEquals(1, history.size());
        }

        @Test
        void worksWithAnImmutableEmptyHistory() {
            givenThreeResults();

            assertDoesNotThrow(() -> rag.answer("Question?", List.of(), OWNER_ID));
        }
    }

    // ---------------------------------------------------------------- answer and sources

    @Nested
    class Answer {

        @Test
        void returnsTheLlmReplyUnchanged() {
            givenThreeResults();
            llm.willReply("You get 25 days of paid vacation [1].");

            RagAnswer answer = rag.answer("Question?", List.of(), OWNER_ID);

            assertEquals("You get 25 days of paid vacation [1].", answer.answer());
        }

        @Test
        void returnsOnlyTheCitedSources() {
            givenThreeResults();
            llm.willReply("Vacation is 25 days [1] and the hotel limit is 180 euros [3].");

            RagAnswer answer = rag.answer("Question?", List.of(), OWNER_ID);

            assertEquals(List.of(1, 3), answer.sources().stream().map(CitedSource::number).toList());
        }

        @Test
        void eachCitedNumberPointsToTheRightSearchResult() {
            givenThreeResults();
            llm.willReply("See [2].");

            CitedSource cited = rag.answer("Question?", List.of(), OWNER_ID).sources().get(0);

            assertEquals(2, cited.number());
            assertEquals(2L, cited.source().chunkId());
            assertEquals("Security", cited.source().documentTitle());
        }

        @Test
        void answerWithoutCitationsHasNoSources() {
            givenThreeResults();
            llm.willReply("I could not find this in your documents.");

            RagAnswer answer = rag.answer("Question?", List.of(), OWNER_ID);

            assertEquals("I could not find this in your documents.", answer.answer());
            assertTrue(answer.sources().isEmpty());
        }

        @Test
        void inventedCitationNumbersAreIgnored() {
            givenThreeResults();
            llm.willReply("True [1], made up [7].");

            RagAnswer answer = rag.answer("Question?", List.of(), OWNER_ID);

            assertEquals(List.of(1), answer.sources().stream().map(CitedSource::number).toList());
        }

        @Test
        void llmErrorsAreNotSwallowed() {
            givenThreeResults();
            llm.willFail(new LLMException("Groq rate limit reached"));

            assertThrows(LLMException.class, () -> rag.answer("Question?", List.of(), OWNER_ID));
        }
    }

    // ---------------------------------------------------------------- extractCitations

    @Nested
    class ExtractCitations {

        @Test
        void singleCitation() {
            assertEquals(Set.of(1), RAGServiceImpl.extractCitations("Answer [1].", 5));
        }

        @Test
        void adjacentCitations() {
            assertEquals(Set.of(2, 3), RAGServiceImpl.extractCitations("Answer [2][3].", 5));
        }

        @Test
        void commaSeparatedCitations() {
            assertEquals(Set.of(1, 2, 4), RAGServiceImpl.extractCitations("Answer [1, 2,4].", 5));
        }

        @Test
        void duplicatesAreKeptOnce() {
            assertEquals(Set.of(1), RAGServiceImpl.extractCitations("A [1]. B [1]. C [1].", 5));
        }

        @Test
        void resultIsSorted() {
            assertEquals(List.of(1, 3, 5),
                    List.copyOf(RAGServiceImpl.extractCitations("[5] then [1] then [3]", 5)));
        }

        @Test
        void numbersAboveTheSourceCountAreIgnored() {
            assertEquals(Set.of(2), RAGServiceImpl.extractCitations("[2] [6] [99]", 5));
        }

        @Test
        void zeroIsIgnored() {
            assertEquals(Set.of(), RAGServiceImpl.extractCitations("[0]", 5));
        }

        @Test
        void textWithoutCitations() {
            assertEquals(Set.of(), RAGServiceImpl.extractCitations("No citations here.", 5));
        }

        @Test
        void otherBracketsAreNotCitations() {
            assertEquals(Set.of(), RAGServiceImpl.extractCitations("See [note] and [a, b] and [] and [-1].", 5));
        }

        @Test
        void citationsInsideAListMixedWithText() {
            assertEquals(Set.of(1, 3),
                    RAGServiceImpl.extractCitations("- 25 days [1]\n- carry over five days [3]", 5));
        }
    }
}
