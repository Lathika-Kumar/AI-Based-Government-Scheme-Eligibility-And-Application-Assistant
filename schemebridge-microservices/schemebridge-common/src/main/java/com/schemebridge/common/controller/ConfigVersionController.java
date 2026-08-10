package com.schemebridge.common.controller;

import com.schemebridge.common.dto.ApiResponse;
import com.schemebridge.common.dto.ConfigVersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/service")
public class ConfigVersionController {

    private final Environment environment;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Value("${schemebridge.version:1.0.0-MODULE12}")
    private String configVersion;

    @Value("${spring.config.import:none}")
    private String configSource;

    public ConfigVersionController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/config-version")
    public ResponseEntity<ApiResponse<ConfigVersionResponse>> getConfigVersion() {
        String[] profiles = environment.getActiveProfiles();
        String activeProfileStr = profiles.length > 0 ? String.join(",", profiles) : "default";

        boolean isConfigServerConnected = configSource.contains("configserver");

        ConfigVersionResponse response = ConfigVersionResponse.builder()
                .applicationName(applicationName)
                .activeProfiles(activeProfileStr)
                .configVersion(configVersion)
                .configServerUrl(environment.getProperty("spring.cloud.config.uri", "http://localhost:8888"))
                .configSource(configSource)
                .loadedTimestamp(Instant.now())
                .configServerConnected(isConfigServerConnected)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Configuration version retrieved successfully", response));
    }
}
