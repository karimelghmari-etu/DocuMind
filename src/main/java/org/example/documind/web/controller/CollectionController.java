package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.documind.dto.collection.CollectionDTO;
import org.example.documind.dto.collection.CollectionRequestDTO;
import org.example.documind.service.interfaces.CollectionService;
import org.example.documind.service.interfaces.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/collections")
@AllArgsConstructor
public class CollectionController {
    private final CollectionService collectionService;
    private final UserService userService;

    @GetMapping
    public List<CollectionDTO> getCollections(){
        Long ownerId = userService.getCurrentUserId();
        return collectionService.getCollections(ownerId);
    }

    @GetMapping("/{collectionId}")
    public CollectionDTO getCollectionById(@PathVariable Long collectionId){
        Long ownerId = userService.getCurrentUserId();
        return collectionService.getCollectionById(collectionId, ownerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionDTO createCollection(@Valid @RequestBody CollectionRequestDTO collectionRequestDTO){
        Long ownerId = userService.getCurrentUserId();
        return collectionService.createCollection(collectionRequestDTO, ownerId);
    }

    @PutMapping("/{collectionId}")
    public CollectionDTO updateCollection(@PathVariable Long collectionId, @Valid @RequestBody CollectionRequestDTO collectionRequestDTO){
        Long ownerId = userService.getCurrentUserId();
        return collectionService.updateCollection(collectionId, collectionRequestDTO, ownerId);
    }

    @DeleteMapping("/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(@PathVariable Long collectionId){
        Long ownerId = userService.getCurrentUserId();
        collectionService.deleteCollection(collectionId, ownerId);
    }

    @PostMapping("/{collectionId}/documents/{documentId}")
    public CollectionDTO addDocumentToCollection(@PathVariable Long collectionId, @PathVariable Long documentId){
        Long ownerId = userService.getCurrentUserId();
        return collectionService.addDocumentToCollection(collectionId, documentId, ownerId);
    }

    @DeleteMapping("/{collectionId}/documents/{documentId}")
    public CollectionDTO removeDocumentFromCollection(@PathVariable Long collectionId, @PathVariable Long documentId){
        Long ownerId = userService.getCurrentUserId();
        return collectionService.removeDocumentFromCollection(collectionId, documentId, ownerId);
    }
}
