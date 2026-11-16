package org.example.documind.service.impl;

import lombok.AllArgsConstructor;
import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.ProcessingJobRepository;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.entity.ProcessingJob;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.domain.enums.ProcessingStatus;
import org.example.documind.service.interfaces.ChunkingService;
import org.example.documind.service.interfaces.DocumentProcessingService;
import org.example.documind.service.interfaces.TextExtractionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class DocumentProcessingServiceImpl implements DocumentProcessingService {
    private final ProcessingJobRepository processingJobRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ChunkingService chunkingService;
    private final TextExtractionService textExtractionService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void processJob(Long jobId) {
        JobContext ctx = startJob(jobId);
        if (ctx == null){
            return;
        }

        try {
            String text = textExtractionService.extractText(ctx.filePath());
            List<String> pieces = chunkingService.chunk(text);

            finishJob(ctx, pieces);
        }catch (Exception e){
            failJob(ctx, e);
        }
    }

    @Override
    @Transactional
    public int requeueStuckJobs() {
        List<ProcessingJob> stuck = processingJobRepository.findByStatus(ProcessingStatus.CHUNKING);
        for (ProcessingJob job : stuck){
            job.setStatus(ProcessingStatus.QUEUED);
            job.getDocument().setStatus(DocumentStatus.PENDING);
        }
        return stuck.size();
    }

    private JobContext startJob(Long jobId){
        return transactionTemplate.execute(status -> {
            ProcessingJob job = processingJobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Processing job not found: " + jobId));

            if (job.getStatus() != ProcessingStatus.QUEUED){
                return null;
            }

            job.setStatus(ProcessingStatus.CHUNKING);
            Document document = job.getDocument();
            document.setStatus(DocumentStatus.PROCESSING);

            return new JobContext(job.getId(), document.getId(), document.getFilePath());
        });
    }

    private void finishJob(JobContext ctx, List<String> pieces){
        transactionTemplate.executeWithoutResult(status -> {
            documentChunkRepository.deleteByDocumentId(ctx.documentId());

            Document document = documentRepository.getReferenceById(ctx.documentId());
            List<DocumentChunk> chunks = new ArrayList<>();
            for (int i = 0; i < pieces.size(); i++){
                chunks.add(DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(i)
                        .content(pieces.get(i))
                        .build());
            }
            documentChunkRepository.saveAll(chunks);

            ProcessingJob job = processingJobRepository.findById(ctx.jobId()).orElseThrow();
            job.setStatus(ProcessingStatus.COMPLETED);
            document.setStatus(DocumentStatus.INDEXED);
        });
    }

    private void failJob(JobContext ctx, Exception e){
        String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        transactionTemplate.executeWithoutResult(status -> {
            ProcessingJob job = processingJobRepository.findById(ctx.jobId()).orElseThrow();
            job.setStatus(ProcessingStatus.FAILED);
            job.setErrorMessage(reason);

            Document document = documentRepository.getReferenceById(ctx.documentId());
            document.setStatus(DocumentStatus.FAILED);
        });
    }
    private record JobContext(Long jobId, Long documentId, String filePath) {}
}