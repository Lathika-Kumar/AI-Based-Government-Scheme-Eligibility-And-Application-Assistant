package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.SchemeStatus;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.SchemeCreateRequest;
import com.schemebridge.scheme.dto.request.SchemeUpdateRequest;
import com.schemebridge.scheme.dto.response.BulkEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.PagedSchemeResponse;
import com.schemebridge.scheme.dto.response.PersonalizedRecommendationResponse;
import com.schemebridge.scheme.dto.response.PersonalizedSchemeRecommendationResponse;
import com.schemebridge.scheme.dto.response.SchemeDraftResponse;
import com.schemebridge.scheme.dto.response.SchemeResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schemes")
@RequiredArgsConstructor
@Tag(name = "Scheme Management", description = "Endpoints for managing schemes")
public class SchemeController {

    private final SchemeService schemeService;
    private final SchemeSearchService schemeSearchService;
    private final SchemeRecommendationService schemeRecommendationService;
    private final AdminAuditService adminAuditService;
    private final GridFsDocumentStorageService gridFsDocumentStorageService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final GeminiSchemeExtractionService geminiSchemeExtractionService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private EligibilityEvaluationService eligibilityEvaluationService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.schemebridge.scheme.service.EligibleSchemeRecommendationService eligibleSchemeRecommendationService;

    private String getActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getActorRole() {
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

    private boolean isPrivileged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SCHEME_MANAGER"));
    }

