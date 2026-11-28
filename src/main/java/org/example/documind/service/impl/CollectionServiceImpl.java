package org.example.documind.service.impl;

import lombok.AllArgsConstructor;
import org.example.documind.dao.CollectionRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.Collection;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.User;
import org.example.documind.dto.collection.CollectionDTO;
import org.example.documind.dto.collection.CollectionRequestDTO;
import org.example.documind.service.interfaces.CollectionService;
import org.example.documind.web.exception.CollectionNotFoundException;
import org.example.documind.web.exception.DocumentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class CollectionServiceImpl implements CollectionService {
    private final CollectionRepository collectionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    private CollectionDTO mapToDTO(Collection collection){
        return new CollectionDTO(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                collection.getDocuments().size(),
                collection.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionDTO> getCollections(Long ownerId) {
        return collectionRepository.findByOwnerId(ownerId).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionDTO getCollectionById(Long collectionId, Long ownerId) {
        return collectionRepository.findByIdAndOwnerId(collectionId, ownerId).map(this::mapToDTO).orElseThrow(() -> new CollectionNotFoundException("Collection not found"));
    }

    @Override
    public CollectionDTO createCollection(CollectionRequestDTO collectionRequestDTO, Long ownerId) {
        User user = userRepository.findById(ownerId).orElseThrow(() -> new IllegalStateException("User not found"));
        Collection collection = Collection.builder()
                .name(collectionRequestDTO.getName())
                .description(collectionRequestDTO.getDescription())
                .owner(user)
                .build();
        return mapToDTO(collectionRepository.save(collection));
    }

    @Override
    public CollectionDTO updateCollection(Long collectionId, CollectionRequestDTO collectionRequestDTO, Long ownerId) {
        Collection collection = collectionRepository.findByIdAndOwnerId(collectionId, ownerId).orElseThrow(() -> new CollectionNotFoundException("Collection not found"));
        collection.setName(collectionRequestDTO.getName());
        collection.setDescription(collectionRequestDTO.getDescription());
        return mapToDTO(collection);
    }

    @Override
    public void deleteCollection(Long collectionId, Long ownerId) {
        Collection collection = collectionRepository.findByIdAndOwnerId(collectionId, ownerId).orElseThrow(() -> new CollectionNotFoundException("Collection not found"));
        collectionRepository.delete(collection);
    }

    @Override
    public CollectionDTO addDocumentToCollection(Long collectionId, Long documentId, Long ownerId) {
        Collection collection = collectionRepository.findByIdAndOwnerId(collectionId, ownerId).orElseThrow(() -> new CollectionNotFoundException("Collection not found"));
        Document document = documentRepository.findByIdAndOwnerId(documentId, ownerId).orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        collection.getDocuments().add(document);
        return mapToDTO(collection);
    }

    @Override
    public CollectionDTO removeDocumentFromCollection(Long collectionId, Long documentId, Long ownerId) {
        Collection collection = collectionRepository.findByIdAndOwnerId(collectionId, ownerId).orElseThrow(() -> new CollectionNotFoundException("Collection not found"));
        Document document = documentRepository.findByIdAndOwnerId(documentId, ownerId).orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        collection.getDocuments().remove(document);
        return mapToDTO(collection);    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getDocumentIds(Long collectionId, Long ownerId) {
        Collection collection = collectionRepository.findByIdAndOwnerId(collectionId, ownerId).orElseThrow(() -> new CollectionNotFoundException("Collection not found"));
        return collection.getDocuments().stream().map(Document::getId).collect(Collectors.toSet());
    }
}
