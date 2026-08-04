package com.schemebridge.service;

import com.schemebridge.dto.ProfileCompletionResponse;
import com.schemebridge.dto.ProfileRequest;
import com.schemebridge.dto.ProfileResponse;
import com.schemebridge.dto.ProfileSummaryResponse;

public interface CitizenProfileService {
    ProfileResponse getProfile(String userEmail);
    ProfileResponse updateProfile(String userEmail, ProfileRequest request);
    ProfileCompletionResponse getProfileCompletion(String userEmail);
    ProfileSummaryResponse getProfileSummary(String userEmail);
}