    @PostMapping(value = "/extract-circular", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extract structured scheme draft from government circular PDF (Admin/Scheme Manager only)")
    public ResponseEntity<SchemeDraftResponse> extractCircular(@RequestParam("file") MultipartFile file) {
        if (!isPrivileged()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String actorId = getActorId();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "circular.pdf";

        // 1. Store original PDF in MongoDB GridFS for traceability
        String documentId = gridFsDocumentStorageService.storeCircular(file, actorId);

        // 2. Extract plain text from PDF using Apache PDFBox
        String extractedText = pdfTextExtractionService.extractText(file);

        // 3. Extract structured scheme metadata using Google Gemini (with zero-fabrication fallback)
        SchemeDraftResponse draft = geminiSchemeExtractionService.extractSchemeFromText(extractedText, originalFilename, documentId, actorId);

        adminAuditService.recordAction(actorId, getActorRole(), "CIRCULAR_PDF_EXTRACTED", "SCHEME_DRAFT", documentId,
                null, Map.of("filename", originalFilename, "status", draft.getExtractionStatus()), null, null,
                Map.of("sourceDocumentId", documentId));

        return ResponseEntity.ok(draft);
    }

    @PostMapping
    @Operation(summary = "Create a new scheme (Admin/Scheme Manager only)")
    public ResponseEntity<SchemeResponse> createScheme(@Valid @RequestBody SchemeCreateRequest request) {
        SchemeResponse response = schemeService.createScheme(request);
        adminAuditService.recordAction(getActorId(), getActorRole(), "SCHEME_CREATED", "SCHEME", response.getId(),
                null, response, null, null, java.util.Map.of("schemeCode", response.getSchemeCode()));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing scheme (Admin/Scheme Manager only)")
    public ResponseEntity<SchemeResponse> updateScheme(@PathVariable String id, @Valid @RequestBody SchemeUpdateRequest request) {
        SchemeResponse response = schemeService.updateScheme(id, request);
        adminAuditService.recordAction(getActorId(), getActorRole(), "SCHEME_UPDATED", "SCHEME", id,
                null, response, null, null, java.util.Map.of("schemeCode", response.getSchemeCode()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a scheme's status (Admin/Scheme Manager only)")
    public ResponseEntity<SchemeResponse> updateStatus(@PathVariable String id, @RequestParam String status) {
        SchemeResponse response = schemeService.updateStatus(id, status);
        adminAuditService.recordAction(getActorId(), getActorRole(), "SCHEME_STATUS_CHANGED", "SCHEME", id,
                null, java.util.Map.of("status", status), null, null, java.util.Map.of("schemeCode", response.getSchemeCode()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an existing scheme (Admin/Scheme Manager only)")
    public ResponseEntity<Void> deleteScheme(@PathVariable String id) {
        schemeService.deleteScheme(id);
        adminAuditService.recordAction(getActorId(), getActorRole(), "SCHEME_DELETED", "SCHEME", id,
                null, null, null, null, java.util.Map.of("schemeId", id));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @GetMapping
    @Operation(summary = "List schemes (Public). Note: Non-privileged users only see ACTIVE schemes.")
    public ResponseEntity<List<SchemeResponse>> getAllSchemes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoryCode) {
        
        SchemeStatus queryStatus = SchemeStatus.ACTIVE;
        if (isPrivileged()) {
            if (status != null && !status.trim().isEmpty()) {
                queryStatus = SchemeStatus.valueOf(status.toUpperCase());
            } else {
                queryStatus = null; // Retrieves all statuses for managers
            }
        }
        
        List<SchemeResponse> response = schemeService.getAllSchemes(queryStatus, categoryCode);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get scheme by ID (Public). Note: Non-privileged users only see ACTIVE schemes.")
    public ResponseEntity<SchemeResponse> getSchemeById(@PathVariable String id) {
        SchemeResponse response = schemeService.getSchemeById(id);
        if (response.getStatus() != SchemeStatus.ACTIVE && !isPrivileged()) {
            throw new ResourceNotFoundException("Scheme not found with ID: " + id);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/code/{schemeCode}")
    @Operation(summary = "Get scheme by schemeCode (Public). Note: Non-privileged users only see ACTIVE schemes.")
    public ResponseEntity<SchemeResponse> getSchemeByCode(@PathVariable String schemeCode) {
        SchemeResponse response = schemeService.getSchemeByCode(schemeCode);
        if (response.getStatus() != SchemeStatus.ACTIVE && !isPrivileged()) {
            throw new ResourceNotFoundException("Scheme not found with code: " + schemeCode);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/category/{categoryCode}")
    @Operation(summary = "Get schemes by category (Public). Note: Non-privileged users only see ACTIVE schemes.")
    public ResponseEntity<List<SchemeResponse>> getSchemesByCategory(
            @PathVariable String categoryCode,
            @RequestParam(required = false) String status) {
        
        SchemeStatus queryStatus = SchemeStatus.ACTIVE;
        if (isPrivileged()) {
            if (status != null && !status.trim().isEmpty()) {
                queryStatus = SchemeStatus.valueOf(status.toUpperCase());
            } else {
                queryStatus = null;
            }
        }
        
        List<SchemeResponse> response = schemeService.getAllSchemes(queryStatus, categoryCode);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping({"/{id}/eligibility", "/code/{schemeCode}/eligibility"})
    @Operation(summary = "Get current authenticated citizen statutory eligibility for a scheme")
    public ResponseEntity<com.schemebridge.scheme.dto.response.EligibilityEvaluationResult> getSchemeEligibility(
            @PathVariable(required = false) String id,
            @PathVariable(required = false) String schemeCode) {
        String userId = getActorId();
        String code = schemeCode;
        if (code == null && id != null) {
            try {
                var s = schemeService.getSchemeById(id);
                code = s.getSchemeCode();
            } catch (Exception e) {
                code = id;
            }
        }
        if (eligibilityEvaluationService != null && code != null) {
            var result = eligibilityEvaluationService.evaluateCitizenAgainstScheme(userId, code);
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/eligibility/evaluate")
    @Operation(summary = "Evaluate citizen eligibility for a scheme by ID")
    public ResponseEntity<EligibilityEvaluationResponse> evaluateEligibilityById(
            @PathVariable String id,
            @Valid @RequestBody CitizenEligibilityProfile profile) {
        EligibilityEvaluationResponse response = schemeService.evaluateEligibilityById(id, profile);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/code/{schemeCode}/eligibility/evaluate")
    @Operation(summary = "Evaluate citizen eligibility for a scheme by schemeCode")
    public ResponseEntity<EligibilityEvaluationResponse> evaluateEligibilityByCode(
            @PathVariable String schemeCode,
            @Valid @RequestBody CitizenEligibilityProfile profile) {
        EligibilityEvaluationResponse response = schemeService.evaluateEligibilityByCode(schemeCode, profile);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/eligibility/evaluate-all")
    @Operation(summary = "Evaluate citizen eligibility across all schemes. Non-privileged users are restricted to ACTIVE schemes.")
    public ResponseEntity<BulkEligibilityEvaluationResponse> evaluateEligibilityForAll(
            @Valid @RequestBody CitizenEligibilityProfile profile,
            @RequestParam(required = false) SchemeStatus status) {
        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, status);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter schemes with dynamic parameters and pagination (Public). Note: Non-privileged users only see ACTIVE schemes.")
    public ResponseEntity<PagedSchemeResponse> searchSchemes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String schemeLevel,
            @RequestParam(required = false) String stateOrUt,
            @RequestParam(required = false) String beneficiaryType,
            @RequestParam(required = false) String schemeType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "schemeCode") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        
        PagedSchemeResponse response = schemeSearchService.search(
                q, categoryCode, schemeLevel, stateOrUt, beneficiaryType, schemeType,
                status, tags, page, size, sort, direction
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/recommendations")
    @Operation(summary = "Get ranked personalized recommendations for a citizen profile (Authenticated only)")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> getRecommendations(
            @Valid @RequestBody CitizenEligibilityProfile profile,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (eligibleSchemeRecommendationService != null) {
            PersonalizedSchemeRecommendationResponse response = eligibleSchemeRecommendationService.getPersonalizedRecommendations(
                    profile, page, size
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        PersonalizedRecommendationResponse response = schemeRecommendationService.getRecommendations(
                profile, status, page, size
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
