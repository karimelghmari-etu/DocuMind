package org.example.documind.service.impl;

import org.example.documind.dao.DocumentRepository;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.service.interfaces.DocumentService;
import org.example.documind.web.exception.DocumentNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;

    public DocumentServiceImpl(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    private DocumentDTO mapToDTO(Document document){
        return new DocumentDTO(
                document.getId(),
                document.getTitle(),
                document.getStatus(),
                document.getUploadedAt()
        );
    }

    @Override
    public List<DocumentDTO> getDocumentsByOwnerId(Long ownerId) {
        return documentRepository.findByOwnerId(ownerId).stream().map(this::mapToDTO).toList();
    }

    @Override
    public DocumentDTO getDocumentById(Long documentId, Long ownerId) {
        return documentRepository.findByIdAndOwnerId(documentId, ownerId)
                .map(this::mapToDTO).orElseThrow(() -> new DocumentNotFoundException("Document not found"));
    }

    @Override
    public List<DocumentDTO> getDocumentByOwnerIdAndStatus(Long ownerId, DocumentStatus status) {
        return documentRepository.findByOwnerIdAndStatus(ownerId, status).stream().map(this::mapToDTO).toList();
    }
}

