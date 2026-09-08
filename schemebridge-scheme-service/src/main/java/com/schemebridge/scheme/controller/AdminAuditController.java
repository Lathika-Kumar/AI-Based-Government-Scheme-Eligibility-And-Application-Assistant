package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.PagedAdminAuditLogResponse;
import com.schemebridge.scheme.service.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping({"/api/admin/audit", "/api/admin/audit-logs"})
@RequiredArgsConstructor
@Tag(name = "Admin Audit Logs", description = "Endpoints for viewing immutable system and administrative audit logs")
@SecurityRequirement(name = "BearerAuth")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    @Operation(summary = "Get paginated audit logs with actor, action, and entity filters (Admin only)")
    public ResponseEntity<PagedAdminAuditLogResponse> getAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PagedAdminAuditLogResponse response = adminAuditService.getAuditLogs(actor, action, entity, startDate, endDate, page, size, sort, direction);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
