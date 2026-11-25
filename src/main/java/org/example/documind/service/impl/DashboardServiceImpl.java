
package org.example.documind.service.impl;

import org.example.documind.dao.CollectionRepository;
import org.example.documind.dao.ConversationRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.ProcessingJobRepository;
import org.example.documind.dto.dashboard.DashboardStatsDTO;
import org.example.documind.dto.document.DocumentDTO;
import org.example.documind.service.interfaces.DashboardService;
import org.example.documind.service.interfaces.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DocumentRepository documentRepository;
    private final CollectionRepository collectionRepository;
    private final ConversationRepository conversationRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final DocumentService documentService;

    public DashboardServiceImpl(
            DocumentRepository documentRepository,
            CollectionRepository collectionRepository,
            ConversationRepository conversationRepository,
            ProcessingJobRepository processingJobRepository,
            DocumentService documentService) {

        this.documentRepository = documentRepository;
        this.collectionRepository = collectionRepository;
        this.conversationRepository = conversationRepository;
        this.processingJobRepository = processingJobRepository;
        this.documentService = documentService;
    }

    @Override
    public DashboardStatsDTO getStatsForUser(Long userId) {

        long documents = documentRepository.countByOwnerId(userId);
        long collections = collectionRepository.countByOwnerId(userId);
        long conversations = conversationRepository.countByUserId(userId);
        long processingJobs =
                processingJobRepository.countByDocumentOwnerId(userId);

        return new DashboardStatsDTO(
                documents,
                collections,
                conversations,
                processingJobs
        );
    }

    @Override
    public List<DocumentDTO> getRecentDocuments(Long userId) {

        return documentService.getRecentDocuments(userId);
    }
}
