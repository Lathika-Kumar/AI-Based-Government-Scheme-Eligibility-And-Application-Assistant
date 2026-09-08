package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.UpdateAdminSettingsRequest;
import com.schemebridge.scheme.dto.response.AdminSettingsResponse;
import com.schemebridge.scheme.service.AdminSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin Platform Settings", description = "Endpoints for managing system SLA, file limits, and notification preferences")
@SecurityRequirement(name = "BearerAuth")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SCHEME_MANAGER"))
                    .findFirst()
                    .orElse("ROLE_USER");
        }
        return "ROLE_USER";
    }

    @GetMapping
    @Operation(summary = "Get current administrative platform configuration (Admin only)")
    public ResponseEntity<AdminSettingsResponse> getSettings() {
        AdminSettingsResponse response = adminSettingsService.getSettings();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping
    @Operation(summary = "Update administrative platform configuration (Admin only)")
    public ResponseEntity<AdminSettingsResponse> updateSettings(@RequestBody UpdateAdminSettingsRequest request) {
        String actorId = getUserId();
        String actorRole = getRole();
        AdminSettingsResponse response = adminSettingsService.updateSettings(request, actorId, actorRole);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
