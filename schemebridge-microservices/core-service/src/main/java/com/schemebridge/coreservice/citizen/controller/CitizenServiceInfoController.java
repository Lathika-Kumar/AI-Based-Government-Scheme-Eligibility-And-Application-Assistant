package com.schemebridge.coreservice.citizen.controller;

import com.schemebridge.coreservice.citizen.dto.ServiceInfoResponse;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/citizen/service")
@Tag(name = "Citizen Service Info", description = "Metadata & System Health Info for Citizen Domain")
public class CitizenServiceInfoController {

    @GetMapping("/info")
    @Operation(summary = "Get Microservice Info", description = "Returns service name, version, port, database engine, status, and build metadata")
    public ResponseEntity<ApiResponse<ServiceInfoResponse>> getServiceInfo() {
        ServiceInfoResponse response = ServiceInfoResponse.builder()
                .service("citizen-service")
                .version("0.0.1-SNAPSHOT")
                .port(8082)
                .database("MongoDB")
                .status("UP")
                .buildTime(Instant.now().toString())
                .environment("development")
                .build();

        return ResponseEntity.ok(ApiResponse.success("Citizen Service info retrieved successfully", response));
    }
}
