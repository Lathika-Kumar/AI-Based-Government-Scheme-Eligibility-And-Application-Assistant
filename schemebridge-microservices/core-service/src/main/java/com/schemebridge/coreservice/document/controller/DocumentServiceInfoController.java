package com.schemebridge.coreservice.document.controller;

import com.schemebridge.coreservice.document.dto.ServiceInfoResponse;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/document/service")
@Tag(name = "Document Service Info", description = "Document Domain metadata endpoint")
public class DocumentServiceInfoController {

    @GetMapping("/info")
    @Operation(summary = "Service Info", description = "Returns document-service metadata: port, database, storage provider, status")
    public ResponseEntity<ApiResponse<ServiceInfoResponse>> getServiceInfo() {
        ServiceInfoResponse info = ServiceInfoResponse.builder()
                .service("document-service")
                .version("0.0.1-SNAPSHOT")
                .port(8085)
                .database("MongoDB (schemebridge_document_db)")
                .status("UP")
                .buildTime(Instant.now())
                .environment("development")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Document Service is running", info));
    }
}
