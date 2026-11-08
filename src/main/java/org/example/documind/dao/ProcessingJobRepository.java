package org.example.documind.dao;

import org.example.documind.domain.entity.ProcessingJob;
import org.example.documind.domain.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

    Optional<ProcessingJob> findByDocumentId(Long documentId);

    List<ProcessingJob> findByStatus(ProcessingStatus status);
}