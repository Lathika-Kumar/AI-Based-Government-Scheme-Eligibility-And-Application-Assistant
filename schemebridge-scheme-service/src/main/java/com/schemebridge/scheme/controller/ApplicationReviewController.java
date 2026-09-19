package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.RejectDocumentRequest;
import com.schemebridge.scheme.dto.request.ReviewActionRequest;
import com.schemebridge.scheme.dto.request.ReviewDecisionRequest;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.dto.response.PagedApplicationResponse;
import com.schemebridge.scheme.service.ApplicationReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@Tag(name = "Application Review Management", description = "Administrative endpoints for application queue review and document verification")
@SecurityRequirement(name = "BearerAuth")
public class ApplicationReviewController {

    private final ApplicationReviewService applicationReviewService;

    private String getReviewerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getReviewerRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_SCHEME_MANAGER"))
                    .findFirst()
                    .orElse("ROLE_USER");
        }
        return "ROLE_USER";
    }

    @GetMapping
    @Operation(summary = "Get the paginated administrative review queue")
    public ResponseEntity<PagedApplicationResponse> getApplicationsQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String schemeCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PagedApplicationResponse response = applicationReviewService.getApplicationsQueue(status, schemeCode, page, size, sort, direction);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{applicationId}")
    @Operation(summary = "Get exact application details for administrative review")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable String applicationId) {
        ApplicationResponse response = applicationReviewService.getApplicationForReview(applicationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{applicationId}/review/start")
    @Operation(summary = "Start application review and move status to UNDER_REVIEW")
    public ResponseEntity<ApplicationResponse> startReview(@PathVariable String applicationId) {
        String reviewerId = getReviewerId();
        String reviewerRole = getReviewerRole();
        ApplicationResponse response = applicationReviewService.startReview(applicationId, reviewerId, reviewerRole);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/documents/{documentCode}/verify")
    @Operation(summary = "Approve and verify a citizen application document")
    public ResponseEntity<ApplicationResponse> verifyDocument(
            @PathVariable String applicationId,
            @PathVariable String documentCode
    ) {
        String reviewerId = getReviewerId();
        ApplicationResponse response = applicationReviewService.verifyDocument(applicationId, documentCode, reviewerId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/documents/{documentCode}/reject")
    @Operation(summary = "Reject a citizen application document with a mandatory reason")
    public ResponseEntity<ApplicationResponse> rejectDocument(
            @PathVariable String applicationId,
            @PathVariable String documentCode,
            @Valid @RequestBody RejectDocumentRequest request
    ) {
        String reviewerId = getReviewerId();
        ApplicationResponse response = applicationReviewService.rejectDocument(applicationId, documentCode, request.getReason(), reviewerId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/documents/{documentCode}/correction")
    @Operation(summary = "Request correction/re-upload of a citizen application document")
    public ResponseEntity<ApplicationResponse> requestDocumentCorrection(
            @PathVariable String applicationId,
            @PathVariable String documentCode,
            @Valid @RequestBody RejectDocumentRequest request
    ) {
        String reviewerId = getReviewerId();
        ApplicationResponse response = applicationReviewService.requestDocumentCorrection(applicationId, documentCode, request.getReason(), reviewerId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/review")
    @Operation(summary = "Execute administrative review decision: APPROVE, REJECT, or REQUEST_MORE_DOCUMENTS")
    public ResponseEntity<ApplicationResponse> reviewApplication(
            @PathVariable String applicationId,
            @Valid @RequestBody(required = false) ReviewActionRequest request
    ) {
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            throw new IllegalArgumentException("Review action is mandatory. Supported actions: APPROVE, REJECT, REQUEST_MORE_DOCUMENTS");
        }

        String reviewerId = getReviewerId();
        String reviewerRole = getReviewerRole();
        String action = request.getAction().trim().toUpperCase();

        String effectiveRemarks = StringUtils.hasText(request.getRemarks())
                ? request.getRemarks()
                : (StringUtils.hasText(request.getReviewerNotes())
                ? request.getReviewerNotes()
                : (StringUtils.hasText(request.getReason())
                ? request.getReason()
                : request.getCorrectionReason()));

        ApplicationResponse response;
        switch (action) {
            case "APPROVE":
            case "APPROVED":
                String approveRemarks = StringUtils.hasText(effectiveRemarks) ? effectiveRemarks : "Approved";
                response = applicationReviewService.approveApplication(applicationId, approveRemarks, reviewerId, reviewerRole);
                break;
            case "REJECT":
            case "REJECTED":
                if (!StringUtils.hasText(effectiveRemarks)) {
                    throw new IllegalArgumentException("Rejection remarks are mandatory.");
                }
                response = applicationReviewService.rejectApplication(applicationId, effectiveRemarks, reviewerId, reviewerRole);
                break;
            case "REQUEST_MORE_DOCUMENTS":
            case "REQUEST_DOCUMENTS":
            case "CORRECTION_REQUIRED":
            case "CORRECTION":
                if (!StringUtils.hasText(effectiveRemarks)) {
                    throw new IllegalArgumentException("Correction/document request reason is mandatory.");
                }
                response = applicationReviewService.requestMoreDocuments(applicationId, effectiveRemarks, reviewerId, reviewerRole);
                break;
            case "START":
            case "START_REVIEW":
                response = applicationReviewService.startReview(applicationId, reviewerId, reviewerRole);
                break;
            default:
                throw new IllegalArgumentException("Invalid review action: '" + request.getAction() + "'. Supported actions are: APPROVE, REJECT, REQUEST_MORE_DOCUMENTS");
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/review/request-documents")
    @Operation(summary = "Request more documents or corrections for an application")
    public ResponseEntity<ApplicationResponse> requestMoreDocuments(
            @PathVariable String applicationId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        String reviewerId = getReviewerId();
        String reviewerRole = getReviewerRole();
        if (request == null || request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new IllegalArgumentException("Correction/document request reason is mandatory.");
        }
        ApplicationResponse response = applicationReviewService.requestMoreDocuments(applicationId, request.getRemarks(), reviewerId, reviewerRole);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/review/approve")
    @Operation(summary = "Approve a completed application (all mandatory documents must be verified)")
    public ResponseEntity<ApplicationResponse> approveApplication(
            @PathVariable String applicationId,
            @Valid @RequestBody(required = false) ReviewDecisionRequest request
    ) {
        String reviewerId = getReviewerId();
        String reviewerRole = getReviewerRole();
        String remarks = (request != null) ? request.getRemarks() : "Approved";
        ApplicationResponse response = applicationReviewService.approveApplication(applicationId, remarks, reviewerId, reviewerRole);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/review/reject")
    @Operation(summary = "Reject a citizen application with a mandatory decision reason")
    public ResponseEntity<ApplicationResponse> rejectApplication(
            @PathVariable String applicationId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        String reviewerId = getReviewerId();
        String reviewerRole = getReviewerRole();
        if (request == null || request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new IllegalArgumentException("Rejection remarks are mandatory.");
        }
        ApplicationResponse response = applicationReviewService.rejectApplication(applicationId, request.getRemarks(), reviewerId, reviewerRole);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
