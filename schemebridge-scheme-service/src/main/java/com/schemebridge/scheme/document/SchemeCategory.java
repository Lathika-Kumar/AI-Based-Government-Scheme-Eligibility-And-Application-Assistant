package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "scheme_categories")
public class SchemeCategory {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String code;
    
    private String name;
    private String description;
    
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
