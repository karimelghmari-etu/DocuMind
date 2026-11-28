package org.example.documind.service.interfaces;

import org.example.documind.dto.collection.CollectionDTO;
import org.example.documind.dto.collection.CollectionRequestDTO;
import java.util.List;
import java.util.Set;

public interface CollectionService {
    List<CollectionDTO> getCollections(Long ownerId);
    CollectionDTO getCollectionById(Long collectionId, Long ownerId);
    CollectionDTO createCollection(CollectionRequestDTO collectionRequestDTO, Long ownerId);
    CollectionDTO updateCollection(Long collectionId, CollectionRequestDTO collectionRequestDTO, Long ownerId);
    void deleteCollection(Long collectionId, Long ownerId);
    CollectionDTO addDocumentToCollection(Long collectionId, Long documentId, Long ownerId);
    CollectionDTO removeDocumentFromCollection(Long collectionId, Long documentId, Long ownerId);

    /** IDs of the documents in this collection, so a page can split "in it" from "not in it". */
    Set<Long> getDocumentIds(Long collectionId, Long ownerId);
}
