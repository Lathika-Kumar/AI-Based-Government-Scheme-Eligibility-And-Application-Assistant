package com.schemebridge.coreservice.application.controller;

import com.schemebridge.coreservice.application.dto.ServiceInfoResponse;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/application/service")
@Tag(name = "Application Service Info", description = "Application Domain metadata endpoint")
public class ApplicationServiceInfoController {

    @GetMapping("/info")
    @Operation(summary = "Service Info", description = "Returns application-service metadata: port, database, version, environment")
    public ResponseEntity<ApiResponse<ServiceInfoResponse>> getServiceInfo() {
        ServiceInfoResponse info = ServiceInfoResponse.builder()
                .service("application-service")
                .version("0.0.1-SNAPSHOT")
                .port(8084)
                .database("MongoDB (schemebridge_application_db)")
                .status("UP")
                .buildTime(Instant.now())
                .environment("development")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Application Service is running", info));
    }
}
