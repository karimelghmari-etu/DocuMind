package org.example.documind.service.interfaces;

import org.example.documind.dto.search.SearchResultDTO;

import java.util.List;

public interface SearchService {
    List<SearchResultDTO> search(String query, int limit, Long ownerId);
}
