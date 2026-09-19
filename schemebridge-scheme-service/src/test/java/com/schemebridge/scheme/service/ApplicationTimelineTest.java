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
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApplicationTimelineTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Autowired
    private ApplicationEventRepository applicationEventRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ApplicationService applicationService;

    private Scheme activeScheme;

    @BeforeEach
    public void setUp() {
        applicationRepository.deleteAll();
        applicationDocumentRepository.deleteAll();
        applicationEventRepository.deleteAll();
        schemeRepository.findBySchemeCode("SCH-TIME-01").ifPresent(schemeRepository::delete);

        // Seed an active scheme with one mandatory document
        activeScheme = Scheme.builder()
                .schemeCode("SCH-TIME-01")
                .slug("timeline-test-scheme")
                .title(MultilingualText.builder().english("Timeline Scheme").build())
                .status(SchemeStatus.ACTIVE)
                .requiredDocuments(List.of(
                        RequiredDocument.builder()
                                .documentCode("IDENTITY")
                                .name(MultilingualText.builder().english("Identity Proof").build())
                                .mandatory(true)
                                .acceptedFormats(List.of("PDF"))
                                .build()
                ))
                .build();
        schemeRepository.save(activeScheme);
    }

    @Test
    @WithMockUser(username = "citizen1", roles = "USER")
    public void testApplicationCreatedEventAndInitialStatus() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-TIME-01")
                .profile(CitizenEligibilityProfile.builder().age(25).build())
                .build();

        ApplicationResponse res = applicationService.createApplication(req, "citizen1");

        assertNotNull(res);
        assertEquals(ApplicationStatus.DOCUMENTS_PENDING.name(), res.getStatus());

        // Check event logged
        List<ApplicationEvent> events = applicationEventRepository.findAllByApplicationIdOrderByCreatedAtAsc(res.getId());
        assertEquals(1, events.size());
        assertEquals(ApplicationEventType.APPLICATION_CREATED, events.get(0).getEventType());
        assertNull(events.get(0).getFromStatus());
        assertEquals(ApplicationStatus.DOCUMENTS_PENDING, events.get(0).getToStatus());
        assertEquals("Application record created.", events.get(0).getMessage());
    }

    @Test
    @WithMockUser(username = "citizen1", roles = "USER")
    public void testDocumentUploadAndReadyForSubmissionTransitions() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-TIME-01")
                .profile(CitizenEligibilityProfile.builder().age(25).build())
                .build();
        ApplicationResponse appRes = applicationService.createApplication(req, "citizen1");

        // Upload file
        MockMultipartFile file = new MockMultipartFile("file", "id_card.pdf", "application/pdf", "dummy data".getBytes());
        ApplicationDocumentResponse docRes = applicationService.uploadDocument(appRes.getId(), "IDENTITY", file, "citizen1");

        assertNotNull(docRes);
        assertTrue(docRes.isUploaded());

        // Retrieve updated timeline events chronologically
        List<ApplicationEvent> events = applicationEventRepository.findAllByApplicationIdOrderByCreatedAtAsc(appRes.getId());
        
        // Should have key lifecycle events: APPLICATION_CREATED, DOCUMENT_UPLOADED, READY_FOR_SUBMISSION
        assertTrue(events.size() >= 3);
        ApplicationEvent createdEvt = events.stream()
                .filter(e -> e.getEventType() == ApplicationEventType.APPLICATION_CREATED)
                .findFirst().orElseThrow();
        assertNotNull(createdEvt);
        
        ApplicationEvent uploadedEvt = events.stream()
                .filter(e -> e.getEventType() == ApplicationEventType.DOCUMENT_UPLOADED)
                .findFirst().orElseThrow();
        assertEquals("IDENTITY", uploadedEvt.getMetadata().get("documentCode"));
        assertEquals("id_card.pdf", uploadedEvt.getMetadata().get("fileName"));
        assertFalse(uploadedEvt.getMetadata().containsKey("storageReference")); // Verify storage ref is not leaked

        ApplicationEvent readyEvt = events.stream()
                .filter(e -> e.getEventType() == ApplicationEventType.READY_FOR_SUBMISSION)
                .findFirst().orElseThrow();
        assertEquals(ApplicationStatus.DOCUMENTS_PENDING, readyEvt.getFromStatus());
        assertEquals(ApplicationStatus.READY_FOR_SUBMISSION, readyEvt.getToStatus());
    }

    @Test
    @WithMockUser(username = "citizen1", roles = "USER")
    public void testApplicationSubmissionTimeline() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-TIME-01")
                .profile(CitizenEligibilityProfile.builder().age(25).build())
                .build();
        ApplicationResponse appRes = applicationService.createApplication(req, "citizen1");

        MockMultipartFile file = new MockMultipartFile("file", "id_card.pdf", "application/pdf", "dummy data".getBytes());
        applicationService.uploadDocument(appRes.getId(), "IDENTITY", file, "citizen1");

        // Submit application
        ApplicationResponse submittedRes = applicationService.submitApplication(appRes.getId(), "citizen1");
        assertEquals(ApplicationStatus.SUBMITTED.name(), submittedRes.getStatus());

        List<ApplicationEvent> events = applicationEventRepository.findAllByApplicationIdOrderByCreatedAtAsc(appRes.getId());
        
        // Events must contain APPLICATION_SUBMITTED
        assertTrue(events.size() >= 4);
        ApplicationEvent submittedEvt = events.stream()
                .filter(e -> e.getEventType() == ApplicationEventType.APPLICATION_SUBMITTED)
                .findFirst().orElseThrow();
        assertEquals(ApplicationEventType.APPLICATION_SUBMITTED, submittedEvt.getEventType());
        assertEquals(ApplicationStatus.READY_FOR_SUBMISSION, submittedEvt.getFromStatus());
        assertEquals(ApplicationStatus.SUBMITTED, submittedEvt.getToStatus());

        // Repeated submission check
        assertThrows(IllegalStateException.class, () ->
                applicationService.submitApplication(appRes.getId(), "citizen1")
        );

        // Verify duplicate submission did not add new event
        List<ApplicationEvent> postDupEvents = applicationEventRepository.findAllByApplicationIdOrderByCreatedAtAsc(appRes.getId());
        assertEquals(events.size(), postDupEvents.size());
    }

    @Test
    @WithMockUser(username = "citizen1", roles = "USER")
    public void testCancellationTransitionsAndEnforcement() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-TIME-01")
                .profile(CitizenEligibilityProfile.builder().age(25).build())
                .build();
        ApplicationResponse appRes = applicationService.createApplication(req, "citizen1");

        // Cancel application
        ApplicationResponse cancelledRes = applicationService.cancelApplication(appRes.getId(), "citizen1");
        assertEquals(ApplicationStatus.CANCELLED.name(), cancelledRes.getStatus());

        List<ApplicationEvent> events = applicationEventRepository.findAllByApplicationIdOrderByCreatedAtAsc(appRes.getId());
        assertEquals(2, events.size());
        assertEquals(ApplicationEventType.CANCELLED, events.get(1).getEventType());

        // Cannot transition from terminal CANCELLED
        assertThrows(IllegalStateException.class, () ->
                applicationService.cancelApplication(appRes.getId(), "citizen1")
        );
        assertThrows(IllegalStateException.class, () ->
                applicationService.submitApplication(appRes.getId(), "citizen1")
        );
    }

    @Test
    @WithMockUser(username = "citizen1", roles = "USER")
    public void testSecurityAndOwnershipVerification() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-TIME-01")
                .profile(CitizenEligibilityProfile.builder().age(25).build())
                .build();
        ApplicationResponse appRes = applicationService.createApplication(req, "citizen1");

        // citizen2 tries to cancel citizen1's application -> SecurityException
        assertThrows(SecurityException.class, () ->
                applicationService.cancelApplication(appRes.getId(), "citizen2")
        );

        // citizen2 tries to view citizen1's timeline -> SecurityException
        assertThrows(SecurityException.class, () ->
                applicationService.getTimeline(appRes.getId(), "citizen2")
        );

        // citizen1 can view own timeline
        ApplicationTimelineResponse timeline = applicationService.getTimeline(appRes.getId(), "citizen1");
        assertEquals(appRes.getId(), timeline.getApplicationId());
        assertEquals(1, timeline.getEvents().size());
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testAdminPrivilegeTimelineAccess() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-TIME-01")
                .profile(CitizenEligibilityProfile.builder().age(25).build())
                .build();
        
        // citizen1 creates
        ApplicationResponse appRes;
        appRes = applicationService.createApplication(req, "citizen1");

        // admin1 queries timeline successfully
        ApplicationTimelineResponse timeline = applicationService.getTimeline(appRes.getId(), "admin1");
        assertEquals(appRes.getId(), timeline.getApplicationId());
        assertEquals(1, timeline.getEvents().size());
    }

    @Test
    @WithMockUser(username = "citizen1", roles = "USER")
    public void testFailedValidationDoesNotCreateEvents() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("SCH-TIME-01")
                .profile(CitizenEligibilityProfile.builder().age(25).build())
                .build();
        ApplicationResponse appRes = applicationService.createApplication(req, "citizen1");

        // Uploading an invalid file extension (TXT instead of PDF) should fail validation
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "dummy text".getBytes());
        assertThrows(IllegalArgumentException.class, () ->
                applicationService.uploadDocument(appRes.getId(), "IDENTITY", file, "citizen1")
        );

        // Timeline should only contain the initial APPLICATION_CREATED event
        List<ApplicationEvent> events = applicationEventRepository.findAllByApplicationIdOrderByCreatedAtAsc(appRes.getId());
        assertEquals(1, events.size());
    }

    @AfterEach
    public void tearDown() {
        schemeRepository.findBySchemeCode("SCH-TIME-01").ifPresent(schemeRepository::delete);
        applicationRepository.deleteAll();
    }
}
