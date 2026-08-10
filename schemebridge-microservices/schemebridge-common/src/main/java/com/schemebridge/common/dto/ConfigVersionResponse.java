package com.schemebridge.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigVersionResponse {

    private String applicationName;
    private String activeProfiles;
    private String configVersion;
    private String configServerUrl;
    private String configSource;
    private Instant loadedTimestamp;
    private boolean configServerConnected;
}
