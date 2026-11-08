package org.example.documind.dao;

import org.example.documind.domain.entity.SourceReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceReferenceRepository extends JpaRepository<SourceReference, Long> {

    List<SourceReference> findByMessageId(Long messageId);
}