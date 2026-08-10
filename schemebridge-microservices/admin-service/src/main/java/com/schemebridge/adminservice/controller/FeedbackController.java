package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.FeedbackResolveRequest;
import com.schemebridge.adminservice.dto.FeedbackResponse;
import com.schemebridge.adminservice.enums.FeedbackStatus;
import com.schemebridge.adminservice.service.FeedbackService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/feedback")
@RequiredArgsConstructor
@Tag(name = "Citizen Feedback Monitoring", description = "Citizen Feedback Monitoring, Escalation, Officer Assignment & Resolution APIs")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PutMapping("/{feedbackId}/resolve")
    @Operation(summary = "Resolve / Update Feedback Status", description = "Assigns officer and resolves citizen feedback issue")
    public ResponseEntity<ApiResponse<FeedbackResponse>> resolveFeedback(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String feedbackId,
            @Valid @RequestBody FeedbackResolveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Feedback status updated", feedbackService.resolveFeedback(actorEmail, feedbackId, request)));
    }

    @GetMapping("/{feedbackId}")
    @Operation(summary = "Get Feedback Details", description = "Retrieves citizen feedback item by ID")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedbackById(@PathVariable String feedbackId) {
        return ResponseEntity.ok(ApiResponse.success("Feedback details retrieved", feedbackService.getFeedbackById(feedbackId)));
    }

    @GetMapping
    @Operation(summary = "List Feedback (Paginated)", description = "Retrieves paginated list of citizen feedback submissions")
    public ResponseEntity<ApiResponse<Page<FeedbackResponse>>> getAllFeedback(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_SIZE) int size) {
        Page<FeedbackResponse> result = (status != null) ? feedbackService.getFeedbackByStatus(status, page, size) : feedbackService.getAllFeedback(page, size);
        return ResponseEntity.ok(ApiResponse.success("Feedback items retrieved", result));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Feedback", description = "Searches feedback by subject or message content")
    public ResponseEntity<ApiResponse<Page<FeedbackResponse>>> searchFeedback(
            @RequestParam String q,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.success("Feedback search results retrieved", feedbackService.searchFeedback(q, page, size)));
    }

    @DeleteMapping("/{feedbackId}")
    @Operation(summary = "Delete Feedback", description = "Soft-deletes a feedback item")
    public ResponseEntity<ApiResponse<String>> deleteFeedback(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String feedbackId) {
        feedbackService.deleteFeedback(actorEmail, feedbackId);
        return ResponseEntity.ok(ApiResponse.success("Feedback deleted", feedbackId));
    }
}
