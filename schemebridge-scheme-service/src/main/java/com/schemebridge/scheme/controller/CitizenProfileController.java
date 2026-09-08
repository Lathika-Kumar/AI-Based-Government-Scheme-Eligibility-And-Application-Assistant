package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.dto.request.CitizenProfileRequest;
import com.schemebridge.scheme.dto.response.CitizenProfileResponse;
import com.schemebridge.scheme.service.CitizenProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Citizen Profile", description = "Endpoints for managing citizen profile and onboarding state")
@SecurityRequirement(name = "BearerAuth")
public class CitizenProfileController {

    private final CitizenProfileService citizenProfileService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Unauthorized request");
        }
        return auth.getName();
    }

    @GetMapping
    @Operation(summary = "Get the authenticated citizen's persistent profile and onboarding state")
    public ResponseEntity<CitizenProfileResponse> getProfile() {
        String userId = getUserId();
        CitizenProfile profile = citizenProfileService.getOrCreateProfile(userId);
        return new ResponseEntity<>(citizenProfileService.toResponse(profile), HttpStatus.OK);
    }

    @PutMapping
    @Operation(summary = "Upsert the authenticated citizen's persistent profile and onboarding state")
    public ResponseEntity<CitizenProfileResponse> updateProfile(@Valid @RequestBody CitizenProfileRequest request) {
        String userId = getUserId();
        CitizenProfile profile = citizenProfileService.upsertProfile(userId, request);
        return new ResponseEntity<>(citizenProfileService.toResponse(profile), HttpStatus.OK);
    }
}
