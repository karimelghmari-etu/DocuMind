package org.example.documind.service.impl;

import lombok.AllArgsConstructor;
import org.example.documind.dao.CollectionRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.ProcessingJobRepository;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.Collection;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.ProcessingJob;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.dto.document.DocumentUpdateDTO;
import org.example.documind.dto.document.DocumentUploadDTO;
import org.example.documind.service.interfaces.DocumentService;
import org.example.documind.service.interfaces.FileStorageService;
import org.example.documind.web.exception.DocumentNotFoundException;
import org.example.documind.web.exception.FileStorageException;
import org.example.documind.web.exception.InvalidFileException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final CollectionRepository collectionRepository;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

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

    @Override
    @Transactional
    public DocumentDTO uploadDocument(DocumentUploadDTO documentUploadDTO, Long ownerId) {
        String title;
        String storedPath;

        MultipartFile file = documentUploadDTO.getFile();
        if (file == null || file.isEmpty()){
            throw new InvalidFileException("File is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)){
            throw new InvalidFileException("Unsupported file type");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("File name is required");
        }

        User user = userRepository.findById(ownerId).orElseThrow(() -> new IllegalStateException("User not found"));

        if (documentUploadDTO.getTitle() == null || documentUploadDTO.getTitle().isBlank()){
            title = originalFilename;
        } else {
            title = documentUploadDTO.getTitle();
        }

        storedPath = fileStorageService.store(file, ownerId);
        try {
            Document document = Document.builder()
                    .title(title)
                    .filename(originalFilename)
                    .filePath(storedPath)
                    .contentType(contentType)
                    .fileSize(file.getSize())
                    .owner(user)
                    .build();
            Document saved = documentRepository.save(document);

            ProcessingJob processingJob = ProcessingJob.builder()
                    .document(saved)
                    .build();
            processingJobRepository.save(processingJob);

            return mapToDTO(saved);
        } catch (RuntimeException e) {
            fileStorageService.delete(storedPath);
            throw e;
        }
    }

    @Override
    @Transactional
    public DocumentDTO updateDocument(Long documentId, DocumentUpdateDTO documentUpdateDTO, Long ownerId) {
        Document document = documentRepository.findByIdAndOwnerId(documentId, ownerId).orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        document.setTitle(documentUpdateDTO.getTitle());
        return mapToDTO(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId, Long ownerId) {
        Document document = documentRepository.findByIdAndOwnerId(documentId, ownerId).orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        for (Collection collection : collectionRepository.findByDocumentsId(documentId)){
            collection.getDocuments().remove(document);
        }
        documentRepository.delete(document);
        documentRepository.flush();
        try {
            fileStorageService.delete(document.getFilePath());
        } catch (FileStorageException e){
            // the file couldnt be deleted; the database delete still goes ahead
        }
    }
}

