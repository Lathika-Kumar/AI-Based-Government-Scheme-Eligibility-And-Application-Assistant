package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.exception.DuplicateResourceException;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.CitizenVaultDocumentRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApplicationReapplyTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Autowired
    private CitizenVaultDocumentRepository citizenVaultDocumentRepository;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    private static final String CITIZEN_USER = "citizen-reapply-user-1";
    private static final String OTHER_USER = "citizen-other-user-2";
    private static final String SCHEME_CODE = "REAPPLY-TEST-SCHEME-001";

    private Scheme testScheme;

    @BeforeEach
    public void setUp() {
        // IMPORTANT: Only delete data for test-specific users — never wipe all production data.
        applicationDocumentRepository.deleteAllByUserId(CITIZEN_USER);
        applicationDocumentRepository.deleteAllByUserId(OTHER_USER);
        applicationRepository.findAllByUserId(CITIZEN_USER).forEach(applicationRepository::delete);
        applicationRepository.findAllByUserId(OTHER_USER).forEach(applicationRepository::delete);
        citizenVaultDocumentRepository.deleteAllByUserId(CITIZEN_USER);
        citizenVaultDocumentRepository.deleteAllByUserId(OTHER_USER);
        citizenProfileRepository.findByUserId(CITIZEN_USER).ifPresent(citizenProfileRepository::delete);
        citizenProfileRepository.findByUserId(OTHER_USER).ifPresent(citizenProfileRepository::delete);
        schemeRepository.findBySchemeCode(SCHEME_CODE).ifPresent(schemeRepository::delete);

        // Create a complete citizen profile for CITIZEN_USER
        CitizenProfile citizenProfile = CitizenProfile.builder()
                .userId(CITIZEN_USER)
                .displayName("Test Citizen")
                .age(25)
                .annualIncome(150000.0)
                .state("Tamil Nadu")
                .gender("Female")
                .occupation("Farmer")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .build();
        citizenProfileRepository.save(citizenProfile);

        RequiredDocument req1 = RequiredDocument.builder()
                .documentCode("AADHAAR")
                .name(MultilingualText.builder().english("Aadhaar Card").build())
                .mandatory(true)
                .acceptedFormats(List.of("PDF", "JPG"))
                .build();

        RequiredDocument req2 = RequiredDocument.builder()
                .documentCode("INCOME_CERT")
                .name(MultilingualText.builder().english("Income Certificate").build())
                .mandatory(false)
                .acceptedFormats(List.of("PDF"))
                .build();

        EligibilityCondition ageCondition = EligibilityCondition.builder()
                .field("AGE")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .required(true)
                .build();

        RuleGroup rules = RuleGroup.builder()
                .logicalOperator("ALL")
                .conditions(List.of(ageCondition))
                .build();

        testScheme = Scheme.builder()
                .schemeCode(SCHEME_CODE)
                .slug("reapply-test-scheme-001")
                .title(MultilingualText.builder().english("Reapply Test Scheme").build())
                .description(MultilingualText.builder().english("Test scheme for reapplication").build())
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(rules)
                .requiredDocuments(List.of(req1, req2))
                .version(1)
                .build();

        schemeRepository.save(testScheme);
    }

    @Test
    @DisplayName("1. Reapply rejected application -> SUCCESS (New ID, old unchanged, previousApplicationId linked)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyRejectedApplication_Success() {
        // Create initial application
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000100")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(1800))
                .build();
        orig = applicationRepository.save(orig);

        // Add a reusable MATCH + AI_VERIFIED document in vault
        CitizenVaultDocument vaultDoc = CitizenVaultDocument.builder()
                .userId(CITIZEN_USER)
                .documentCode("AADHAAR")
                .canonicalDocumentCode("AADHAAR")
                .documentName("Aadhaar Card")
                .fileName("aadhaar.pdf")
                .storageReference("/storage/aadhaar.pdf")
                .identityMatchStatus("MATCH")
                .detailedStatus(DetailedDocumentStatus.AI_VERIFIED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .uploadedAt(Instant.now())
                .build();
        citizenVaultDocumentRepository.save(vaultDoc);

        // Call reapply
        ApplicationResponse reapplyResponse = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);

        assertNotNull(reapplyResponse);
        assertNotEquals(orig.getId(), reapplyResponse.getId(), "New application must have a new ID");
        assertNotEquals(orig.getApplicationNumber(), reapplyResponse.getApplicationNumber(), "New application must have a new number");
        assertEquals(orig.getId(), reapplyResponse.getPreviousApplicationId(), "New application must link to previousApplicationId");
        assertEquals(CITIZEN_USER, reapplyResponse.getUserId());
        assertEquals(SCHEME_CODE, reapplyResponse.getSchemeCode());

        // Old application remains REJECTED in database
        Application persistedOrig = applicationRepository.findById(orig.getId()).orElseThrow();
        assertEquals(ApplicationStatus.REJECTED, persistedOrig.getStatus(), "Original application status must remain REJECTED");
        assertEquals("SB-APP-2026-000100", persistedOrig.getApplicationNumber());

        // Check document readiness and reused document
        assertNotNull(reapplyResponse.getDocumentReadiness());
        assertTrue(reapplyResponse.getDocumentReadiness().getUploaded() >= 1, "Aadhaar should have been auto-reused from vault");
    }

    @Test
    @DisplayName("2. Reapply cancelled application -> SUCCESS")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyCancelledApplication_Success() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000101")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.CANCELLED)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(1800))
                .build();
        orig = applicationRepository.save(orig);

        ApplicationResponse reapplyResponse = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);
        assertNotNull(reapplyResponse);
        assertNotEquals(orig.getId(), reapplyResponse.getId());
        assertEquals(orig.getId(), reapplyResponse.getPreviousApplicationId());

        Application persistedOrig = applicationRepository.findById(orig.getId()).orElseThrow();
        assertEquals(ApplicationStatus.CANCELLED, persistedOrig.getStatus());
    }

    @Test
    @DisplayName("3. Reapply approved application -> Succeeds (APPROVED is terminal status)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyApprovedApplication_Succeeds() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000102")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.APPROVED)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(1800))
                .build();
        orig = applicationRepository.save(orig);

        ApplicationResponse reapplyResponse = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);
        assertNotNull(reapplyResponse);
        assertNotEquals(orig.getId(), reapplyResponse.getId(), "New application must have a new ID");
        assertEquals(orig.getId(), reapplyResponse.getPreviousApplicationId(), "New application must link to previousApplicationId");

        Application persistedOrig = applicationRepository.findById(orig.getId()).orElseThrow();
        assertEquals(ApplicationStatus.APPROVED, persistedOrig.getStatus(), "Original application must remain APPROVED");
    }

    @Test
    @DisplayName("4. Reapply documents_pending application -> Throws IllegalArgumentException (400)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyPendingApplication_Fails() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000103")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.DOCUMENTS_PENDING)
                .createdAt(Instant.now())
                .build();
        orig = applicationRepository.save(orig);

        final String origId = orig.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("This application is not eligible for reapplication"));
    }

    @Test
    @DisplayName("4b. Reapply draft application -> Throws IllegalArgumentException (400)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyDraftApplication_Fails() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000103-B")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.DRAFT)
                .createdAt(Instant.now())
                .build();
        orig = applicationRepository.save(orig);

        final String origId = orig.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("This application is not eligible for reapplication"));
    }

    @Test
    @DisplayName("4c. Reapply ready_for_submission application -> Throws IllegalArgumentException (400)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyReadyForSubmissionApplication_Fails() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000103-C")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.READY_FOR_SUBMISSION)
                .createdAt(Instant.now())
                .build();
        orig = applicationRepository.save(orig);

        final String origId = orig.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("This application is not eligible for reapplication"));
    }

    @Test
    @DisplayName("4d. Reapply submitted application -> Throws IllegalArgumentException (400)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplySubmittedApplication_Fails() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000103-D")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(Instant.now())
                .build();
        orig = applicationRepository.save(orig);

        final String origId = orig.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("This application is not eligible for reapplication"));
    }

    @Test
    @DisplayName("4e. Reapply under_review application -> Throws IllegalArgumentException (400)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyUnderReviewApplication_Fails() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000103-E")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.UNDER_REVIEW)
                .createdAt(Instant.now())
                .build();
        orig = applicationRepository.save(orig);

        final String origId = orig.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("This application is not eligible for reapplication"));
    }

    @Test
    @DisplayName("4f. Reapply correction_required application -> Throws IllegalArgumentException (400)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyCorrectionRequiredApplication_Fails() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000103-F")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.CORRECTION_REQUIRED)
                .createdAt(Instant.now())
                .build();
        orig = applicationRepository.save(orig);

        final String origId = orig.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("This application is not eligible for reapplication"));
    }

    @Test
    @DisplayName("5. User reapplying another user's application -> Throws SecurityException (403)")
    @WithMockUser(username = OTHER_USER, roles = {"USER"})
    public void testReapplyOtherUsersApplication_Fails() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000104")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now())
                .build();
        orig = applicationRepository.save(orig);

        final String origId = orig.getId();
        SecurityException ex = assertThrows(SecurityException.class, () ->
                applicationService.reapplyApplication(origId, OTHER_USER)
        );
        assertTrue(ex.getMessage().contains("You are not authorized to reapply for this application"));
    }

    @Test
    @DisplayName("6. Previous application not found -> Throws ResourceNotFoundException (404)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyNonExistentApplication_Fails() {
        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.reapplyApplication("non-existent-app-id-9999", CITIZEN_USER)
        );
    }

    @Test
    @DisplayName("7. Existing active application for scheme -> Throws DuplicateResourceException (409)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyWithExistingActiveApplication_Fails() {
        // Old rejected application
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000105")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();
        orig = applicationRepository.save(orig);

        // Another active application currently UNDER_REVIEW for same scheme
        Application activeApp = Application.builder()
                .applicationNumber("SB-APP-2026-000106")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.UNDER_REVIEW)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        applicationRepository.save(activeApp);

        final String origId = orig.getId();
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("An active application already exists for this scheme."),
                "Error message should mention active application exists, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("SB-APP-2026-000106"),
                "Error message should reference the blocking application number");
    }

    @Test
    @DisplayName("8. Document Reuse: MISMATCH or REJECTED documents in vault are NEVER reused")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testDocumentReuseRules_StrictIdentityVerification() {
        // Aadhaar has MISMATCH and AI_REJECTED in vault
        CitizenVaultDocument mismatchDoc = CitizenVaultDocument.builder()
                .userId(CITIZEN_USER)
                .documentCode("AADHAAR")
                .canonicalDocumentCode("AADHAAR")
                .documentName("Aadhaar Card")
                .fileName("aadhaar_fake.pdf")
                .identityMatchStatus("MISMATCH")
                .detailedStatus(DetailedDocumentStatus.AI_REJECTED)
                .verificationStatus(DocumentVerificationStatus.REJECTED)
                .uploadedAt(Instant.now())
                .build();
        citizenVaultDocumentRepository.save(mismatchDoc);

        // Old rejected app
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000107")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        orig = applicationRepository.save(orig);

        ApplicationResponse reapplyResponse = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);
        assertNotNull(reapplyResponse);

        // Ensure Aadhaar was NOT reused into the new application
        List<ApplicationDocument> newDocs = applicationDocumentRepository.findAllByApplicationId(reapplyResponse.getId());
        ApplicationDocument aadhaarDoc = newDocs.stream()
                .filter(d -> d.getDocumentCode().equalsIgnoreCase("AADHAAR"))
                .findFirst()
                .orElseThrow();

        assertFalse(aadhaarDoc.isUploaded(), "Mismatched/rejected vault document must NOT be reused");
        assertEquals(DetailedDocumentStatus.NOT_UPLOADED, aadhaarDoc.getDetailedStatus());
        assertEquals(0, reapplyResponse.getDocumentReadiness().getUploaded(), "Readiness must reflect missing documents");
    }

    @Test
    @DisplayName("9. Document Reuse: UNCERTAIN or PENDING identity documents are NOT reused")
    public void testIsReusableVaultDocument_RejectsUncertainAndPending() {
        CitizenVaultDocument uncertainDoc = CitizenVaultDocument.builder()
                .identityMatchStatus("UNCERTAIN")
                .detailedStatus(DetailedDocumentStatus.AI_VERIFIED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .build();
        assertFalse(CitizenVaultDocumentService.isReusableVaultDocument(uncertainDoc));

        CitizenVaultDocument pendingDoc = CitizenVaultDocument.builder()
                .identityMatchStatus("PENDING")
                .detailedStatus(DetailedDocumentStatus.AI_VERIFIED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .build();
        assertFalse(CitizenVaultDocumentService.isReusableVaultDocument(pendingDoc));

        CitizenVaultDocument matchVerified = CitizenVaultDocument.builder()
                .identityMatchStatus("MATCH")
                .detailedStatus(DetailedDocumentStatus.AI_VERIFIED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .build();
        assertTrue(CitizenVaultDocumentService.isReusableVaultDocument(matchVerified));
    }

    @Test
    @DisplayName("10. CORRECTION_REQUIRED blocks reapplication (active status)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyBlockedByCorrectionRequired() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000108")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();
        orig = applicationRepository.save(orig);

        Application correctionApp = Application.builder()
                .applicationNumber("SB-APP-2026-000109")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.CORRECTION_REQUIRED)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        applicationRepository.save(correctionApp);

        final String origId = orig.getId();
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("An active application already exists for this scheme."));
        assertTrue(ex.getMessage().contains("CORRECTION_REQUIRED"));
    }

    @Test
    @DisplayName("11. DRAFT blocks reapplication (active status)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyBlockedByDraft() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000110")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();
        orig = applicationRepository.save(orig);

        Application draftApp = Application.builder()
                .applicationNumber("SB-APP-2026-000111")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.DRAFT)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        applicationRepository.save(draftApp);

        final String origId = orig.getId();
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("An active application already exists for this scheme."));
        assertTrue(ex.getMessage().contains("DRAFT"));
    }

    @Test
    @DisplayName("12. READY_FOR_SUBMISSION blocks reapplication (active status)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyBlockedByReadyForSubmission() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000112")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();
        orig = applicationRepository.save(orig);

        Application readyApp = Application.builder()
                .applicationNumber("SB-APP-2026-000113")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.READY_FOR_SUBMISSION)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        applicationRepository.save(readyApp);

        final String origId = orig.getId();
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("An active application already exists for this scheme."));
        assertTrue(ex.getMessage().contains("READY_FOR_SUBMISSION"));
    }

    @Test
    @DisplayName("13. SUBMITTED blocks reapplication (active status)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testReapplyBlockedBySubmitted() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000114")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();
        orig = applicationRepository.save(orig);

        Application submittedApp = Application.builder()
                .applicationNumber("SB-APP-2026-000115")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.SUBMITTED)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        applicationRepository.save(submittedApp);

        final String origId = orig.getId();
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("An active application already exists for this scheme."));
        assertTrue(ex.getMessage().contains("SUBMITTED"));
    }

    @Test
    @DisplayName("14. Rapid duplicate reapply -> second call blocked by newly created active application")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testRapidDuplicateReapply_Blocked() {
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000116")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        orig = applicationRepository.save(orig);

        // First reapplication succeeds
        ApplicationResponse firstReapply = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);
        assertNotNull(firstReapply);
        assertNotNull(firstReapply.getId());

        // Immediate second reapplication for same scheme MUST fail with 409 DuplicateResourceException
        final String origId = orig.getId();
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                applicationService.reapplyApplication(origId, CITIZEN_USER)
        );
        assertTrue(ex.getMessage().contains("An active application already exists for this scheme."));
        assertTrue(ex.getMessage().contains(firstReapply.getApplicationNumber()));
    }

    @Test
    @DisplayName("15. Malicious previousApplicationId referencing another user's application -> Throws SecurityException")
    @WithMockUser(username = OTHER_USER, roles = {"USER"})
    public void testCreateApplicationWithOtherUsersPreviousApp_Fails() {
        // Application belonging to CITIZEN_USER
        Application victimApp = Application.builder()
                .applicationNumber("SB-APP-2026-000117")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        victimApp = applicationRepository.save(victimApp);

        // OTHER_USER tries to create application linking to victimApp.getId()
        CreateApplicationRequest maliciousRequest = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_CODE)
                .previousApplicationId(victimApp.getId())
                .build();

        SecurityException ex = assertThrows(SecurityException.class, () ->
                applicationService.createApplication(maliciousRequest, OTHER_USER)
        );
        assertTrue(ex.getMessage().contains("You are not authorized to reference another user's application"));
    }

    @Test
    @DisplayName("16. Missing previousApplicationId target -> Throws ResourceNotFoundException (404)")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testCreateApplicationWithNonExistentPreviousApp_Fails() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_CODE)
                .previousApplicationId("non-existent-app-id-99999")
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.createApplication(req, CITIZEN_USER)
        );
    }

    @Test
    @DisplayName("17. Document Reuse: PENDING vault document is not reused")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testDocumentReuse_PendingVaultDocNotReused() {
        CitizenVaultDocument pendingDoc = CitizenVaultDocument.builder()
                .userId(CITIZEN_USER)
                .documentCode("AADHAAR")
                .canonicalDocumentCode("AADHAAR")
                .documentName("Aadhaar Card")
                .fileName("aadhaar_pending.pdf")
                .identityMatchStatus("PENDING")
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .uploadedAt(Instant.now())
                .build();
        citizenVaultDocumentRepository.save(pendingDoc);

        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000118")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        orig = applicationRepository.save(orig);

        ApplicationResponse reapply = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);
        assertNotNull(reapply);

        List<ApplicationDocument> newDocs = applicationDocumentRepository.findAllByApplicationId(reapply.getId());
        ApplicationDocument doc = newDocs.stream().filter(d -> d.getDocumentCode().equalsIgnoreCase("AADHAAR")).findFirst().orElseThrow();
        assertFalse(doc.isUploaded(), "PENDING vault document must not be reused");
    }

    @Test
    @DisplayName("18. Document Reuse: UNCERTAIN vault document is not reused")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testDocumentReuse_UncertainVaultDocNotReused() {
        CitizenVaultDocument uncertainDoc = CitizenVaultDocument.builder()
                .userId(CITIZEN_USER)
                .documentCode("AADHAAR")
                .canonicalDocumentCode("AADHAAR")
                .documentName("Aadhaar Card")
                .fileName("aadhaar_uncertain.pdf")
                .identityMatchStatus("UNCERTAIN")
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .uploadedAt(Instant.now())
                .build();
        citizenVaultDocumentRepository.save(uncertainDoc);

        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000119")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.CANCELLED)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        orig = applicationRepository.save(orig);

        ApplicationResponse reapply = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);
        assertNotNull(reapply);

        List<ApplicationDocument> newDocs = applicationDocumentRepository.findAllByApplicationId(reapply.getId());
        ApplicationDocument doc = newDocs.stream().filter(d -> d.getDocumentCode().equalsIgnoreCase("AADHAAR")).findFirst().orElseThrow();
        assertFalse(doc.isUploaded(), "UNCERTAIN vault document must not be reused");
    }

    @Test
    @DisplayName("19. previousApplicationId is correctly stored and original application remains completely unchanged")
    @WithMockUser(username = CITIZEN_USER, roles = {"USER"})
    public void testPreviousApplicationId_CorrectlyStoredAndOriginalUnchanged() {
        Instant origCreated = Instant.now().minusSeconds(7200);
        Instant origUpdated = Instant.now().minusSeconds(3600);
        Application orig = Application.builder()
                .applicationNumber("SB-APP-2026-000120")
                .userId(CITIZEN_USER)
                .schemeId(testScheme.getId())
                .schemeCode(SCHEME_CODE)
                .status(ApplicationStatus.REJECTED)
                .createdAt(origCreated)
                .updatedAt(origUpdated)
                .build();
        orig = applicationRepository.save(orig);

        ApplicationResponse reapply = applicationService.reapplyApplication(orig.getId(), CITIZEN_USER);
        assertNotNull(reapply);
        assertEquals(orig.getId(), reapply.getPreviousApplicationId());

        // Verify original application
        Application origInDb = applicationRepository.findById(orig.getId()).orElseThrow();
        assertEquals(ApplicationStatus.REJECTED, origInDb.getStatus(), "Status must remain REJECTED");
        assertEquals("SB-APP-2026-000120", origInDb.getApplicationNumber(), "Number must remain unchanged");
        assertEquals(origCreated.toEpochMilli(), origInDb.getCreatedAt().toEpochMilli(), "Created date must remain unchanged");
        assertEquals(origUpdated.toEpochMilli(), origInDb.getUpdatedAt().toEpochMilli(), "Updated date must remain unchanged");
        assertNull(origInDb.getPreviousApplicationId(), "Original previousApplicationId must remain null");
    }
}
