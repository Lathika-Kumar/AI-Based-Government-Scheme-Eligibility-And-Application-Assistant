package com.schemebridge.coreservice.scheme.controller;

import com.schemebridge.coreservice.scheme.dto.ServiceInfoResponse;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/scheme/service")
@Tag(name = "Scheme Service Info", description = "Scheme Domain metadata endpoint")
public class SchemeServiceInfoController {

    @GetMapping("/info")
    @Operation(summary = "Get Scheme Service Info", description = "Returns scheme-service microservice metadata, version, port, and Oracle DB connection status")
    public ResponseEntity<ApiResponse<ServiceInfoResponse>> getServiceInfo() {
        ServiceInfoResponse info = ServiceInfoResponse.builder()
                .service("scheme-service")
                .version("0.0.1-SNAPSHOT")
                .port(8083)
                .database("Oracle XE (XEPDB1)")
                .status("UP")
                .buildTime(Instant.now())
                .environment("development")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Scheme Service info retrieved successfully", info));
    }
}
