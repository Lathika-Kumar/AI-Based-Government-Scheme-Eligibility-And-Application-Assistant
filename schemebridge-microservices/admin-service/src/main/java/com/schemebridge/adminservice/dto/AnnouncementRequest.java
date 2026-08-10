package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.AnnouncementAudience;
import com.schemebridge.adminservice.enums.AnnouncementPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @Builder.Default
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    @NotNull(message = "Target audience is required")
    private AnnouncementAudience targetAudience;

    private Instant scheduledAt;
    private Instant expiresAt;
}
