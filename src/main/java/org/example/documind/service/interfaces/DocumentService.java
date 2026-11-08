package org.example.documind.service.interfaces;

import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;

import java.util.List;

public interface DocumentService {
    List<DocumentDTO> getDocumentsByOwnerId(Long ownerId);
    DocumentDTO getDocumentById(Long documentId, Long ownerId);
    List<DocumentDTO> getDocumentByOwnerIdAndStatus(Long ownerId, DocumentStatus status);
}
