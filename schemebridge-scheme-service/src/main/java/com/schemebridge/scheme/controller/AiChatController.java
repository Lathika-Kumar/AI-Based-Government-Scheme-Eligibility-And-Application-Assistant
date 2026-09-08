package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.AiChatRequest;
import com.schemebridge.scheme.dto.response.AiChatResponse;
import com.schemebridge.scheme.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Conversational AI", description = "Endpoints for Citizen and Admin Conversational AI Assistants")
@SecurityRequirement(name = "BearerAuth")
public class AiChatController {

    private final AiChatService aiChatService;

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

    @PostMapping({"/api/ai/citizen/chat", "/api/ai/chat", "/api/v1/ai/chat"})
    @Operation(summary = "Send chat message to Citizen Conversational AI Assistant")
    public ResponseEntity<AiChatResponse> chatCitizen(@Valid @RequestBody AiChatRequest request) {
        String userId = getUserId();
        AiChatResponse response = aiChatService.chatCitizen(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/ai/admin/chat")
    @Operation(summary = "Send chat message to Admin Intelligence Conversational Assistant")
    public ResponseEntity<AiChatResponse> chatAdmin(@Valid @RequestBody AiChatRequest request) {
        String adminId = getUserId();
        String role = getRole();
        AiChatResponse response = aiChatService.chatAdmin(request, adminId, role);
        return ResponseEntity.ok(response);
    }
}
