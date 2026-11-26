
package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.documind.dto.chat.*;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantWebController {

    private final ConversationService conversationService;

    // Display the AI Assistant page
    @GetMapping
    public String assistantPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            org.springframework.ui.Model model) {

        String username = userDetails.getUsername();

        model.addAttribute("username", username);
        model.addAttribute(
                "avatarInitial",
                username.substring(0, 1).toUpperCase()
        );
        model.addAttribute("activePage", "assistant");

        return "assistant";
    }

    // Get all conversations of the connected user
    @GetMapping("/conversations")
    @ResponseBody
    public List<ConversationDTO> getConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return conversationService.getConversations(userDetails.getId());
    }

    // Create a conversation
    @PostMapping("/conversations")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDTO createConversation(
            @RequestBody(required = false) ConversationRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return conversationService.createConversation(
                request,
                userDetails.getId()
        );
    }

    // Get a conversation with its messages
    @GetMapping("/conversations/{conversationId}")
    @ResponseBody
    public ConversationDetailDTO getConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return conversationService.getConversation(
                conversationId,
                userDetails.getId()
        );
    }

    // Send a question
    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return conversationService.sendMessage(
                conversationId,
                request.question(),
                userDetails.getId()
        );
    }

    // Delete a conversation
    @DeleteMapping("/conversations/{conversationId}")
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        conversationService.deleteConversation(
                conversationId,
                userDetails.getId()
        );
    }
}
