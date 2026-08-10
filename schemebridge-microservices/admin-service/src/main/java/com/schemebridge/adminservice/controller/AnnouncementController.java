package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.AnnouncementRequest;
import com.schemebridge.adminservice.dto.AnnouncementResponse;
import com.schemebridge.adminservice.enums.AnnouncementAudience;
import com.schemebridge.adminservice.service.AnnouncementService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
@Tag(name = "System Announcements", description = "Broadcast, Schedule, Priority & Target Audience System Announcement APIs")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @Operation(summary = "Create Announcement", description = "Creates a new system announcement for broadcast or scheduled publication")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Announcement created successfully", announcementService.createAnnouncement(actorEmail, request)));
    }

    @PutMapping("/{announcementId}")
    @Operation(summary = "Update Announcement", description = "Updates content, audience, or priority of an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String announcementId,
            @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement updated successfully", announcementService.updateAnnouncement(actorEmail, announcementId, request)));
    }

    @GetMapping("/{announcementId}")
    @Operation(summary = "Get Announcement Details", description = "Retrieves announcement metadata by ID")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(@PathVariable String announcementId) {
        return ResponseEntity.ok(ApiResponse.success("Announcement details retrieved", announcementService.getAnnouncementById(announcementId)));
    }

    @GetMapping
    @Operation(summary = "List All Announcements", description = "Retrieves paginated list of announcements")
    public ResponseEntity<ApiResponse<Page<AnnouncementResponse>>> getAllAnnouncements(
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.success("Announcements list retrieved", announcementService.getAllAnnouncements(page, size)));
    }

    @GetMapping("/audience/{audience}")
    @Operation(summary = "Get Announcements for Audience", description = "Retrieves active announcements targeted to a specific audience group")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAnnouncementsForAudience(@PathVariable AnnouncementAudience audience) {
        return ResponseEntity.ok(ApiResponse.success("Audience announcements retrieved", announcementService.getAnnouncementsForAudience(audience)));
    }

    @PostMapping("/{announcementId}/broadcast")
    @Operation(summary = "Broadcast Announcement", description = "Triggers immediate system-wide broadcast for an announcement")
    public ResponseEntity<ApiResponse<String>> broadcastAnnouncement(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String announcementId) {
        announcementService.broadcastAnnouncement(actorEmail, announcementId);
        return ResponseEntity.ok(ApiResponse.success("Announcement broadcasted successfully", announcementId));
    }

    @DeleteMapping("/{announcementId}")
    @Operation(summary = "Delete Announcement", description = "Soft-deletes a system announcement")
    public ResponseEntity<ApiResponse<String>> deleteAnnouncement(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String announcementId) {
        announcementService.deleteAnnouncement(actorEmail, announcementId);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully", announcementId));
    }
}
