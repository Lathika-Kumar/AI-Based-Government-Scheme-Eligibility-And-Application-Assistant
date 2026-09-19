package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Feedback;
import com.schemebridge.scheme.dto.request.CreateFeedbackRequest;
import com.schemebridge.scheme.dto.response.FeedbackResponse;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void testCreateFeedback_WithValidRating1_PersistsExactRating1() {
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));

        CreateFeedbackRequest req = CreateFeedbackRequest.builder()
                .type("Bug Report")
                .rating(1)
                .comment("Issue occurred during document upload.")
                .citizenName("Citizen One")
                .citizenEmail("citizen1@test.in")
                .build();

        FeedbackResponse res = feedbackService.createFeedback(req, "user-1");
        assertNotNull(res);
        assertEquals(1, res.getRating());
        assertEquals("Bug Report", res.getType());
        assertEquals("Issue occurred during document upload.", res.getComment());
    }

    @Test
    void testCreateFeedback_WithValidRating5_PersistsExactRating5() {
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));

        CreateFeedbackRequest req = CreateFeedbackRequest.builder()
                .type("Portal Rating")
                .rating(5)
                .comment("Outstanding service.")
                .citizenName("Citizen Two")
                .citizenEmail("citizen2@test.in")
                .build();

        FeedbackResponse res = feedbackService.createFeedback(req, "user-2");
        assertNotNull(res);
        assertEquals(5, res.getRating());
    }

    @Test
    void testCreateFeedback_WithMissingRating_ThrowsIllegalArgumentException() {
        CreateFeedbackRequest req = CreateFeedbackRequest.builder()
                .type("General Feedback")
                .rating(null)
                .comment("No rating provided.")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                feedbackService.createFeedback(req, "user-3"));
        assertTrue(ex.getMessage().contains("Feedback rating must be an integer between 1 and 5"));
    }

    @Test
    void testCreateFeedback_WithInvalidRatingZero_ThrowsIllegalArgumentException() {
        CreateFeedbackRequest req = CreateFeedbackRequest.builder()
                .type("General Feedback")
                .rating(0)
                .comment("Rating is 0.")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                feedbackService.createFeedback(req, "user-4"));
        assertTrue(ex.getMessage().contains("Feedback rating must be an integer between 1 and 5"));
    }

    @Test
    void testCreateFeedback_WithInvalidRatingOver5_ThrowsIllegalArgumentException() {
        CreateFeedbackRequest req = CreateFeedbackRequest.builder()
                .type("General Feedback")
                .rating(6)
                .comment("Rating is 6.")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                feedbackService.createFeedback(req, "user-5"));
        assertTrue(ex.getMessage().contains("Feedback rating must be an integer between 1 and 5"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void testCreateFeedback_DispatchesAdminNotificationOnSuccess() {
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));

        CreateFeedbackRequest req = CreateFeedbackRequest.builder()
                .type("Bug Report")
                .rating(2)
                .comment("Button click did not respond.")
                .citizenName("Aarav Sharma")
                .citizenEmail("aarav@test.in")
                .build();

        FeedbackResponse res = feedbackService.createFeedback(req, "user-82");
        assertNotNull(res);

        verify(notificationService, times(1)).sendNotification(
                isNull(),
                eq("ROLE_ADMIN"),
                eq(com.schemebridge.scheme.document.NotificationType.FEEDBACK_SUBMITTED),
                eq("New Citizen Feedback Submitted"),
                contains("Aarav Sharma"),
                eq("IN_APP"),
                eq("FEEDBACK"),
                any(),
                eq("user-82"),
                any()
        );
    }
}
