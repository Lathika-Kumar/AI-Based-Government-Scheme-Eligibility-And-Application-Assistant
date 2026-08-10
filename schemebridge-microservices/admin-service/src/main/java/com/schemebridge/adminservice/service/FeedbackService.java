package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.FeedbackResolveRequest;
import com.schemebridge.adminservice.dto.FeedbackResponse;
import com.schemebridge.adminservice.enums.FeedbackStatus;
import org.springframework.data.domain.Page;

public interface FeedbackService {

    FeedbackResponse resolveFeedback(String actorEmail, String feedbackId, FeedbackResolveRequest request);

    FeedbackResponse getFeedbackById(String feedbackId);

    Page<FeedbackResponse> getAllFeedback(int page, int size);

    Page<FeedbackResponse> getFeedbackByStatus(FeedbackStatus status, int page, int size);

    Page<FeedbackResponse> searchFeedback(String query, int page, int size);

    void deleteFeedback(String actorEmail, String feedbackId);
}
