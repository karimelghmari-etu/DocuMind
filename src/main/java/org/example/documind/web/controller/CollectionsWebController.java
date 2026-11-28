package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.documind.dto.collection.CollectionDTO;
import org.example.documind.dto.collection.CollectionRequestDTO;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.CollectionService;
import org.example.documind.service.interfaces.DocumentService;
import org.example.documind.web.exception.CollectionNotFoundException;
import org.example.documind.web.exception.DocumentNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Browser pages for collections. Like the documents page, every error comes back
 * as a red flash message, never as a JSON page.
 */
@Controller
@RequiredArgsConstructor
public class CollectionsWebController {

    static final int MAX_NAME = 100;
    static final int MAX_DESCRIPTION = 1000;

    private final CollectionService collectionService;
    private final DocumentService documentService;

    private static void addUser(Model model, CustomUserDetails userDetails) {
        String username = userDetails.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("avatarInitial", username.substring(0, 1).toUpperCase());
        model.addAttribute("activePage", "collections");
    }

    /** Collection names and descriptions come from a form: trim them and check their length. */
    private static String validate(CollectionRequestDTO request, BindingResult bindingResult) {
        String name = request.getName() == null ? "" : request.getName().strip();
        String description = request.getDescription() == null ? "" : request.getDescription().strip();

        if (bindingResult.hasErrors() || name.isEmpty()) {
            return "Please give the collection a name.";
        }
        if (name.length() > MAX_NAME) {
            return "The name must be at most " + MAX_NAME + " characters.";
        }
        if (description.length() > MAX_DESCRIPTION) {
            return "The description must be at most " + MAX_DESCRIPTION + " characters.";
        }
        request.setName(name);
        request.setDescription(description.isEmpty() ? null : description);
        return null;
    }

    // ---------------------------------------------------------------- list

    @GetMapping("/collections")
    public String collectionsPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        addUser(model, userDetails);
        model.addAttribute("currentPage", "Collections");

        List<CollectionDTO> collections = collectionService.getCollections(userDetails.getId()).stream()
                .sorted(Comparator.comparing(CollectionDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("collections", collections);
        model.addAttribute("maxName", MAX_NAME);
        model.addAttribute("maxDescription", MAX_DESCRIPTION);

        return "collections";
    }

    @PostMapping("/collections")
    public String createCollection(
            @Valid @ModelAttribute CollectionRequestDTO request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String error = validate(request, bindingResult);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/collections";
        }

        CollectionDTO created = collectionService.createCollection(request, userDetails.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Collection created. Now add some documents to it.");
        return "redirect:/collections/" + created.getId();
    }

    @PostMapping("/collections/{collectionId}/delete")
    public String deleteCollection(
            @PathVariable Long collectionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            collectionService.deleteCollection(collectionId, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Collection deleted. Its documents were kept.");
        } catch (CollectionNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "This collection no longer exists.");
        }
        return "redirect:/collections";
    }

    // ---------------------------------------------------------------- detail

    @GetMapping("/collections/{collectionId}")
    public String collectionPage(
            @PathVariable Long collectionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = userDetails.getId();
        CollectionDTO collection;
        Set<Long> inCollection;
        try {
            collection = collectionService.getCollectionById(collectionId, userId);
            inCollection = collectionService.getDocumentIds(collectionId, userId);
        } catch (CollectionNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "This collection no longer exists.");
            return "redirect:/collections";
        }

        List<DocumentDTO> all = documentService.getDocumentsByOwnerId(userId);

        addUser(model, userDetails);
        model.addAttribute("currentPage", collection.getName());
        model.addAttribute("collection", collection);
        model.addAttribute("activeCollectionId", collection.getId());   // highlights it in the sidebar
        model.addAttribute("documents", all.stream().filter(d -> inCollection.contains(d.getId())).toList());
        model.addAttribute("otherDocuments", all.stream().filter(d -> !inCollection.contains(d.getId())).toList());
        model.addAttribute("maxName", MAX_NAME);
        model.addAttribute("maxDescription", MAX_DESCRIPTION);

        return "collection";
    }

    @PostMapping("/collections/{collectionId}")
    public String updateCollection(
            @PathVariable Long collectionId,
            @Valid @ModelAttribute CollectionRequestDTO request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String error = validate(request, bindingResult);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/collections/" + collectionId;
        }

        try {
            collectionService.updateCollection(collectionId, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Collection updated.");
        } catch (CollectionNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "This collection no longer exists.");
            return "redirect:/collections";
        }
        return "redirect:/collections/" + collectionId;
    }

    @PostMapping("/collections/{collectionId}/documents")
    public String addDocument(
            @PathVariable Long collectionId,
            @RequestParam(required = false) Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (documentId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose a document to add.");
            return "redirect:/collections/" + collectionId;
        }
        return changeMembership(collectionId, documentId, true, userDetails, redirectAttributes);
    }

    @PostMapping("/collections/{collectionId}/documents/{documentId}/remove")
    public String removeDocument(
            @PathVariable Long collectionId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        return changeMembership(collectionId, documentId, false, userDetails, redirectAttributes);
    }

    private String changeMembership(Long collectionId, Long documentId, boolean add,
                                    CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            if (add) {
                collectionService.addDocumentToCollection(collectionId, documentId, userDetails.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Document added to the collection.");
            } else {
                collectionService.removeDocumentFromCollection(collectionId, documentId, userDetails.getId());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Document removed from the collection. It is still in My Documents.");
            }
        } catch (CollectionNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "This collection no longer exists.");
            return "redirect:/collections";
        } catch (DocumentNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "This document no longer exists.");
        }
        return "redirect:/collections/" + collectionId;
    }
}
