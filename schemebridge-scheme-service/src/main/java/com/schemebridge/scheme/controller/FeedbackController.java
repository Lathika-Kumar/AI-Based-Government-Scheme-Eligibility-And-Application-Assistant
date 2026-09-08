package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.CreateFeedbackRequest;
import com.schemebridge.scheme.dto.response.FeedbackResponse;
import com.schemebridge.scheme.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Portal Feedback Management", description = "Endpoints for submitting and managing citizen portal feedback")
@SecurityRequirement(name = "BearerAuth")
public class FeedbackController {

    private final FeedbackService feedbackService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    @PostMapping("/api/feedback")
    @Operation(summary = "Submit citizen portal feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(@Valid @RequestBody CreateFeedbackRequest request) {
        String userId = getUserId();
        FeedbackResponse response = feedbackService.createFeedback(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping({"/api/feedback/my", "/api/feedback"})
    @Operation(summary = "Get feedback submitted by current citizen")
    public ResponseEntity<List<FeedbackResponse>> getMyFeedback() {
        String userId = getUserId();
        List<FeedbackResponse> response = feedbackService.getMyFeedback(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/admin/feedback")
    @Operation(summary = "Get paginated feedback records for administrators")
    public ResponseEntity<Page<FeedbackResponse>> getAdminFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<FeedbackResponse> response = feedbackService.getAdminFeedback(PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/admin/feedback/{id}/status")
    @Operation(summary = "Update feedback status (e.g. RECEIVED, REVIEWED, ACKNOWLEDGED)")
    public ResponseEntity<FeedbackResponse> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.getOrDefault("status", "REVIEWED");
        FeedbackResponse response = feedbackService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }
}
