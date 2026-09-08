package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.dto.response.ApplicationStepsResponse;
import com.schemebridge.scheme.dto.response.DocumentChecklistItemResponse;
import com.schemebridge.scheme.dto.response.SchemeDocumentChecklistResponse;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.service.ApplicationService;
import com.schemebridge.scheme.service.DocumentChecklistGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Checklist", description = "Endpoints for retrieving scheme and application document requirement checklists and application steps")
public class DocumentChecklistController {

    private final ApplicationService applicationService;
    private final DocumentChecklistGenerator checklistGenerator;
    private final SchemeRepository schemeRepository;
    private final CitizenProfileRepository citizenProfileRepository;

    @GetMapping("/applications/{applicationId}/document-checklist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get document checklist and status for a specific application")
    public ResponseEntity<SchemeDocumentChecklistResponse> getApplicationDocumentChecklist(
            @PathVariable String applicationId
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "anonymous";
        boolean isPrivileged = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_SCHEME_MANAGER")
                            || a.getAuthority().equals("ROLE_VERIFICATION_OFFICER"));

        SchemeDocumentChecklistResponse response = applicationService.getDocumentChecklist(applicationId, userId, isPrivileged);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/schemes/{schemeCode}/document-checklist", "/schemes/{schemeCode}/checklist"})
    @Operation(summary = "Get required document checklist definition for a scheme (by schemeCode or ID)")
    public ResponseEntity<SchemeDocumentChecklistResponse> getSchemeDocumentChecklist(
            @PathVariable String schemeCode
    ) {
        SchemeDocumentChecklistResponse response = resolveChecklist(schemeCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/schemes/{schemeCode}/application-steps", "/schemes/id/{schemeId}/application-steps"})
    @Operation(summary = "Get canonical application steps and official links for a scheme")
    public ResponseEntity<ApplicationStepsResponse> getSchemeApplicationSteps(
            @PathVariable(required = false) String schemeCode,
            @PathVariable(required = false) String schemeId
    ) {
        String identifier = schemeCode != null ? schemeCode : schemeId;
        SchemeDocumentChecklistResponse checklist = resolveChecklist(identifier);
        List<String> docNames = checklist.getItems() != null
                ? checklist.getItems().stream().map(DocumentChecklistItemResponse::getDocumentName).toList()
                : List.of();

        ApplicationStepsResponse response = ApplicationStepsResponse.builder()
                .schemeCode(checklist.getSchemeCode())
                .schemeTitle(checklist.getSchemeTitle())
                .applicationMode(checklist.getApplicationMode())
                .officialApplicationUrl(checklist.getOfficialApplicationUrl())
                .helplineNumber(checklist.getHelplineNumber())
                .applicationSteps(checklist.getApplicationSteps())
                .requiredDocuments(docNames)
                .benefits(checklist.getBenefits())
                .build();
        return ResponseEntity.ok(response);
    }

    private SchemeDocumentChecklistResponse resolveChecklist(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return applicationService.getSchemeDocumentChecklist(identifier);
        }

        // Try lookup by schemeCode, then by MongoDB ID
        Optional<Scheme> schemeOpt = schemeRepository.findBySchemeCode(identifier);
        if (schemeOpt.isEmpty()) {
            schemeOpt = schemeRepository.findById(identifier);
        }

        if (schemeOpt.isPresent()) {
            Scheme scheme = schemeOpt.get();
            CitizenProfile currentProfile = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                currentProfile = citizenProfileRepository.findByUserId(auth.getName()).orElse(null);
            }
            return checklistGenerator.generateChecklist(currentProfile, scheme);
        }

        // Fallback to ApplicationService
        return applicationService.getSchemeDocumentChecklist(identifier);
    }
}
