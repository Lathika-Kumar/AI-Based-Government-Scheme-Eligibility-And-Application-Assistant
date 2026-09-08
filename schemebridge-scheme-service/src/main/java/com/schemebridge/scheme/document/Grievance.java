package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "grievances")
@CompoundIndexes({
    @CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}"),
    @CompoundIndex(name = "assigned_status_idx", def = "{'assignedTo': 1, 'status': 1}")
})
public class Grievance {

    @Id
    private String id;

    @Indexed(unique = true)
    private String grievanceNumber;

    @Indexed
    private String userId;

    @Indexed
    private String applicationId;

    @Indexed
    private String schemeCode;

    private String category;
    private String subject;
    private String description;

    @Indexed
    private GrievancePriority priority;

    @Indexed
    private GrievanceStatus status;

    @Indexed
    private String assignedTo;

    private String resolution;
    private Instant resolvedAt;

    @Builder.Default
    private List<GrievanceTimelineEntry> timeline = new ArrayList<>();

    @CreatedDate
    @Indexed
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
