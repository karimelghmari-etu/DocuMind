package org.example.documind.dao;

import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    long countByDocumentId(Long documentId);

    @Query("select c from DocumentChunk c join fetch c.document d " +
            "where d.owner.id = :ownerId and d.status = :status")
    List<DocumentChunk> findSearchableChunks(@Param("ownerId") Long ownerId,
                                             @Param("status") DocumentStatus status);
}