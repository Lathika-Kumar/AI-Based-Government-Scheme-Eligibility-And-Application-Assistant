package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.dto.ServiceInfoResponse;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/service/info")
@RequiredArgsConstructor
@Tag(name = "Service Information", description = "Microservice Metadata, Health, and Environment Diagnostic Endpoints")
public class ServiceInfoController {

    @GetMapping
    @Operation(summary = "Get Microservice Information", description = "Retrieves service status, port 8087 configuration, and Oracle Database details")
    public ResponseEntity<ApiResponse<ServiceInfoResponse>> getServiceInfo() {
        ServiceInfoResponse info = ServiceInfoResponse.builder()
                .serviceName("admin-service")
                .version("1.0.0")
                .port("8087")
                .status("UP")
                .database("Oracle XE / XEPDB1")
                .metrics(Map.of(
                        "activeConnections", 2,
                        "scheduledJobs", 1,
                        "uptime", "Active"
                ))
                .build();
        return ResponseEntity.ok(ApiResponse.success("Admin Service Info retrieved", info));
    }
}
