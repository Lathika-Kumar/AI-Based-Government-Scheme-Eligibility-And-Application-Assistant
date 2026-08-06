package com.schemebridge.entity;

import com.schemebridge.enums.GrievanceCategory;
import com.schemebridge.enums.GrievanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "grievances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grievance extends BaseEntity {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String subject;
    private String description;

    private GrievanceCategory category;

    @Builder.Default
    private GrievanceStatus status = GrievanceStatus.OPEN;

    private String resolutionNotes;
    private Instant resolvedAt;
}
