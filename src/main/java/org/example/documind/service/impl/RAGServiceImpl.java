package org.example.documind.service.impl;

import org.example.documind.domain.enums.MessageRole;
import org.example.documind.dto.chat.CitedSource;
import org.example.documind.dto.chat.RagAnswer;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.service.interfaces.LLMService;
import org.example.documind.service.interfaces.RAGService;
import org.example.documind.service.interfaces.SearchService;
import org.example.documind.service.llm.ChatTurn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RAGServiceImpl implements RAGService {
    private final SearchService searchService;
    private final LLMService llmService;
    private final int topK;
    private final double minScore;

    private static final String SYSTEM_PROMPT = """
        You are DocuMind, an assistant that answers questions about the user's documents.
        Rules:
        - Answer only with information from the numbered sources given with the question.
        - After each fact, cite its source number in square brackets, like [1] or [2][3].
        - If the sources do not contain the answer, reply exactly: "I could not find this in your documents."
        - Never use outside knowledge and never guess.
        - Answer in the same language as the question, and be concise.
        """;
    private static final String NOT_FOUND = "I could not find this in your documents.";

    public RAGServiceImpl(SearchService searchService, LLMService llmService, @Value("${app.rag.top-k}") int topk, @Value("${app.rag.min-score}") double minScore){
        this.searchService = searchService;
        this.llmService = llmService;
        this.topK = topk;
        this.minScore = minScore;
    }
    @Override
    public RagAnswer answer(String question, List<ChatTurn> history, Long ownerId) {
        List<SearchResultDTO> results = searchService.search(question, topK, ownerId);

        if (results.isEmpty() || results.get(0).score() < minScore){
            return new RagAnswer(NOT_FOUND, List.of());
        }

        StringBuilder sources = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResultDTO r = results.get(i);
            sources.append("[").append(i + 1).append("] (").append(r.documentTitle()).append(")\n")
                    .append(r.content()).append("\n\n");
        }

        List<ChatTurn> conversation = new ArrayList<>(history);
        conversation.add(new ChatTurn(MessageRole.USER,
                "Sources:\n" + sources + "Question: " + question));

        String reply = llmService.generate(SYSTEM_PROMPT, conversation);

        List<CitedSource> cited = new ArrayList<>();
        for (int n : extractCitations(reply, results.size())) {
            cited.add(new CitedSource(n, results.get(n - 1)));
        }

        return new RagAnswer(reply, cited);
    }

    private static final Pattern CITATION = Pattern.compile("\\[(\\d+(?:\\s*,\\s*\\d+)*)]");

    static Set<Integer> extractCitations(String text, int sourceCount) {
        Set<Integer> numbers = new TreeSet<>();
        Matcher matcher = CITATION.matcher(text);
        while (matcher.find()) {
            for (String part : matcher.group(1).split(",")) {
                int n = Integer.parseInt(part.strip());
                if (n >= 1 && n <= sourceCount) {
                    numbers.add(n);
                }
            }
        }
        return numbers;
    }
}
