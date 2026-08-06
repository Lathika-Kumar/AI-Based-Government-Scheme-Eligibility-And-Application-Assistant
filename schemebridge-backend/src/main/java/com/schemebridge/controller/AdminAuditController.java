package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.entity.AdminAuditLog;
import com.schemebridge.repository.AdminAuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHEME_MANAGER', 'VERIFICATION_OFFICER')")
@Tag(name = "Admin Audit History", description = "Audit trail endpoints for administrative review and accountability")
public class AdminAuditController {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @GetMapping("/audit-logs/{targetId}")
    @Operation(summary = "Get audit history", description = "Returns audit events associated with the requested target record")
    public ResponseEntity<ApiResponse<List<AdminAuditLog>>> getAuditLogs(@PathVariable String targetId) {
        List<AdminAuditLog> logs = adminAuditLogRepository.findByTargetIdOrderByCreatedAtDesc(targetId);
        return ResponseEntity.ok(ApiResponse.success("Audit history retrieved successfully", logs));
    }
}
