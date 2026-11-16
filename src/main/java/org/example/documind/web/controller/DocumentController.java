package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.dto.document.DocumentStatusDTO;
import org.example.documind.dto.document.DocumentUpdateDTO;
import org.example.documind.dto.document.DocumentUploadDTO;
import org.example.documind.service.interfaces.DocumentService;
import org.example.documind.service.interfaces.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDTO uploadDocument(@Valid @ModelAttribute DocumentUploadDTO documentUploadDTO){
        Long ownerId = userService.getCurrentUserId();
        return documentService.uploadDocument(documentUploadDTO, ownerId);
    }

    @PutMapping("/{documentId}")
    public DocumentDTO updateDocument(@PathVariable Long documentId, @Valid @RequestBody DocumentUpdateDTO documentUpdateDTO){
        Long ownerId = userService.getCurrentUserId();
        return documentService.updateDocument(documentId, documentUpdateDTO, ownerId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable Long documentId){
        Long ownerId = userService.getCurrentUserId();
        documentService.deleteDocument(documentId, ownerId);
    }

    @GetMapping("/{documentId}/status")
    public DocumentStatusDTO getDocumentStatus(@PathVariable Long documentId){
        Long ownerId = userService.getCurrentUserId();
        return documentService.getDocumentStatus(documentId, ownerId);
    }
}
