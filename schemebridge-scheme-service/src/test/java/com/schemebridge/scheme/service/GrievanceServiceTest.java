package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.AssignGrievanceRequest;
import com.schemebridge.scheme.dto.request.CreateGrievanceRequest;
import com.schemebridge.scheme.dto.request.GrievanceReplyRequest;
import com.schemebridge.scheme.dto.request.ResolveGrievanceRequest;
import com.schemebridge.scheme.dto.response.GrievanceResponse;
import com.schemebridge.scheme.repository.GrievanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrievanceServiceTest {

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private GrievanceService grievanceService;

    private Grievance testGrievance;

    @BeforeEach
    void setUp() {
        testGrievance = Grievance.builder()
                .id("grv-100")
                .grievanceNumber("GRV-2026-001")
                .userId("citizen-1")
                .category("DOCUMENT_ISSUE")
                .subject("Income Certificate Rejected")
                .description("Please review my updated income certificate.")
                .priority(GrievancePriority.HIGH)
                .status(GrievanceStatus.OPEN)
                .timeline(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testCreateGrievance_Success() {
        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(i -> {
            Grievance g = i.getArgument(0);
            g.setId("grv-new");
            return g;
        });

        CreateGrievanceRequest req = CreateGrievanceRequest.builder()
                .category("DOCUMENT_ISSUE")
                .subject("Income Certificate Rejected")
                .description("Please review my updated income certificate.")
                .priority(GrievancePriority.HIGH)
                .build();

        GrievanceResponse response = grievanceService.createGrievance(req, "citizen-1");
        assertNotNull(response);
        assertEquals("citizen-1", response.getUserId());
        assertEquals(GrievanceStatus.OPEN, response.getStatus());
        verify(notificationService).sendNotification(any(), eq("ROLE_ADMIN"), eq(NotificationType.GRIEVANCE_CREATED), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testAssignGrievance_Success() {
        when(grievanceRepository.findById("grv-100")).thenReturn(Optional.of(testGrievance));
        when(grievanceRepository.save(any(Grievance.class))).thenReturn(testGrievance);

        AssignGrievanceRequest req = AssignGrievanceRequest.builder().assignedTo("officer-42").build();
        GrievanceResponse response = grievanceService.assignGrievance("grv-100", req, "admin-1", "ROLE_ADMIN");

        assertNotNull(response);
        assertEquals("officer-42", testGrievance.getAssignedTo());
        assertEquals(GrievanceStatus.IN_PROGRESS, testGrievance.getStatus());
        verify(adminAuditService).recordAction(eq("admin-1"), eq("ROLE_ADMIN"), eq("GRIEVANCE_ASSIGNED"), eq("GRIEVANCE"), eq("grv-100"), any(), any(), any(), any(), any());
    }

    @Test
    void testResolveGrievance_Success() {
        when(grievanceRepository.findById("grv-100")).thenReturn(Optional.of(testGrievance));
        when(grievanceRepository.save(any(Grievance.class))).thenReturn(testGrievance);

        ResolveGrievanceRequest req = ResolveGrievanceRequest.builder().resolution("Document re-verified and approved.").build();
        GrievanceResponse response = grievanceService.resolveGrievance("grv-100", req, "admin-1", "ROLE_ADMIN");

        assertNotNull(response);
        assertEquals(GrievanceStatus.RESOLVED, testGrievance.getStatus());
        assertEquals("Document re-verified and approved.", testGrievance.getResolution());
        verify(notificationService).sendNotification(eq("citizen-1"), any(), eq(NotificationType.GRIEVANCE_UPDATED), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testOfficerReply_ResolvesGrievanceAndNotifiesCitizen() {
        when(grievanceRepository.findById("grv-100")).thenReturn(Optional.of(testGrievance));
        when(grievanceRepository.save(any(Grievance.class))).thenReturn(testGrievance);

        GrievanceReplyRequest req = GrievanceReplyRequest.builder()
                .message("We have approved your revised certificate.")
                .internalOnly(false)
                .build();

        GrievanceResponse response = grievanceService.replyToGrievance("grv-100", req, "officer-1", "ROLE_VERIFICATION_OFFICER");

        assertNotNull(response);
        assertEquals(GrievanceStatus.RESOLVED, testGrievance.getStatus());
        assertEquals("We have approved your revised certificate.", testGrievance.getResolution());
        assertNotNull(testGrievance.getResolvedAt());
        assertTrue(testGrievance.getTimeline().stream().anyMatch(t -> "RESOLVED".equals(t.getAction())));
        verify(notificationService).sendNotification(eq("citizen-1"), any(), eq(NotificationType.GRIEVANCE_UPDATED), any(), any(), any(), any(), any(), any(), any());
        verify(adminAuditService).recordAction(eq("officer-1"), eq("ROLE_VERIFICATION_OFFICER"), eq("GRIEVANCE_RESOLVED"), eq("GRIEVANCE"), eq("grv-100"), any(), any(), any(), any(), any());
    }

    @Test
    void testCitizenReply_ToResolvedGrievance_ThrowsIllegalStateException() {
        testGrievance.setStatus(GrievanceStatus.RESOLVED);
        when(grievanceRepository.findById("grv-100")).thenReturn(Optional.of(testGrievance));

        GrievanceReplyRequest req = GrievanceReplyRequest.builder()
                .message("Can I still ask something?")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                grievanceService.replyToGrievance("grv-100", req, "citizen-1", "ROLE_USER"));
        assertEquals("This grievance has been resolved. Further replies are closed.", ex.getMessage());
    }

    @Test
    void testCitizenReply_ToOpenGrievance_Success() {
        when(grievanceRepository.findById("grv-100")).thenReturn(Optional.of(testGrievance));
        when(grievanceRepository.save(any(Grievance.class))).thenReturn(testGrievance);

        GrievanceReplyRequest req = GrievanceReplyRequest.builder()
                .message("Uploaded the latest pay slip.")
                .build();

        GrievanceResponse response = grievanceService.replyToGrievance("grv-100", req, "citizen-1", "ROLE_USER");
        assertNotNull(response);
        assertTrue(testGrievance.getTimeline().stream().anyMatch(t -> "CITIZEN_REPLY".equals(t.getAction())));
    }

    @Test
    void testCitizenUnauthorizedAccess_ThrowsSecurityException() {
        when(grievanceRepository.findById("grv-100")).thenReturn(Optional.of(testGrievance));

        assertThrows(SecurityException.class, () ->
                grievanceService.getGrievanceById("grv-100", "other-citizen", false));
    }
}
