package com.schemebridge.notificationservice.controller;

import com.schemebridge.common.dto.ApiResponse;
import com.schemebridge.notificationservice.dto.PreferenceResponse;
import com.schemebridge.notificationservice.dto.PreferenceUpdateRequest;
import com.schemebridge.notificationservice.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Endpoints for managing user notification channels and subscription alerts")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get Notification Preferences", description = "Retrieves current channel and alert settings for the citizen")
    public ResponseEntity<ApiResponse<PreferenceResponse>> getPreferences(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId) {
        return ResponseEntity.ok(ApiResponse.success("Notification preferences retrieved", preferenceService.getPreferences(authUserId)));
    }

    @PutMapping
    @Operation(summary = "Update Notification Preferences", description = "Updates citizen channel toggles and notification alert preferences")
    public ResponseEntity<ApiResponse<PreferenceResponse>> updatePreferences(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId,
            @Valid @RequestBody PreferenceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Notification preferences updated", preferenceService.updatePreferences(authUserId, request)));
    }
}
