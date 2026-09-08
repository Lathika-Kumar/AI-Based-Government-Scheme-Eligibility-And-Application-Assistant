package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "admin_settings")
public class AdminSettings {

    @Id
    @Builder.Default
    private String id = "DEFAULT_SETTINGS";

    @Builder.Default
    private boolean emailNotificationsEnabled = true;

    @Builder.Default
    private int applicationSlaDays = 7;

    @Builder.Default
    private long maxUploadSizeBytes = 5 * 1024 * 1024; // 5MB

    @Builder.Default
    private List<String> allowedMimeTypes = List.of("application/pdf", "image/jpeg", "image/png");

    @Builder.Default
    private boolean enableSseAlerts = true;

    @Builder.Default
    private boolean maintenanceMode = false;

    private String updatedBy;

    @LastModifiedDate
    private Instant updatedAt;
}
