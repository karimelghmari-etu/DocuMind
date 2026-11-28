package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.documind.dto.document.DocumentStatusDTO;
import org.example.documind.dto.document.DocumentUploadDTO;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.DocumentService;
import org.example.documind.web.exception.DocumentNotFoundException;
import org.example.documind.web.exception.FileStorageException;
import org.example.documind.web.exception.InvalidDocumentStateException;
import org.example.documind.web.exception.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Browser pages for documents. Errors never leave this page as raw JSON:
 * they are caught here and shown as a red flash message on /documents.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DocumentsWebController {

    private final DocumentService documentService;

    /** Shown to the browser so documents.js can reject big files before uploading them. */
    @Value("${spring.servlet.multipart.max-file-size:1MB}")
    private String maxFileSize;

    @GetMapping("/documents")
    public String documentsPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getId();
        String username = userDetails.getUsername();

        model.addAttribute("username", username);
        model.addAttribute(
                "avatarInitial",
                username.substring(0, 1).toUpperCase()
        );
        model.addAttribute("activePage", "documents");

        model.addAttribute(
                "documents",
                documentService.getDocumentsByOwnerId(userId)
        );
        model.addAttribute("currentPage", "My Documents");
        model.addAttribute("maxFileSize", maxFileSize);

        return "documents";
    }

    @PostMapping(
            value = "/documents/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String uploadDocument(
            @Valid @ModelAttribute DocumentUploadDTO documentUploadDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "The title must be at most 255 characters."
            );
            return "redirect:/documents";
        }

        try {
            documentService.uploadDocument(
                    documentUploadDTO,
                    userDetails.getId()
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Document uploaded successfully!"
            );
        } catch (InvalidFileException e) {
            // "File is required", "Unsupported file type", ... : safe to show.
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (FileStorageException e) {
            log.error("Could not store uploaded file", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Could not save the file. Please try again."
            );
        }

        return "redirect:/documents";
    }

    @PostMapping("/documents/{documentId}/delete")
    public String deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            documentService.deleteDocument(
                    documentId,
                    userDetails.getId()
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Document deleted successfully!"
            );
        } catch (DocumentNotFoundException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This document no longer exists."
            );
        }

        return "redirect:/documents";
    }

    @PostMapping("/documents/{documentId}/reprocess")
    public String reprocessDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            documentService.reprocessDocument(
                    documentId,
                    userDetails.getId()
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Document reprocessing requested!"
            );
        } catch (DocumentNotFoundException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This document no longer exists."
            );
        } catch (InvalidDocumentStateException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only failed documents can be reprocessed."
            );
        }

        return "redirect:/documents";
    }

    /**
     * Polled by documents.js while a document is PENDING or PROCESSING,
     * so its badge updates without reloading the page.
     * Another user's document gives 404 (DocumentNotFoundException → GlobalExceptionHandler).
     */
    @GetMapping("/documents/{documentId}/status")
    @ResponseBody
    public DocumentStatusDTO documentStatus(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return documentService.getDocumentStatus(
                documentId,
                userDetails.getId()
        );
    }
}
