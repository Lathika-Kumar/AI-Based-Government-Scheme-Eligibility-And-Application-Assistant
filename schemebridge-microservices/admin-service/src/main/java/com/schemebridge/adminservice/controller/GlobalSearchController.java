package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.dto.GlobalSearchResponse;
import com.schemebridge.adminservice.service.GlobalSearchService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
@Tag(name = "Global System Search", description = "Global Cross-Module Search Across Officers, Announcements, Feedback, and Audit Logs")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @Operation(summary = "Global System Search", description = "Executes multi-entity search across officers, announcements, feedback, and audit logs")
    public ResponseEntity<ApiResponse<GlobalSearchResponse>> searchAll(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Global search results retrieved", globalSearchService.searchAll(q)));
    }
}
