package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.documind.dto.chat.*;
import org.example.documind.service.interfaces.ConversationService;
import org.example.documind.service.interfaces.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@AllArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final UserService userService;

    @GetMapping
    public List<ConversationDTO> getConversations(){
        Long userId = userService.getCurrentUserId();
        return conversationService.getConversations(userId);
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailDTO getConversation(@PathVariable Long conversationId){
        Long userId = userService.getCurrentUserId();
        return conversationService.getConversation(conversationId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDTO createConversation(@Valid @RequestBody(required = false) ConversationRequestDTO conversationRequestDTO){
        Long userId = userService.getCurrentUserId();
        return conversationService.createConversation(conversationRequestDTO, userId);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO sendMessage(@Valid @RequestBody ChatRequest chatRequest, @PathVariable Long conversationId){
        Long userId = userService.getCurrentUserId();
        return conversationService.sendMessage(conversationId, chatRequest.question(), userId);
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable Long conversationId){
        Long userId = userService.getCurrentUserId();
        conversationService.deleteConversation(conversationId, userId);
    }
}
