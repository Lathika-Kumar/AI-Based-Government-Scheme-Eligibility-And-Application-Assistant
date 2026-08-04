package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.ProfileCompletionResponse;
import com.schemebridge.dto.ProfileRequest;
import com.schemebridge.dto.ProfileResponse;
import com.schemebridge.dto.ProfileSummaryResponse;
import com.schemebridge.service.CitizenProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Citizen Profile", description = "Endpoints for viewing, updating, and scoring citizen profiles")
public class CitizenProfileController {

    private final CitizenProfileService profileService;

    @GetMapping
    @Operation(summary = "Get user profile", description = "Retrieves profile information for the authenticated user.")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        ProfileResponse response = profileService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @PutMapping
    @Operation(summary = "Update user profile", description = "Updates profile information for the authenticated user.")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileRequest profileRequest) {
        ProfileResponse response = profileService.updateProfile(userDetails.getUsername(), profileRequest);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @GetMapping("/completion")
    @Operation(summary = "Get profile completion score", description = "Calculates profile completion percentage, status classification, and missing fields.")
    public ResponseEntity<ApiResponse<ProfileCompletionResponse>> getProfileCompletion(@AuthenticationPrincipal UserDetails userDetails) {
        ProfileCompletionResponse response = profileService.getProfileCompletion(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile completion calculated successfully", response));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard profile summary", description = "Retrieves lightweight profile summary payload optimized for citizen dashboard loading.")
    public ResponseEntity<ApiResponse<ProfileSummaryResponse>> getProfileSummary(@AuthenticationPrincipal UserDetails userDetails) {
        ProfileSummaryResponse response = profileService.getProfileSummary(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile summary retrieved successfully", response));
    }
}
