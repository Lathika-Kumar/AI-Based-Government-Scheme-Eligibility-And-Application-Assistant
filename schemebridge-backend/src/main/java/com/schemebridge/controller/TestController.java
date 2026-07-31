package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> checkHealth() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "application", "schemebridge-backend",
                "framework", "Spring Boot 3.3.1",
                "mongodb", "Configured",
                "security", "Spring Security 6 + JWT",
                "swagger", "Enabled at /swagger-ui.html"
        );
        return ApiResponse.ok("Backend foundation is running cleanly", status);
    }
}
