package org.example.documind.service.interfaces;

import org.example.documind.dto.document.DocumentDetailDTO;
import org.example.documind.dto.document.StoredFile;

import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.dto.document.DocumentStatusDTO;
import org.example.documind.dto.document.DocumentUpdateDTO;
import org.example.documind.dto.document.DocumentUploadDTO;

import java.util.List;

public interface DocumentService {
    List<DocumentDTO> getDocumentsByOwnerId(Long ownerId);
    DocumentDTO getDocumentById(Long documentId, Long ownerId);
    List<DocumentDTO> getDocumentByOwnerIdAndStatus(Long ownerId, DocumentStatus status);
    DocumentDTO uploadDocument(DocumentUploadDTO documentUploadDTO, Long ownerId);
    DocumentDTO updateDocument(Long documentId, DocumentUpdateDTO documentUpdateDTO, Long ownerId);
    void deleteDocument(Long documentId, Long ownerId);
    DocumentStatusDTO getDocumentStatus(Long documentId, Long ownerId);
    void reprocessDocument(Long documentId, Long ownerId);
    List<DocumentDTO> getRecentDocuments(Long ownerId);

    /** The document page: details, collections and indexed passages. */
    DocumentDetailDTO getDocumentDetail(Long documentId, Long ownerId);

    /** The uploaded file itself, for viewing, printing or downloading. */
    StoredFile getDocumentFile(Long documentId, Long ownerId);
}
