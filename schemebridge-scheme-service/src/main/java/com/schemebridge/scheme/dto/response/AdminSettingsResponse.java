package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsResponse {
    private String id;
    private boolean emailNotificationsEnabled;
    private int applicationSlaDays;
    private long maxUploadSizeBytes;
    private List<String> allowedMimeTypes;
    private boolean enableSseAlerts;
    private boolean maintenanceMode;
    private String updatedBy;
    private Instant updatedAt;
}
