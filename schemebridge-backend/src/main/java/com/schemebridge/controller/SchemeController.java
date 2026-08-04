package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.CategoryResponse;
import com.schemebridge.dto.EligibilityCheckResponse;
import com.schemebridge.dto.SchemeCardResponse;
import com.schemebridge.dto.SchemeRecommendationResponse;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.enums.SchemeType;
import com.schemebridge.service.SchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
@Tag(name = "Schemes & Eligibility", description = "Public and Citizen endpoints for searching schemes, recommendations, and eligibility checks")
public class SchemeController {

    private final SchemeService schemeService;

    @GetMapping
    @Operation(summary = "List published schemes", description = "Retrieves paginated list of published schemes with optional filters")
    public ResponseEntity<ApiResponse<Page<SchemeCardResponse>>> listSchemes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) SchemeType type,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SchemeCardResponse> result = schemeService.listSchemes(category, type, state, featured, pageable);
        return ResponseEntity.ok(ApiResponse.success("Schemes retrieved successfully", result));
    }

    @GetMapping("/categories")
    @Operation(summary = "List scheme categories", description = "Returns active scheme categories with published scheme counts")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> categories = schemeService.getCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured schemes", description = "Returns top featured schemes for home page display")
    public ResponseEntity<ApiResponse<List<SchemeCardResponse>>> getFeaturedSchemes() {
        List<SchemeCardResponse> featured = schemeService.getFeaturedSchemes();
        return ResponseEntity.ok(ApiResponse.success("Featured schemes retrieved successfully", featured));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending schemes", description = "Returns popular schemes sorted by views and recommendation frequency")
    public ResponseEntity<ApiResponse<List<SchemeCardResponse>>> getTrendingSchemes() {
        List<SchemeCardResponse> trending = schemeService.getTrendingSchemes();
        return ResponseEntity.ok(ApiResponse.success("Trending schemes retrieved successfully", trending));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest schemes", description = "Returns recently launched government schemes")
    public ResponseEntity<ApiResponse<List<SchemeCardResponse>>> getLatestSchemes() {
        List<SchemeCardResponse> latest = schemeService.getLatestSchemes();
        return ResponseEntity.ok(ApiResponse.success("Latest schemes retrieved successfully", latest));
    }

    @GetMapping("/search")
    @Operation(summary = "Search schemes", description = "Full-text search across scheme names, short descriptions, and tags")
    public ResponseEntity<ApiResponse<Page<SchemeCardResponse>>> searchSchemes(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SchemeCardResponse> result = schemeService.searchSchemes(q, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get scheme details", description = "Retrieves complete information for a specific published scheme")
    public ResponseEntity<ApiResponse<SchemeResponse>> getSchemeDetails(@PathVariable String id) {
        SchemeResponse scheme = schemeService.getScheme(id);
        return ResponseEntity.ok(ApiResponse.success("Scheme details retrieved successfully", scheme));
    }

    @PostMapping("/recommendations")
    @Operation(summary = "Get personalized recommendations", description = "Runs deterministic rule engine to return ranked scheme recommendations for authenticated citizen")
    public ResponseEntity<ApiResponse<List<SchemeRecommendationResponse>>> getRecommendations(@AuthenticationPrincipal UserDetails userDetails) {
        List<SchemeRecommendationResponse> recommendations = schemeService.getRecommendations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Personalized recommendations calculated successfully", recommendations));
    }

    @PostMapping("/{id}/eligibility")
    @Operation(summary = "Run eligibility check", description = "Evaluates citizen profile against specific scheme rules and returns explainable pass/fail breakdown")
    public ResponseEntity<ApiResponse<EligibilityCheckResponse>> checkEligibility(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        EligibilityCheckResponse result = schemeService.checkEligibility(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Eligibility evaluation completed successfully", result));
    }
}
