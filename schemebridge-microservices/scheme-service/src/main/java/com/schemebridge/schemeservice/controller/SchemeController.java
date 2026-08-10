package com.schemebridge.schemeservice.controller;

import com.schemebridge.schemeservice.dto.*;
import com.schemebridge.schemeservice.entity.SchemeCategory;
import com.schemebridge.schemeservice.entity.SchemeDepartment;
import com.schemebridge.schemeservice.repository.SchemeCategoryRepository;
import com.schemebridge.schemeservice.repository.SchemeDepartmentRepository;
import com.schemebridge.schemeservice.service.*;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
@Tag(name = "Scheme Catalog", description = "Scheme catalog, eligibility evaluation, trending, featured, and search APIs")
public class SchemeController {

    private final SchemeService schemeService;
    private final EligibilityRuleService eligibilityRuleService;
    private final TrendingSchemeService trendingSchemeService;
    private final RecommendationSupportService recommendationSupportService;
    private final SchemeCategoryRepository categoryRepository;
    private final SchemeDepartmentRepository departmentRepository;

    // -------------------------
    // Scheme CRUD
    // -------------------------

    @PostMapping
    @Operation(summary = "Create Scheme", description = "Creates a new government scheme entry in Oracle with multilingual metadata and recommendation fields")
    public ResponseEntity<ApiResponse<SchemeResponse>> createScheme(@Valid @RequestBody SchemeRequest request) {
        SchemeResponse response = schemeService.createScheme(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Scheme created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Scheme By ID", description = "Retrieves a scheme by its UUID, including eligibility rules, benefits, documents, FAQs, and tags")
    public ResponseEntity<ApiResponse<SchemeResponse>> getSchemeById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Scheme retrieved successfully", schemeService.getSchemeById(id)));
    }

    @GetMapping("/code/{schemeCode}")
    @Operation(summary = "Get Scheme By Code", description = "Retrieves an active scheme by its unique scheme code (e.g. PM_KISAN_2026)")
    public ResponseEntity<ApiResponse<SchemeResponse>> getSchemeByCode(@PathVariable String schemeCode) {
        return ResponseEntity.ok(ApiResponse.success("Scheme retrieved successfully", schemeService.getSchemeByCode(schemeCode)));
    }

    @GetMapping
    @Operation(summary = "Search Schemes", description = "Searches active schemes with optional filters: keyword, categoryCode, schemeType, featured. Supports pagination and sorting.")
    public ResponseEntity<ApiResponse<Page<SchemeResponse>>> searchSchemes(
            @RequestParam(required = false) @Parameter(description = "Search keyword in scheme title") String keyword,
            @RequestParam(required = false) @Parameter(description = "Category code e.g. AGRICULTURE") String categoryCode,
            @RequestParam(required = false) @Parameter(description = "CENTRAL, STATE, CENTRALLY_SPONSORED") String schemeType,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "popularityScore") String sortBy) {
        Page<SchemeResponse> results = schemeService.searchSchemes(keyword, categoryCode, schemeType, featured, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success("Scheme search completed", results));
    }

    @GetMapping("/all")
    @Operation(summary = "Get All Active Schemes", description = "Returns all active schemes ordered by priority and popularity score")
    public ResponseEntity<ApiResponse<List<SchemeResponse>>> getAllActiveSchemes() {
        return ResponseEntity.ok(ApiResponse.success("All active schemes retrieved", schemeService.getAllActiveSchemes()));
    }

    // -------------------------
    // Featured & Newly Added
    // -------------------------

    @GetMapping("/featured")
    @Operation(summary = "Get Featured Schemes", description = "Returns schemes marked as featured=true for homepage banners and promotions")
    public ResponseEntity<ApiResponse<List<SchemeResponse>>> getFeaturedSchemes() {
        return ResponseEntity.ok(ApiResponse.success("Featured schemes retrieved", schemeService.getFeaturedSchemes()));
    }

    @GetMapping("/newly-added")
    @Operation(summary = "Get Newly Added Schemes", description = "Returns recently added schemes marked as newlyAdded=true")
    public ResponseEntity<ApiResponse<List<SchemeResponse>>> getNewlyAddedSchemes() {
        return ResponseEntity.ok(ApiResponse.success("Newly added schemes retrieved", schemeService.getNewlyAddedSchemes()));
    }

    // -------------------------
    // Trending
    // -------------------------

    @GetMapping("/trending")
    @Operation(summary = "Get Trending Schemes", description = "Returns trending schemes ordered by rank position from TRENDING_SCHEMES table")
    public ResponseEntity<ApiResponse<List<SchemeResponse>>> getTrendingSchemes() {
        return ResponseEntity.ok(ApiResponse.success("Trending schemes retrieved", trendingSchemeService.getTrendingSchemes()));
    }

    // -------------------------
    // Eligibility Evaluation (Called by Citizen Service)
    // -------------------------

    @PostMapping("/evaluate-eligibility")
    @Operation(summary = "Evaluate Eligibility",
               description = "Evaluates citizen profile data against dynamic Oracle eligibility rules. " +
                             "Called by Citizen Service via EligibilityServiceClient. " +
                             "Returns matched schemes, eligibility score, and evaluation notes.")
    public ResponseEntity<ApiResponse<EligibilityEvaluationResponse>> evaluateEligibility(
            @RequestBody EligibilityEvaluationRequest request) {
        EligibilityEvaluationResponse response = eligibilityRuleService.evaluateEligibility(request);
        return ResponseEntity.ok(ApiResponse.success("Eligibility evaluation completed", response));
    }

    // -------------------------
    // Recommendations
    // -------------------------

    @GetMapping("/recommendations")
    @Operation(summary = "Get Recommended Schemes", description = "Returns schemes recommended based on category and income for the Citizen Service integration")
    public ResponseEntity<ApiResponse<List<SchemeResponse>>> getRecommendedSchemes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) java.math.BigDecimal income) {
        return ResponseEntity.ok(ApiResponse.success("Recommended schemes retrieved",
                recommendationSupportService.getRecommendedSchemes(category, income)));
    }

    // -------------------------
    // Categories & Departments
    // -------------------------

    @GetMapping("/categories")
    @Operation(summary = "Get All Categories", description = "Returns all active scheme categories ordered by display order")
    public ResponseEntity<ApiResponse<List<SchemeCategory>>> getAllCategories() {
        List<SchemeCategory> categories = categoryRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE");
        return ResponseEntity.ok(ApiResponse.success("Scheme categories retrieved", categories));
    }

    @GetMapping("/departments")
    @Operation(summary = "Get All Departments", description = "Returns all active scheme departments and their ministry information")
    public ResponseEntity<ApiResponse<List<SchemeDepartment>>> getAllDepartments() {
        List<SchemeDepartment> departments = departmentRepository.findByStatus("ACTIVE");
        return ResponseEntity.ok(ApiResponse.success("Scheme departments retrieved", departments));
    }
}
