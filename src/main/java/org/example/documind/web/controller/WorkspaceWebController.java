package org.example.documind.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.ConversationService;
import org.example.documind.service.interfaces.SearchService;
import org.example.documind.web.exception.ConversationNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** The History page (all conversations) and the Search page (semantic search from the navbar). */
@Controller
@RequiredArgsConstructor
public class WorkspaceWebController {

    static final int SEARCH_LIMIT = 10;
    static final int MAX_QUERY = 500;

    private final ConversationService conversationService;
    private final SearchService searchService;

    /** Same threshold as the chat: below it, a passage is not shown as a match. */
    @Value("${app.rag.min-score}")
    private double minScore;

    private static void addUser(Model model, CustomUserDetails userDetails, String activePage, String currentPage) {
        String username = userDetails.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("avatarInitial", username.substring(0, 1).toUpperCase());
        model.addAttribute("activePage", activePage);
        model.addAttribute("currentPage", currentPage);
    }

    // ---------------------------------------------------------------- history

    @GetMapping("/history")
    public String historyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        addUser(model, userDetails, "history", "History");
        model.addAttribute("conversations", conversationService.getConversations(userDetails.getId()));
        return "history";
    }

    @PostMapping("/history/{conversationId}/delete")
    public String deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            conversationService.deleteConversation(conversationId, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Conversation deleted.");
        } catch (ConversationNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "This conversation no longer exists.");
        }
        return "redirect:/history";
    }

    // ---------------------------------------------------------------- search

    @GetMapping("/search")
    public String searchPage(
            @RequestParam(name = "q", required = false) String query,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        addUser(model, userDetails, "search", "Search");

        String q = query == null ? "" : query.strip();
        model.addAttribute("query", q);

        if (q.isEmpty()) {
            return "search";
        }
        if (q.length() > MAX_QUERY) {
            model.addAttribute("errorMessage", "Your search must be at most " + MAX_QUERY + " characters.");
            return "search";
        }

        List<SearchResultDTO> results = searchService.search(q, SEARCH_LIMIT, userDetails.getId()).stream()
                .filter(r -> r.score() >= minScore)
                .toList();
        model.addAttribute("results", results);
        return "search";
    }
}
