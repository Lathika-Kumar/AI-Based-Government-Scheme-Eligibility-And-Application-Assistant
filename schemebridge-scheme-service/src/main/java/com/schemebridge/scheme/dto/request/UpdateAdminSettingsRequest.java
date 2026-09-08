package com.schemebridge.scheme.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminSettingsRequest {
    private Boolean emailNotificationsEnabled;
    private Integer applicationSlaDays;
    private Long maxUploadSizeBytes;
    private List<String> allowedMimeTypes;
    private Boolean enableSseAlerts;
    private Boolean maintenanceMode;
}
