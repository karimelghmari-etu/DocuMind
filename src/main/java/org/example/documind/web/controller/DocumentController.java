package org.example.documind.web.controller;

import lombok.AllArgsConstructor;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.service.interfaces.DocumentService;
import org.example.documind.service.interfaces.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@AllArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final UserService userService;

    @GetMapping("/{documentId}")
    public DocumentDTO getDocumentById(@PathVariable Long documentId) {
        Long ownerId = userService.getCurrentUserId();
        return documentService.getDocumentById(documentId, ownerId);
    }
    @GetMapping
    public List<DocumentDTO> getDocumentsByOwnerId(@RequestParam(required = false) DocumentStatus status) {
        Long ownerId = userService.getCurrentUserId();
        if (status != null) {
            return documentService.getDocumentByOwnerIdAndStatus(ownerId, status);
        }

        return documentService.getDocumentsByOwnerId(ownerId);
    }

}
