package com.schemebridge.service;

import com.schemebridge.dto.ApplicationRequest;
import com.schemebridge.dto.ApplicationResponse;
import com.schemebridge.dto.ApplicationTimelineEntryResponse;

import java.util.List;

public interface ApplicationService {

    List<ApplicationResponse> getApplications(String userEmail);

    ApplicationResponse getApplicationById(String id, String userEmail);

    ApplicationResponse submitApplication(String userEmail, ApplicationRequest request);

    List<ApplicationTimelineEntryResponse> getApplicationTimeline(String id, String userEmail);

    void withdrawApplication(String id, String userEmail);

    List<String> getSavedSchemes(String userEmail);

    void saveScheme(String userEmail, String schemeId);

    void unsaveScheme(String userEmail, String schemeId);
}
