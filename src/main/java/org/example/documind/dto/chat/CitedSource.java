package org.example.documind.dto.chat;

import org.example.documind.dto.search.SearchResultDTO;

public record CitedSource(
        int number,
        SearchResultDTO source
) {}
