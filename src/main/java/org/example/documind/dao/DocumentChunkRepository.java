package org.example.documind.dao;

import org.example.documind.domain.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}