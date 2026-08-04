package com.schemebridge.entity;

import com.schemebridge.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application extends BaseEntity {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String schemeId;

    private String schemeName;
    private String ministry;
    private String referenceNumber;
    private String remarks;

    @Builder.Default
    private List<String> documents = new ArrayList<>();

    @Builder.Default
    private List<ApplicationTimelineEntry> timeline = new ArrayList<>();

    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    private Instant submittedAt;
    private Instant withdrawnAt;
}
