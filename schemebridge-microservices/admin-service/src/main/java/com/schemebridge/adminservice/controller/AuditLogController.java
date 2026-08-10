package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.AdminActivityLogResponse;
import com.schemebridge.adminservice.enums.AdminActionType;
import com.schemebridge.adminservice.service.AuditLogService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "System Audit Logs", description = "Administrative Activity Tracking, System Governance, and Audit Trail APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get Audit Logs (Paginated)", description = "Retrieves paginated admin activity logs with optional filtering by actor or action type")
    public ResponseEntity<ApiResponse<Page<AdminActivityLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AdminActionType action,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_SIZE) int size) {
        Page<AdminActivityLogResponse> result;
        if (actor != null) {
            result = auditLogService.getLogsByActor(actor, page, size);
        } else if (action != null) {
            result = auditLogService.getLogsByAction(action, page, size);
        } else {
            result = auditLogService.getAllLogs(page, size);
        }
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", result));
    }
}
