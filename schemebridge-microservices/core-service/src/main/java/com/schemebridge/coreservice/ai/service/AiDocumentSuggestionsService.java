package com.schemebridge.coreservice.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.common.exception.ResourceNotFoundException;
import com.schemebridge.common.exception.UnauthorizedException;
import com.schemebridge.coreservice.ai.client.GeminiApiClient;
import com.schemebridge.coreservice.ai.dto.AiDocumentSuggestionsRequest;
import com.schemebridge.coreservice.ai.dto.AiDocumentSuggestionsResponse;
import com.schemebridge.coreservice.ai.dto.AiDocumentSuggestionsResponse.DocumentStatusItem;
import com.schemebridge.coreservice.document.enums.VerificationStatus;
import com.schemebridge.coreservice.document.model.DocumentMetadata;
import com.schemebridge.coreservice.document.repository.DocumentRepository;
import com.schemebridge.coreservice.scheme.entity.Scheme;
import com.schemebridge.coreservice.scheme.entity.SchemeDocument;
import com.schemebridge.coreservice.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiDocumentSuggestionsService {

    private final GeminiApiClient geminiApiClient;
    private final SchemeRepository schemeRepository;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @Value("${feature.ai.enabled:true}")
    private boolean aiEnabled;

    public AiDocumentSuggestionsResponse generateSuggestions(String authUserId, AiDocumentSuggestionsRequest request) {
        if (authUserId == null || authUserId.trim().isEmpty()) {
            throw new UnauthorizedException("Unauthorized: Missing user authentication context");
        }

        String schemeId = request.getSchemeId();
        Scheme scheme = null;

        if (schemeId != null && !schemeId.trim().isEmpty()) {
            scheme = schemeRepository.findBySchemeCodeAndStatus(schemeId.trim(), "ACTIVE").orElse(null);
            if (scheme == null) {
                scheme = schemeRepository.findById(schemeId.trim()).orElse(null);
            }
            if (scheme == null) {
                throw new ResourceNotFoundException("Scheme not found with ID or code: " + schemeId);
            }
        } else {
            // Fall back to first active scheme if no schemeId provided
            List<Scheme> activeSchemes = schemeRepository.findByStatusOrderByPriorityAscPopularityScoreDesc("ACTIVE");
            if (!activeSchemes.isEmpty()) {
                scheme = activeSchemes.get(0);
            }
        }

        if (scheme == null) {
            throw new ResourceNotFoundException("No active schemes found for document comparison");
        }

        // 1. Fetch citizen's vault documents from MongoDB
        List<DocumentMetadata> vaultDocs = documentRepository.findByAuthUserIdAndActiveTrue(authUserId);

        // 2. Perform DETERMINISTIC DOCUMENT MATCHING against Scheme required documents
        List<SchemeDocument> requiredSchemeDocs = scheme.getDocuments() != null ? scheme.getDocuments() : List.of();

        List<DocumentStatusItem> availableDocs = new ArrayList<>();
        List<DocumentStatusItem> missingDocs = new ArrayList<>();
        List<DocumentStatusItem> pendingDocs = new ArrayList<>();
        List<DocumentStatusItem> rejectedDocs = new ArrayList<>();

        for (SchemeDocument reqDoc : requiredSchemeDocs) {
            String reqType = reqDoc.getDocumentType() != null ? reqDoc.getDocumentType() : "";
            String reqName = reqDoc.getDocumentName() != null ? reqDoc.getDocumentName() : "";
            boolean isMandatory = reqDoc.getMandatory() == null || reqDoc.getMandatory();

            DocumentMetadata matchedVaultDoc = findMatchingVaultDoc(reqType, reqName, vaultDocs);

            if (matchedVaultDoc == null) {
                missingDocs.add(DocumentStatusItem.builder()
                        .documentName(reqName.isEmpty() ? reqType : reqName)
                        .documentType(reqType)
                        .mandatory(isMandatory)
                        .status("MISSING")
                        .remarks("Required document not found in your Document Vault")
                        .build());
            } else {
                VerificationStatus status = matchedVaultDoc.getVerificationStatus();
                String remarks = matchedVaultDoc.getRemarks() != null ? matchedVaultDoc.getRemarks() : "";

                DocumentStatusItem item = DocumentStatusItem.builder()
                        .documentName(reqName.isEmpty() ? matchedVaultDoc.getDocumentName() : reqName)
                        .documentType(matchedVaultDoc.getDocumentType() != null ? matchedVaultDoc.getDocumentType().name() : reqType)
                        .mandatory(isMandatory)
                        .status(status != null ? status.name() : "UPLOADED")
                        .remarks(remarks)
                        .build();

                if (status == VerificationStatus.VERIFIED) {
                    availableDocs.add(item);
                } else if (status == VerificationStatus.UPLOADED || status == VerificationStatus.UNDER_REVIEW) {
                    pendingDocs.add(item);
                } else if (status == VerificationStatus.REJECTED || status == VerificationStatus.EXPIRED) {
                    rejectedDocs.add(item);
                } else {
                    pendingDocs.add(item);
                }
            }
        }

        String lang = "ta".equalsIgnoreCase(request.getLanguage()) ? "ta" : "en";

        int totalRequired = requiredSchemeDocs.size();
        int availableCount = availableDocs.size();
        int missingCount = missingDocs.size();
        int pendingCount = pendingDocs.size();
        int rejectedCount = rejectedDocs.size();

        // 3. Check AI status or return deterministic fallback
        if (!aiEnabled) {
            return buildFallbackResponse(scheme, availableDocs, missingDocs, pendingDocs, rejectedDocs,
                    totalRequired, availableCount, missingCount, pendingCount, rejectedCount, lang, true);
        }

        // 4. Construct constrained prompt for Gemini (NO PII sent!)
        String prompt = buildConstrainedPrompt(scheme, availableDocs, missingDocs, pendingDocs, rejectedDocs, lang);

        String aiText = geminiApiClient.generateContent(prompt);

        if (aiText != null && !aiText.trim().isEmpty()) {
            AiDocumentSuggestionsResponse parsed = parseGeminiResponse(aiText, scheme, availableDocs, missingDocs, pendingDocs, rejectedDocs,
                    totalRequired, availableCount, missingCount, pendingCount, rejectedCount, lang);
            if (parsed != null) {
                return parsed;
            }
        }

        // Fallback on Gemini error or failure
        return buildFallbackResponse(scheme, availableDocs, missingDocs, pendingDocs, rejectedDocs,
                totalRequired, availableCount, missingCount, pendingCount, rejectedCount, lang, false);
    }

    private DocumentMetadata findMatchingVaultDoc(String reqType, String reqName, List<DocumentMetadata> vaultDocs) {
        if (vaultDocs == null || vaultDocs.isEmpty()) return null;

        for (DocumentMetadata doc : vaultDocs) {
            String docTypeStr = doc.getDocumentType() != null ? doc.getDocumentType().name() : "";
            String docNameStr = doc.getDocumentName() != null ? doc.getDocumentName() : "";

            if (!reqType.isEmpty() && (docTypeStr.equalsIgnoreCase(reqType) || reqType.equalsIgnoreCase(docTypeStr))) {
                return doc;
            }
            if (!reqName.isEmpty() && (docNameStr.equalsIgnoreCase(reqName) || docNameStr.toLowerCase().contains(reqName.toLowerCase()))) {
                return doc;
            }
        }
        return null;
    }

    private String buildConstrainedPrompt(Scheme scheme, List<DocumentStatusItem> available,
                                         List<DocumentStatusItem> missing, List<DocumentStatusItem> pending,
                                         List<DocumentStatusItem> rejected, String lang) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the SchemeBridge document assistance system.\n");
        sb.append("CRITICAL RULES:\n");
        sb.append("1. Use ONLY the verified backend document requirements and deterministic matching results provided below.\n");
        sb.append("2. Do NOT invent document requirements or government policies.\n");
        sb.append("3. Do NOT claim a document is legally valid or change document requirements.\n");
        sb.append("4. Respond strictly in the requested language: ").append("ta".equals(lang) ? "Tamil (தமிழ்)" : "English").append(".\n\n");

        sb.append("SCHEME METADATA:\n");
        sb.append("- Code: ").append(scheme.getSchemeCode()).append("\n");
        sb.append("- Title: ").append(scheme.getTitleEnglish()).append("\n\n");

        sb.append("DETERMINISTIC DOCUMENT MATCHING RESULT (NO PII):\n");
        sb.append("- Available (Verified) Documents: ");
        available.forEach(d -> sb.append(d.getDocumentName()).append(" [").append(d.getStatus()).append("], "));
        sb.append("\n- Missing Documents: ");
        missing.forEach(d -> sb.append(d.getDocumentName()).append(" (Mandatory: ").append(d.isMandatory()).append("), "));
        sb.append("\n- Pending Verification Documents: ");
        pending.forEach(d -> sb.append(d.getDocumentName()).append(" [").append(d.getStatus()).append("], "));
        sb.append("\n- Rejected / Expired Documents: ");
        rejected.forEach(d -> sb.append(d.getDocumentName()).append(" [").append(d.getStatus()).append("], "));
        sb.append("\n\n");

        sb.append("Respond ONLY with a valid JSON object in this exact format:\n");
        sb.append("{\n");
        sb.append("  \"suggestion\": \"<Clear guidance explaining missing/pending documents in ").append("ta".equals(lang) ? "Tamil" : "English").append(">\",\n");
        sb.append("  \"suggestedNextSteps\": [\"<Next action 1>\", \"<Next action 2>\"]\n");
        sb.append("}\n");

        return sb.toString();
    }

    private AiDocumentSuggestionsResponse parseGeminiResponse(String aiText, Scheme scheme,
                                                              List<DocumentStatusItem> available, List<DocumentStatusItem> missing,
                                                              List<DocumentStatusItem> pending, List<DocumentStatusItem> rejected,
                                                              int total, int avail, int miss, int pend, int rej, String lang) {
        try {
            String cleaned = aiText.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();

            JsonNode node = objectMapper.readTree(cleaned);
            String suggestion = node.has("suggestion") ? node.get("suggestion").asText() : aiText;
            List<String> nextSteps = new ArrayList<>();
            if (node.has("suggestedNextSteps") && node.get("suggestedNextSteps").isArray()) {
                node.get("suggestedNextSteps").forEach(n -> nextSteps.add(n.asText()));
            }

            return AiDocumentSuggestionsResponse.builder()
                    .schemeId(scheme.getSchemeCode())
                    .schemeName(scheme.getTitleEnglish())
                    .language(lang)
                    .totalRequiredCount(total)
                    .availableCount(avail)
                    .missingCount(miss)
                    .pendingCount(pend)
                    .rejectedCount(rej)
                    .availableDocuments(available)
                    .missingDocuments(missing)
                    .pendingDocuments(pending)
                    .rejectedDocuments(rejected)
                    .suggestion(suggestion)
                    .suggestedNextSteps(nextSteps)
                    .isFallback(false)
                    .timestamp(Instant.now())
                    .build();
        } catch (Exception e) {
            log.warn("[AiDocumentSuggestionsService] Error parsing Gemini JSON response: {}", e.getMessage());
            return null;
        }
    }

    private AiDocumentSuggestionsResponse buildFallbackResponse(Scheme scheme, List<DocumentStatusItem> available,
                                                                List<DocumentStatusItem> missing, List<DocumentStatusItem> pending,
                                                                List<DocumentStatusItem> rejected, int total, int avail, int miss,
                                                                int pend, int rej, String lang, boolean isConfigDisabled) {
        boolean isTamil = "ta".equalsIgnoreCase(lang);
        String suggestion;
        List<String> nextSteps = new ArrayList<>();

        if (miss > 0) {
            StringBuilder missingNames = new StringBuilder();
            missing.forEach(d -> missingNames.append(d.getDocumentName()).append(", "));
            String namesStr = missingNames.length() > 2 ? missingNames.substring(0, missingNames.length() - 2) : missingNames.toString();

            if (isTamil) {
                suggestion = String.format("%s திட்டத்திற்கு நீங்கள் %d/%d சரிபார்க்கப்பட்ட ஆவணங்களை வைத்துள்ளீர்கள். " +
                        "விடுபட்ட ஆவணங்கள்: %s. விண்ணப்பிப்பதற்கு முன் இவற்றை பதிவேற்றவும்.",
                        scheme.getTitleTamil() != null ? scheme.getTitleTamil() : scheme.getTitleEnglish(),
                        avail, total, namesStr);
                nextSteps.add("ஆவண பெட்டகத்திற்குச் செல்லவும்");
                nextSteps.add("விடுபட்ட ஆவணங்களை பதிவேற்றவும்");
            } else {
                suggestion = String.format("You have %d of %d required documents verified in your Document Vault for %s. " +
                        "Missing documents: %s. Upload these to reach 100%% document readiness.",
                        avail, total, scheme.getTitleEnglish(), namesStr);
                nextSteps.add("Go to Document Vault");
                nextSteps.add("Upload missing documents");
            }
        } else if (pend > 0) {
            if (isTamil) {
                suggestion = String.format("உங்களின் அனைத்து ஆவணங்களும் பதிவேற்றப்பட்டுள்ளன. %d ஆவணம்(கள்) சரிபார்ப்பிற்கு நிலுவையில் உள்ளது.", pend);
                nextSteps.add("ஆவண சரிபார்ப்பு நிலையை சரிபார்க்கவும்");
            } else {
                suggestion = String.format("All required documents are uploaded. %d document(s) are currently pending verification by officers.", pend);
                nextSteps.add("Check verification status");
            }
        } else {
            if (isTamil) {
                suggestion = String.format("வாழ்த்துக்கள்! %s திட்டத்திற்கான அனைத்து %d ஆவணங்களும் சரிபார்க்கப்பட்டுள்ளன. நீங்கள் விண்ணப்பிக்கத் தயார்!",
                        scheme.getTitleTamil() != null ? scheme.getTitleTamil() : scheme.getTitleEnglish(), total);
                nextSteps.add("இப்பொழுதே விண்ணப்பிக்கவும்");
            } else {
                suggestion = String.format("Great news! All %d required documents for %s are verified in your vault. You are ready to apply!",
                        total, scheme.getTitleEnglish());
                nextSteps.add("Apply Now");
            }
        }

        return AiDocumentSuggestionsResponse.builder()
                .schemeId(scheme.getSchemeCode())
                .schemeName(scheme.getTitleEnglish())
                .language(lang)
                .totalRequiredCount(total)
                .availableCount(avail)
                .missingCount(miss)
                .pendingCount(pend)
                .rejectedCount(rej)
                .availableDocuments(available)
                .missingDocuments(missing)
                .pendingDocuments(pending)
                .rejectedDocuments(rejected)
                .suggestion(suggestion)
                .suggestedNextSteps(nextSteps)
                .isFallback(true)
                .timestamp(Instant.now())
                .build();
    }
}
