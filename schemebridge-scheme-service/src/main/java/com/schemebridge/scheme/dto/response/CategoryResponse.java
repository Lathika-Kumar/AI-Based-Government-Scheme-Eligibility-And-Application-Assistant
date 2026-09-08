package com.schemebridge.scheme.dto.response;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
