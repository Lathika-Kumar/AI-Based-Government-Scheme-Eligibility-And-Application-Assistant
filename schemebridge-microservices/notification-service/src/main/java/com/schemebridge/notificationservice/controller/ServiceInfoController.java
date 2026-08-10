package com.schemebridge.notificationservice.controller;

import com.schemebridge.common.dto.ApiResponse;
import com.schemebridge.notificationservice.dto.ServiceInfoResponse;
import com.schemebridge.notificationservice.provider.EmailProvider;
import com.schemebridge.notificationservice.provider.PushProvider;
import com.schemebridge.notificationservice.provider.SmsProvider;
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
@Tag(name = "Service Information", description = "Public health, metadata, and provider readiness endpoints")
public class ServiceInfoController {

    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;
    private final PushProvider pushProvider;

    @GetMapping
    @Operation(summary = "Get Notification Service Info", description = "Returns active microservice status, database info, and provider readiness metrics")
    public ResponseEntity<ApiResponse<ServiceInfoResponse>> getServiceInfo() {
        ServiceInfoResponse info = ServiceInfoResponse.builder()
                .serviceName("notification-service")
                .version("1.0.0-RELEASE")
                .port("8086")
                .status("UP")
                .database("MongoDB (schemebridge_notification_db)")
                .providerStatus(Map.of(
                        emailProvider.getProviderName(), emailProvider.isAvailable(),
                        smsProvider.getProviderName(), smsProvider.isAvailable(),
                        pushProvider.getProviderName(), pushProvider.isAvailable()
                ))
                .activeSchedulerTasks(2L)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Service info retrieved successfully", info));
    }
}
