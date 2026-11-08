package org.example.documind.web.controller;

import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.service.interfaces.DocumentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{documentId}/owner/{ownerId}")
    public DocumentDTO getDocumentById(@PathVariable Long documentId, @PathVariable Long ownerId) {
        return documentService.getDocumentById(documentId, ownerId);
    }
    @GetMapping("/owner/{ownerId}")
    public List<DocumentDTO> getDocumentsByOwnerId(
            @PathVariable Long ownerId,
            @RequestParam(required = false) DocumentStatus status) {

        if (status != null) {
            return documentService.getDocumentByOwnerIdAndStatus(ownerId, status);
        }

        return documentService.getDocumentsByOwnerId(ownerId);
    }

}
