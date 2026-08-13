package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedScheme {
    private String schemeId;
    private LocalDateTime savedAt;
    private Boolean favorite;
    private String source; // DIRECT_SEARCH, AI_RECOMMENDATION, CATEGORY_BROWSE
}
