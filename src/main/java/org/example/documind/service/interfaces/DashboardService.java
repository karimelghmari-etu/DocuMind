
package org.example.documind.service.interfaces;

import org.example.documind.dto.dashboard.DashboardStatsDTO;
import org.example.documind.dto.document.DocumentDTO;

import java.util.List;

public interface DashboardService {

    DashboardStatsDTO getStatsForUser(Long userId);

    List<DocumentDTO> getRecentDocuments(Long userId);

}
