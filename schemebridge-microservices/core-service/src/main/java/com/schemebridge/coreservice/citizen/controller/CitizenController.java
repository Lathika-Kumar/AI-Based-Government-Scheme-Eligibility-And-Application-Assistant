package com.schemebridge.coreservice.citizen.controller;

import com.schemebridge.coreservice.citizen.dto.CitizenDashboardResponse;
import com.schemebridge.coreservice.citizen.dto.CitizenProfileRequest;
import com.schemebridge.coreservice.citizen.dto.CitizenProfileResponse;
import com.schemebridge.coreservice.citizen.service.CitizenService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/citizen")
@Tag(name = "Citizen Profile & Dashboard", description = "Endpoints for Profile CRUD, Saved Schemes, Dashboard Summary, and Search")
public class CitizenController {

    private final CitizenService citizenService;

    public CitizenController(CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    @PutMapping("/profile")
    @Operation(summary = "Create or Update Citizen Profile", description = "Upserts citizen demographic, family, education, income, occupation, and special category details in MongoDB")
    public ResponseEntity<ApiResponse<CitizenProfileResponse>> upsertProfile(
            @RequestHeader(value = "X-User-Id", defaultValue = "1fdf067e-db2a-429d-96bc-839d153c3bc8") String authUserId,
            @Valid @RequestBody CitizenProfileRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        CitizenProfileResponse response = citizenService.createOrUpdateProfile(authUserId, request, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Citizen profile updated successfully", response));
    }

    @GetMapping("/profile/me")
    @Operation(summary = "Get Current Citizen Profile", description = "Retrieves citizen profile document matching current user's authUserId")
    public ResponseEntity<ApiResponse<CitizenProfileResponse>> getMyProfile(
            @RequestHeader(value = "X-User-Id", defaultValue = "1fdf067e-db2a-429d-96bc-839d153c3bc8") String authUserId) {
        CitizenProfileResponse response = citizenService.getProfileByAuthUserId(authUserId);
        return ResponseEntity.ok(ApiResponse.success("Citizen profile retrieved successfully", response));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get Citizen Dashboard Summary", description = "Returns dashboard overview containing Profile Completion %, Eligibility Score, Saved Schemes Count, Document Readiness, and Recent Activities")
    public ResponseEntity<ApiResponse<CitizenDashboardResponse>> getDashboard(
            @RequestHeader(value = "X-User-Id", defaultValue = "1fdf067e-db2a-429d-96bc-839d153c3bc8") String authUserId) {
        CitizenDashboardResponse response = citizenService.getDashboardSummary(authUserId);
        return ResponseEntity.ok(ApiResponse.success("Citizen dashboard summary retrieved successfully", response));
    }

    @PostMapping("/saved-schemes/{schemeId}")
    @Operation(summary = "Save Scheme to Vault", description = "Adds schemeId to citizen's savedSchemes set and creates a SavedScheme object in MongoDB")
    public ResponseEntity<ApiResponse<Void>> saveScheme(
            @RequestHeader(value = "X-User-Id", defaultValue = "1fdf067e-db2a-429d-96bc-839d153c3bc8") String authUserId,
            @PathVariable("schemeId") String schemeId,
            @RequestParam(value = "source", defaultValue = "DIRECT_SEARCH") String source) {
        citizenService.saveScheme(authUserId, schemeId, source);
        return ResponseEntity.ok(ApiResponse.success("Scheme saved successfully to vault"));
    }

    @DeleteMapping("/saved-schemes/{schemeId}")
    @Operation(summary = "Remove Saved Scheme", description = "Removes schemeId from citizen's savedSchemes set in MongoDB")
    public ResponseEntity<ApiResponse<Void>> unsaveScheme(
            @RequestHeader(value = "X-User-Id", defaultValue = "1fdf067e-db2a-429d-96bc-839d153c3bc8") String authUserId,
            @PathVariable("schemeId") String schemeId) {
        citizenService.unsaveScheme(authUserId, schemeId);
        return ResponseEntity.ok(ApiResponse.success("Scheme removed from saved list"));
    }

    @GetMapping("/saved-schemes")
    @Operation(summary = "Get Saved Scheme IDs", description = "Returns list of scheme IDs saved by citizen")
    public ResponseEntity<ApiResponse<Set<String>>> getSavedSchemes(
            @RequestHeader(value = "X-User-Id", defaultValue = "1fdf067e-db2a-429d-96bc-839d153c3bc8") String authUserId) {
        Set<String> saved = citizenService.getSavedSchemes(authUserId);
        return ResponseEntity.ok(ApiResponse.success("Saved schemes retrieved successfully", saved));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Citizens", description = "Searches active citizen profiles in MongoDB by state, district, category, occupationType, farmer status, minority status, and disability")
    public ResponseEntity<ApiResponse<List<CitizenProfileResponse>>> searchCitizens(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "occupationType", required = false) String occupationType,
            @RequestParam(value = "farmer", required = false) Boolean farmer,
            @RequestParam(value = "minority", required = false) Boolean minority,
            @RequestParam(value = "disability", required = false) Boolean disability) {
        List<CitizenProfileResponse> list = citizenService.searchCitizens(state, district, category, occupationType, farmer, minority, disability);
        return ResponseEntity.ok(ApiResponse.success("Citizen search completed", list));
    }
}
