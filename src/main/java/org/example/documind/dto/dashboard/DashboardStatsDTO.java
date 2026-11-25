
package org.example.documind.dto.dashboard;

public record DashboardStatsDTO(
        long totalDocuments,
        long totalCollections,
        long totalConversations,
        long totalProcessingJobs
) {
}
