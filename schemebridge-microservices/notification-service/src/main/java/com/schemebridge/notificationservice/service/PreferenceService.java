package com.schemebridge.notificationservice.service;

import com.schemebridge.notificationservice.dto.PreferenceResponse;
import com.schemebridge.notificationservice.dto.PreferenceUpdateRequest;

public interface PreferenceService {

    PreferenceResponse getPreferences(String authUserId);

    PreferenceResponse updatePreferences(String authUserId, PreferenceUpdateRequest request);

    boolean isChannelEnabled(String authUserId, String channelName, String notificationType);
}
