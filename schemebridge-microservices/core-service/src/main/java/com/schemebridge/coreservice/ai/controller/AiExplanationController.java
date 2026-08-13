package com.schemebridge.coreservice.ai.controller;

import com.schemebridge.common.dto.ApiResponse;
import com.schemebridge.coreservice.ai.dto.AiExplanationRequest;
import com.schemebridge.coreservice.ai.dto.AiExplanationResponse;
import com.schemebridge.coreservice.ai.service.AiExplanationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;



import com.schemebridge.coreservice.ai.dto.AiChatRequest;
import com.schemebridge.coreservice.ai.dto.AiChatResponse;
import com.schemebridge.coreservice.ai.service.AiChatService;

import com.schemebridge.coreservice.ai.dto.AiDocumentSuggestionsRequest;
import com.schemebridge.coreservice.ai.dto.AiDocumentSuggestionsResponse;
import com.schemebridge.coreservice.ai.service.AiDocumentSuggestionsService;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistance & Explanations", description = "Backend-controlled AI explanation APIs powered by Google Gemini with deterministic rule engine fallbacks")
public class AiExplanationController {

    private final AiExplanationService aiExplanationService;
    private final AiChatService aiChatService;
    private final AiDocumentSuggestionsService aiDocumentSuggestionsService;

    @PostMapping("/explanation")
    @Operation(summary = "Get AI Scheme Explanation",
               description = "Generates a structured, plain-language AI explanation of scheme eligibility for the authenticated citizen in English or Tamil.")
    public ResponseEntity<ApiResponse<AiExplanationResponse>> getExplanation(
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @Valid @RequestBody AiExplanationRequest request) {

        if (authUserId == null || authUserId.trim().isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Missing user authentication context", List.of("UNAUTHORIZED")));
        }

        AiExplanationResponse response = aiExplanationService.generateExplanation(authUserId, request);
        return ResponseEntity.ok(ApiResponse.success("AI explanation generated successfully", response));
    }

    @PostMapping("/chat")
    @Operation(summary = "Interactive Scheme AI Chatbot",
               description = "Multi-turn, context-aware interactive chatbot for scheme queries, document requirements, and eligibility guidance.")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @Valid @RequestBody AiChatRequest request) {

        if (authUserId == null || authUserId.trim().isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Missing user authentication context", List.of("UNAUTHORIZED")));
        }

        AiChatResponse response = aiChatService.processChat(authUserId, request);
        return ResponseEntity.ok(ApiResponse.success("AI chat response generated successfully", response));
    }

    @PostMapping("/document-suggestions")
    @Operation(summary = "AI Document Vault Suggestions",
               description = "Provides deterministic document matching and AI explanation of available, missing, and pending documents for a scheme.")
    public ResponseEntity<ApiResponse<AiDocumentSuggestionsResponse>> getDocumentSuggestions(
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @Valid @RequestBody AiDocumentSuggestionsRequest request) {

        if (authUserId == null || authUserId.trim().isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Missing user authentication context", List.of("UNAUTHORIZED")));
        }

        AiDocumentSuggestionsResponse response = aiDocumentSuggestionsService.generateSuggestions(authUserId, request);
        return ResponseEntity.ok(ApiResponse.success("AI document suggestions generated successfully", response));
    }
}


