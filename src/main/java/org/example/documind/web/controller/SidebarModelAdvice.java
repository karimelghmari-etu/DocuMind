package org.example.documind.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.documind.dto.collection.CollectionDTO;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.CollectionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Comparator;
import java.util.List;

/**
 * The sidebar lists the user's collections on every page, so every page controller
 * would need the same lines. This adds them once, only for the browser-page controllers
 * (not the REST API), and only when someone is logged in.
 */
@ControllerAdvice(assignableTypes = {
        PageController.class,
        DocumentsWebController.class,
        CollectionsWebController.class,
        WorkspaceWebController.class,
        SettingsWebController.class,
        AssistantWebController.class
})
@RequiredArgsConstructor
public class SidebarModelAdvice {

    static final int MAX_IN_SIDEBAR = 6;

    private final CollectionService collectionService;

    @ModelAttribute("sidebarCollections")
    public List<CollectionDTO> sidebarCollections(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return List.of();   // login and register pages
        }
        return collectionService.getCollections(userDetails.getId()).stream()
                .sorted(Comparator.comparing(CollectionDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_IN_SIDEBAR)
                .toList();
    }
}
