package org.example.documind.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.documind.dao.ProcessingJobRepository;
import org.example.documind.domain.entity.ProcessingJob;
import org.example.documind.domain.enums.ProcessingStatus;
import org.example.documind.service.interfaces.DocumentProcessingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.processing.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ProcessingJobScheduler {
    private final ProcessingJobRepository processingJobRepository;
    private final DocumentProcessingService documentProcessingService;

    @Scheduled(fixedDelay = 5000)
    public void processQueuedJobs(){
        List<ProcessingJob> jobs = processingJobRepository.findTop10ByStatusOrderByCreatedAtAsc(ProcessingStatus.QUEUED);

        for (ProcessingJob job : jobs){
            try {
                documentProcessingService.processJob(job.getId());
            }catch (Exception e){
                //one bad job must not stop the others; it's still QUEUED and will be retried next run
                log.error("Processing job {} failed unexpectedly", job.getId(), e);
            }
        }
    }
}
