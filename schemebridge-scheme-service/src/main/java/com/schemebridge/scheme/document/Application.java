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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "applications")
@CompoundIndexes({
    @CompoundIndex(name = "user_scheme_idx", def = "{'userId': 1, 'schemeCode': 1}")
})
public class Application {
    @Id
    private String id;

    @Indexed(unique = true)
    private String applicationNumber;

    @Indexed
    private String userId;

    @Indexed
    private String schemeId;

    @Indexed
    private String schemeCode;

    @Indexed
    private ApplicationStatus status;

    private Instant submittedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
