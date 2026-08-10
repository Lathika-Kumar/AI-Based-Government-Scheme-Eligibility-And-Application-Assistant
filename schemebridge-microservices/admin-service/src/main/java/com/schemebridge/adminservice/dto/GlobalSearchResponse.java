package com.schemebridge.adminservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSearchResponse {

    private String query;
    private List<OfficerResponse> officers;
    private List<AnnouncementResponse> announcements;
    private List<FeedbackResponse> feedback;
    private List<AdminActivityLogResponse> activityLogs;
}
