package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PlatformEndToEndTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Autowired
    private ApplicationReviewRepository applicationReviewRepository;

    @Autowired
    private ApplicationEventRepository applicationEventRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationReviewService applicationReviewService;

    @Autowired
    private SchemeSearchService schemeSearchService;

    @Autowired
    private SchemeRecommendationService schemeRecommendationService;

    private Scheme activeScheme;

    @BeforeEach
    public void setUp() {
        applicationRepository.deleteAll();
        applicationDocumentRepository.deleteAll();
        applicationReviewRepository.deleteAll();
        applicationEventRepository.deleteAll();
        schemeRepository.deleteAll();

        // Seed an active scheme with one mandatory document
        activeScheme = Scheme.builder()
                .schemeCode("SCH-E2E-001")
                .slug("end-to-end-fixture")
                .title(MultilingualText.builder().english("E2E Test Scheme").build())
                .status(SchemeStatus.ACTIVE)
                .requiredDocuments(List.of(
                        RequiredDocument.builder()
                                .documentCode("AADHAAR")
                                .name(MultilingualText.builder().english("Aadhaar Proof").build())
                                .mandatory(true)
                                .acceptedFormats(List.of("PDF"))
                                .build()
                ))
                .build();
        schemeRepository.save(activeScheme);
    }

    @AfterEach
    public void tearDown() {
        applicationRepository.deleteAll();
        applicationDocumentRepository.deleteAll();
        applicationReviewRepository.deleteAll();
        applicationEventRepository.deleteAll();
        if (activeScheme != null && activeScheme.getId() != null) {
            schemeRepository.deleteById(activeScheme.getId());
        }
        logout();
    }

    private void login(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toList());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testCompletePlatformLifecycleEndToEnd() {
        // 1. Citizen logs in.
        login("citizen_user", "USER");

        // 2. Citizen searches schemes.
        PagedSchemeResponse searchRes = schemeSearchService.search(
                "E2E", null, null, null, null, null, "ACTIVE", null, 0, 10, "createdAt", "DESC"
        );
        assertEquals(1, searchRes.getContent().size());
        assertEquals("SCH-E2E-001", searchRes.getContent().get(0).getSchemeCode());

        // 3. Citizen receives recommendation.
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(22)
                .build();
        PersonalizedRecommendationResponse recs = schemeRecommendationService.getRecommendations(profile, "ACTIVE", 0, 10);
        assertNotNull(recs);

        // 4. Citizen creates eligible application.
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-E2E-001")
                .profile(profile)
                .build();
        ApplicationResponse appRes = applicationService.createApplication(req, "citizen_user");
        assertNotNull(appRes);
        assertEquals(ApplicationStatus.DOCUMENTS_PENDING.name(), appRes.getStatus());
        assertEquals(1, appRes.getDocumentReadiness().getTotal());
        assertEquals(0, appRes.getDocumentReadiness().getUploaded());

        // 5. Citizen uploads mandatory document.
        byte[] validPdf = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"
                + "Official Government Document Aadhaar Simulation Test Fixture with Valid Minimum Byte Length\n"
                + "%%EOF\n").getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "aadhaar.pdf", "application/pdf", validPdf);
        ApplicationDocumentResponse uploadRes = applicationService.uploadDocument(
                appRes.getId(), "AADHAAR", file, "citizen_user"
        );
        assertNotNull(uploadRes);
        assertTrue(uploadRes.isUploaded());
        assertEquals("PENDING", uploadRes.getVerificationStatus());

        // 6. Verify readiness is updated. Application becomes READY_FOR_SUBMISSION automatically.
        ApplicationResponse readyApp = applicationService.getApplicationDetails(appRes.getId(), "citizen_user");
        assertEquals(ApplicationStatus.READY_FOR_SUBMISSION.name(), readyApp.getStatus());
        assertEquals(1, readyApp.getDocumentReadiness().getUploaded());

        // 7. Citizen submits application.
        ApplicationResponse submittedApp = applicationService.submitApplication(appRes.getId(), "citizen_user");
        assertEquals(ApplicationStatus.SUBMITTED.name(), submittedApp.getStatus());
        assertNotNull(submittedApp.getSubmittedAt());

        // 8. Admin logs in.
        login("admin_officer", "ADMIN");

        // 9. Admin retrieves queue.
        PagedApplicationResponse queue = applicationReviewService.getApplicationsQueue(
                "SUBMITTED", "SCH-E2E-001", 0, 10, "createdAt", "DESC"
        );
        assertEquals(1, queue.getContent().size());
        assertEquals(submittedApp.getId(), queue.getContent().get(0).getId());

        // 10. Admin starts review.
        ApplicationResponse reviewStartApp = applicationReviewService.startReview(submittedApp.getId(), "admin_officer", "ROLE_ADMIN");
        assertEquals(ApplicationStatus.UNDER_REVIEW.name(), reviewStartApp.getStatus());

        // Verify ApplicationReview entity was created
        ApplicationReview reviewObj = applicationReviewRepository.findByApplicationId(submittedApp.getId()).orElse(null);
        assertNotNull(reviewObj);
        assertEquals("admin_officer", reviewObj.getReviewerId());
        assertEquals("ROLE_ADMIN", reviewObj.getReviewerRole());
        assertEquals("UNDER_REVIEW", reviewObj.getReviewStatus());

        // 11. Admin rejects the mandatory Aadhaar document with a reason.
        ApplicationResponse rejectedDocApp = applicationReviewService.rejectDocument(
                submittedApp.getId(), "AADHAAR", "Uploaded document image is blurred.", "admin_officer"
        );
        // Rejection must transition application to CORRECTION_REQUIRED
        assertEquals(ApplicationStatus.CORRECTION_REQUIRED.name(), rejectedDocApp.getStatus());
        assertEquals("REJECTED", rejectedDocApp.getDocuments().get(0).getVerificationStatus());
        assertEquals("Uploaded document image is blurred.", rejectedDocApp.getDocuments().get(0).getRejectionReason());

        // 12. Citizen logs back in.
        login("citizen_user", "USER");

        // 13. Verify citizen sees rejection reason and CORRECTION_REQUIRED status.
        ApplicationResponse citizenViewApp = applicationService.getApplicationDetails(appRes.getId(), "citizen_user");
        assertEquals(ApplicationStatus.CORRECTION_REQUIRED.name(), citizenViewApp.getStatus());
        assertEquals("Uploaded document image is blurred.", citizenViewApp.getDocuments().get(0).getRejectionReason());

        // 14. Citizen uploads a new/corrected document.
        MockMultipartFile correctedFile = new MockMultipartFile("file", "aadhaar_clear.pdf", "application/pdf", validPdf);
        ApplicationDocumentResponse reuploadRes = applicationService.uploadDocument(
                appRes.getId(), "AADHAAR", correctedFile, "citizen_user"
        );
        // Verify verification status is reset to PENDING and rejectionReason is cleared
        assertEquals("PENDING", reuploadRes.getVerificationStatus());
        assertNull(reuploadRes.getRejectionReason());

        // 15. Verify status has transitioned back to READY_FOR_SUBMISSION after corrected upload
        ApplicationResponse updatedReadyApp = applicationService.getApplicationDetails(appRes.getId(), "citizen_user");
        assertEquals(ApplicationStatus.READY_FOR_SUBMISSION.name(), updatedReadyApp.getStatus());

        // 16. Citizen resubmits application.
        ApplicationResponse resubmittedApp = applicationService.submitApplication(appRes.getId(), "citizen_user");
        assertEquals(ApplicationStatus.SUBMITTED.name(), resubmittedApp.getStatus());

        // 17. Admin logs in again.
        login("admin_officer", "ADMIN");

        // 18. Admin starts/re-enters review.
        applicationReviewService.startReview(appRes.getId(), "admin_officer", "ROLE_ADMIN");

        // 19. Admin verifies the mandatory Aadhaar document.
        ApplicationResponse verifiedDocApp = applicationReviewService.verifyDocument(appRes.getId(), "AADHAAR", "admin_officer");
        assertEquals("VERIFIED", verifiedDocApp.getDocuments().get(0).getVerificationStatus());

        // 20. Admin approves the application.
        ApplicationResponse approvedApp = applicationReviewService.approveApplication(
                appRes.getId(), "All verified.", "admin_officer", "ROLE_ADMIN"
        );
        assertEquals(ApplicationStatus.APPROVED.name(), approvedApp.getStatus());

        // Verify final review state
        ApplicationReview finalReview = applicationReviewRepository.findByApplicationId(appRes.getId()).orElse(null);
        assertNotNull(finalReview);
        assertEquals("APPROVED", finalReview.getReviewStatus());
        assertEquals("All verified.", finalReview.getRemarks());
        assertNotNull(finalReview.getCompletedAt());

        // 21. Citizen logs back in.
        login("citizen_user", "USER");

        // 22. Citizen retrieves final status.
        ApplicationResponse finalCitizenApp = applicationService.getApplicationDetails(appRes.getId(), "citizen_user");
        assertEquals(ApplicationStatus.APPROVED.name(), finalCitizenApp.getStatus());

        // 23. Citizen retrieves timeline.
        ApplicationTimelineResponse timeline = applicationService.getTimeline(appRes.getId(), "citizen_user");
        assertNotNull(timeline);
        assertEquals(appRes.getId(), timeline.getApplicationId());

        // Verify the chronological events logged
        List<String> eventTypes = timeline.getEvents().stream()
                .map(ApplicationTimelineEventResponse::getEventType)
                .collect(Collectors.toList());

        assertTrue(eventTypes.contains("APPLICATION_CREATED"));
        assertTrue(eventTypes.contains("DOCUMENT_UPLOADED"));
        assertTrue(eventTypes.contains("READY_FOR_SUBMISSION"));
        assertTrue(eventTypes.contains("APPLICATION_SUBMITTED"));
        assertTrue(eventTypes.contains("UNDER_REVIEW"));
        assertTrue(eventTypes.contains("DOCUMENT_REJECTED"));
        assertTrue(eventTypes.contains("CORRECTION_REQUIRED"));
        assertTrue(eventTypes.contains("DOCUMENT_VERIFIED"));
        assertTrue(eventTypes.contains("APPROVED"));

        // 24. Verify another citizen receives 403.
        login("citizen_two", "USER");
        assertThrows(SecurityException.class, () ->
                applicationService.getApplicationDetails(appRes.getId(), "citizen_two")
        );
        assertThrows(SecurityException.class, () ->
                applicationService.getTimeline(appRes.getId(), "citizen_two")
        );

        // 25. Verify citizen cannot access admin review APIs.
        assertThrows(IllegalStateException.class, () ->
                applicationReviewService.startReview(appRes.getId(), "citizen_two", "ROLE_USER")
        );
        assertThrows(IllegalStateException.class, () ->
                applicationReviewService.verifyDocument(appRes.getId(), "AADHAAR", "citizen_two")
        );
    }
}
