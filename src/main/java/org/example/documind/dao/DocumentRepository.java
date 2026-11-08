package org.example.documind.dao;

import org.example.documind.domain.entity.Document;
import org.example.documind.domain.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerId(Long ownerId);

    Optional<Document> findByIdAndOwnerId(Long id, Long ownerId);

    List<Document> findByOwnerIdAndStatus(Long ownerId, DocumentStatus status);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}