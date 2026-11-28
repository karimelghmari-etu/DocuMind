
package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.documind.dto.document.DocumentUploadDTO;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DocumentsWebController {

    private final DocumentService documentService;

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

        return "documents";
    }

    @PostMapping(
            value = "/documents/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String uploadDocument(
            @Valid @ModelAttribute DocumentUploadDTO documentUploadDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        documentService.uploadDocument(
                documentUploadDTO,
                userDetails.getId()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Document uploaded successfully!"
        );

        return "redirect:/documents";
    }

    @PostMapping("/documents/{documentId}/delete")
    public String deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        documentService.deleteDocument(
                documentId,
                userDetails.getId()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Document deleted successfully!"
        );

        return "redirect:/documents";
    }

    @PostMapping("/documents/{documentId}/reprocess")
    public String reprocessDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        documentService.reprocessDocument(
                documentId,
                userDetails.getId()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Document reprocessing requested!"
        );

        return "redirect:/documents";
    }

}
