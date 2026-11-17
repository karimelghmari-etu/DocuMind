package org.example.documind.web.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.example.documind.dto.search.SearchResultDTO;
import org.example.documind.service.interfaces.SearchService;
import org.example.documind.service.interfaces.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@AllArgsConstructor
public class SearchController {
    private final UserService userService;
    private final SearchService searchService;

    @GetMapping
    public List<SearchResultDTO> search(
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        Long ownerId = userService.getCurrentUserId();
        return searchService.search(q, limit, ownerId);
    }
}
