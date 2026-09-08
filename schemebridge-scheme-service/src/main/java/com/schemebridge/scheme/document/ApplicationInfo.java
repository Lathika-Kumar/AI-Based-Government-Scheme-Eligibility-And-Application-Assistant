package com.schemebridge.scheme.document;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationInfo {
    private String applicationMode; // ONLINE, OFFLINE, BOTH
    private String applicationUrl;
    
    private Instant applicationStartDate;
    private Instant applicationEndDate;
    private Instant deadline;
    
    private MultilingualText instructions;
}
