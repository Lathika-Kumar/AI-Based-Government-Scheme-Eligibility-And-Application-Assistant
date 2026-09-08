package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.AssignGrievanceRequest;
import com.schemebridge.scheme.dto.request.CreateGrievanceRequest;
import com.schemebridge.scheme.dto.request.GrievanceReplyRequest;
import com.schemebridge.scheme.dto.request.ResolveGrievanceRequest;
import com.schemebridge.scheme.dto.response.GrievanceResponse;
import com.schemebridge.scheme.dto.response.PagedGrievanceResponse;
import com.schemebridge.scheme.service.GrievanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Grievance Management", description = "Endpoints for lodging, replying, and resolving citizen grievances")
@SecurityRequirement(name = "BearerAuth")
public class GrievanceController {

    private final GrievanceService grievanceService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SCHEME_MANAGER") || r.equals("ROLE_VERIFICATION_OFFICER"))
                    .findFirst()
                    .orElse("ROLE_USER");
        }
        return "ROLE_USER";
    }

    private boolean isPrivileged() {
        String role = getRole();
        return "ROLE_ADMIN".equals(role) || "ROLE_SCHEME_MANAGER".equals(role) || "ROLE_VERIFICATION_OFFICER".equals(role);
    }

    // ================= Citizen Endpoints =================

    @PostMapping("/api/grievances")
    @Operation(summary = "Lodge a new citizen grievance")
    public ResponseEntity<GrievanceResponse> createGrievance(@Valid @RequestBody CreateGrievanceRequest request) {
        String userId = getUserId();
        GrievanceResponse response = grievanceService.createGrievance(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping({"/api/grievances", "/api/grievances/my"})
    @Operation(summary = "Get all grievances lodged by the current citizen")
    public ResponseEntity<List<GrievanceResponse>> getMyGrievances() {
        String userId = getUserId();
        List<GrievanceResponse> response = grievanceService.getMyGrievances(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/grievances/{id}")
    @Operation(summary = "Get detailed information for a single grievance")
    public ResponseEntity<GrievanceResponse> getGrievanceById(@PathVariable String id) {
        String userId = getUserId();
        GrievanceResponse response = grievanceService.getGrievanceById(id, userId, isPrivileged());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/grievances/{id}/reply")
    @Operation(summary = "Add a citizen reply to an existing grievance")
    public ResponseEntity<GrievanceResponse> replyToGrievance(
            @PathVariable String id,
            @Valid @RequestBody GrievanceReplyRequest request) {
        String userId = getUserId();
        String role = getRole();
        GrievanceResponse response = grievanceService.replyToGrievance(id, request, userId, role);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ================= Administrative Endpoints =================

    @GetMapping("/api/admin/grievances")
    @Operation(summary = "Get paginated administrative list of all grievances")
    public ResponseEntity<PagedGrievanceResponse> getAdminGrievances(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PagedGrievanceResponse response = grievanceService.getAdminGrievances(status, category, priority, assignedTo, search, page, size, sort, direction);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/admin/grievances/{id}")
    @Operation(summary = "Get grievance details including internal notes (Admin only)")
    public ResponseEntity<GrievanceResponse> getAdminGrievanceById(@PathVariable String id) {
        String userId = getUserId();
        GrievanceResponse response = grievanceService.getGrievanceById(id, userId, true);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping({"/api/admin/grievances/{id}/respond", "/api/admin/grievances/{id}/reply"})
    @Operation(summary = "Add an official administrative response to a grievance (also accepts /reply path)")
    public ResponseEntity<GrievanceResponse> respondToGrievance(
            @PathVariable String id,
            @Valid @RequestBody GrievanceReplyRequest request) {
        String userId = getUserId();
        String role = getRole();
        GrievanceResponse response = grievanceService.replyToGrievance(id, request, userId, role);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/admin/grievances/{id}/assign")
    @Operation(summary = "Assign a grievance to a specific operations officer")
    public ResponseEntity<GrievanceResponse> assignGrievance(
            @PathVariable String id,
            @Valid @RequestBody AssignGrievanceRequest request) {
        String userId = getUserId();
        String role = getRole();
        GrievanceResponse response = grievanceService.assignGrievance(id, request, userId, role);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/admin/grievances/{id}/resolve")
    @Operation(summary = "Resolve a grievance with official resolution remarks")
    public ResponseEntity<GrievanceResponse> resolveGrievance(
            @PathVariable String id,
            @Valid @RequestBody ResolveGrievanceRequest request) {
        String userId = getUserId();
        String role = getRole();
        GrievanceResponse response = grievanceService.resolveGrievance(id, request, userId, role);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/admin/grievances/{id}/close")
    @Operation(summary = "Close a grievance ticket")
    public ResponseEntity<GrievanceResponse> closeGrievance(@PathVariable String id) {
        String userId = getUserId();
        String role = getRole();
        GrievanceResponse response = grievanceService.closeGrievance(id, userId, role);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
