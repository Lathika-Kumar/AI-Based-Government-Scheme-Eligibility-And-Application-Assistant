package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Feedback;
import com.schemebridge.scheme.dto.request.CreateFeedbackRequest;
import com.schemebridge.scheme.dto.response.FeedbackResponse;
import com.schemebridge.scheme.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final com.schemebridge.scheme.repository.CitizenProfileRepository citizenProfileRepository;
    private final NotificationService notificationService;

    @Transactional
    public FeedbackResponse createFeedback(CreateFeedbackRequest request, String userId) {
        String feedbackNumber = "FB-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));

        String resolvedName = request.getCitizenName();
        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = citizenProfileRepository.findByUserId(userId)
                    .map(com.schemebridge.scheme.document.CitizenProfile::getDisplayName)
                    .filter(n -> n != null && !n.isBlank())
                    .orElse("Citizen");
        }

        Integer rating = request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Feedback rating must be an integer between 1 and 5.");
        }

        Feedback feedback = Feedback.builder()
                .feedbackNumber(feedbackNumber)
                .userId(userId)
                .citizenEmail(request.getCitizenEmail())
                .citizenName(resolvedName)
                .type(request.getType())
                .rating(rating)
                .comment(request.getComment())
                .relatedScheme(request.getRelatedScheme())
                .status("RECEIVED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Persisted portal feedback reference={}, userId={}, citizenName={}, type={}", saved.getFeedbackNumber(), userId, saved.getCitizenName(), saved.getType());

        // Notify Admins of new citizen feedback
        try {
            notificationService.sendNotification(
                    null,
                    "ROLE_ADMIN",
                    com.schemebridge.scheme.document.NotificationType.FEEDBACK_SUBMITTED,
                    "New Citizen Feedback Submitted",
                    String.format("Citizen %s submitted a %s with a %d-star rating.",
                            resolvedName, saved.getType() != null ? saved.getType() : "General feedback", saved.getRating()),
                    "IN_APP",
                    "FEEDBACK",
                    saved.getId(),
                    userId,
                    java.util.Map.of(
                            "feedbackNumber", saved.getFeedbackNumber(),
                            "feedbackType", saved.getType() != null ? saved.getType() : "General",
                            "rating", saved.getRating()
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch admin notification for feedback {}: {}", saved.getFeedbackNumber(), e.getMessage());
        }

        return FeedbackResponse.fromEntity(saved);
    }

    public List<FeedbackResponse> getMyFeedback(String userId) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FeedbackResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<FeedbackResponse> getAdminFeedback(Pageable pageable) {
        return feedbackRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(FeedbackResponse::fromEntity);
    }

    @Transactional
    public FeedbackResponse updateStatus(String id, String status) {
        Feedback feedback = feedbackRepository.findById(id)
                .or(() -> feedbackRepository.findByFeedbackNumber(id))
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found: " + id));

        feedback.setStatus(status.toUpperCase());
        feedback.setUpdatedAt(Instant.now());
        Feedback updated = feedbackRepository.save(feedback);
        log.info("Updated feedback status id={}, status={}", id, status);
        return FeedbackResponse.fromEntity(updated);
    }
}
