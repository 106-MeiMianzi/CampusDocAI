package com.campusdoc.search.api;

import com.campusdoc.common.ApiResponse;
import com.campusdoc.search.dto.SearchHitResponse;
import com.campusdoc.search.dto.SearchRequest;
import com.campusdoc.search.service.SearchService;
import com.campusdoc.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/semantic")
    public ApiResponse<List<SearchHitResponse>> semantic(@Valid @RequestBody SearchRequest request) {
        return ApiResponse.ok(searchService.semantic(SecurityUtils.currentUserId(), request));
    }

    @PostMapping("/keyword")
    public ApiResponse<List<SearchHitResponse>> keyword(@Valid @RequestBody SearchRequest request) {
        return ApiResponse.ok(searchService.keyword(SecurityUtils.currentUserId(), request));
    }
}
