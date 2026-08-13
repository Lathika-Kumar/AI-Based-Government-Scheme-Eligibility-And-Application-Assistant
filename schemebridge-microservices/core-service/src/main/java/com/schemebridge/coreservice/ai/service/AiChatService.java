package com.schemebridge.coreservice.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.common.exception.ForbiddenException;
import com.schemebridge.common.exception.ResourceNotFoundException;
import com.schemebridge.common.exception.UnauthorizedException;

import com.schemebridge.coreservice.ai.client.GeminiApiClient;
import com.schemebridge.coreservice.ai.dto.AiChatRequest;
import com.schemebridge.coreservice.ai.dto.AiChatResponse;
import com.schemebridge.coreservice.ai.model.AiChatConversationDocument;
import com.schemebridge.coreservice.ai.repository.AiChatConversationRepository;
import com.schemebridge.coreservice.citizen.model.CitizenDocument;
import com.schemebridge.coreservice.citizen.repository.CitizenRepository;
import com.schemebridge.coreservice.scheme.dto.EligibilityEvaluationRequest;
import com.schemebridge.coreservice.scheme.dto.EligibilityEvaluationResponse;
import com.schemebridge.coreservice.scheme.entity.Scheme;
import com.schemebridge.coreservice.scheme.repository.SchemeRepository;
import com.schemebridge.coreservice.scheme.service.EligibilityRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final GeminiApiClient geminiApiClient;
    private final SchemeRepository schemeRepository;
    private final CitizenRepository citizenRepository;
    private final EligibilityRuleService eligibilityRuleService;
    private final AiChatConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    @Value("${feature.ai.enabled:true}")
    private boolean aiEnabled;

    public AiChatResponse processChat(String authUserId, AiChatRequest request) {
        if (authUserId == null || authUserId.trim().isEmpty()) {
            throw new UnauthorizedException("Unauthorized: Missing user authentication context");
        }

        String conversationId = request.getConversationId();
        AiChatConversationDocument conversation = null;

        // 1. Validate / retrieve conversation
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            Optional<AiChatConversationDocument> convOpt = conversationRepository.findByConversationId(conversationId.trim());
            if (convOpt.isPresent()) {
                conversation = convOpt.get();

                if (!conversation.getAuthUserId().equals(authUserId)) {
                    log.warn("[AiChatService] Access denied: User {} attempted to access conversation {} owned by {}",
                            authUserId, conversationId, conversation.getAuthUserId());
                    throw new ForbiddenException("Access Denied: You do not own this conversation");
                }

            } else {
                log.info("[AiChatService] Conversation {} not found, creating new instance", conversationId);
            }
        }

        // Create new conversation if needed
        if (conversation == null) {
            conversationId = "CONV-" + UUID.randomUUID().toString();
            conversation = AiChatConversationDocument.builder()
                    .conversationId(conversationId)
                    .authUserId(authUserId)
                    .schemeId(request.getSchemeId())
                    .messages(new ArrayList<>())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        // 2. Validate scheme if provided
        Scheme scheme = null;
        String schemeId = request.getSchemeId() != null ? request.getSchemeId() : conversation.getSchemeId();
        if (schemeId != null && !schemeId.trim().isEmpty()) {
            scheme = schemeRepository.findBySchemeCodeAndStatus(schemeId.trim(), "ACTIVE")
                    .orElse(null);

            if (scheme == null) {
                scheme = schemeRepository.findById(schemeId.trim()).orElse(null);
            }

            if (scheme == null) {
                throw new ResourceNotFoundException("Scheme not found with ID or code: " + schemeId);
            }
            conversation.setSchemeId(scheme.getSchemeCode());
        }

        // 3. Retrieve citizen profile & sanitize (NO PII)
        CitizenDocument citizen = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId).orElse(null);
        Integer age = null;
        if (citizen != null && citizen.getPersonalDetails() != null && citizen.getPersonalDetails().getDateOfBirth() != null) {
            age = Period.between(citizen.getPersonalDetails().getDateOfBirth(), LocalDate.now()).getYears();
        }

        EligibilityEvaluationRequest evalReq = EligibilityEvaluationRequest.builder()
                .authUserId(authUserId)
                .category(citizen != null && citizen.getPersonalDetails() != null ? citizen.getPersonalDetails().getCategory() : null)
                .gender(citizen != null && citizen.getPersonalDetails() != null ? citizen.getPersonalDetails().getGender() : null)
                .age(age)
                .annualIncome(citizen != null && citizen.getIncomeDetails() != null ? citizen.getIncomeDetails().getAnnualIncome() : null)
                .state(citizen != null && citizen.getAddressDetails() != null ? citizen.getAddressDetails().getState() : null)
                .district(citizen != null && citizen.getAddressDetails() != null ? citizen.getAddressDetails().getDistrict() : null)
                .occupationType(citizen != null && citizen.getOccupationDetails() != null ? citizen.getOccupationDetails().getOccupationType() : null)
                .religion(citizen != null && citizen.getPersonalDetails() != null ? citizen.getPersonalDetails().getReligion() : null)
                .community(citizen != null && citizen.getPersonalDetails() != null ? citizen.getPersonalDetails().getCommunity() : null)
                .maritalStatus(citizen != null && citizen.getPersonalDetails() != null ? citizen.getPersonalDetails().getMaritalStatus() : null)
                .isFarmer(citizen != null && citizen.getSpecialCategoryDetails() != null ? citizen.getSpecialCategoryDetails().getIsFarmer() : null)
                .isDisabled(citizen != null && citizen.getSpecialCategoryDetails() != null ? citizen.getSpecialCategoryDetails().getIsDisabled() : null)
                .isMinority(citizen != null && citizen.getSpecialCategoryDetails() != null ? citizen.getSpecialCategoryDetails().getIsMinority() : null)
                .isSeniorCitizen(age != null ? age >= 60 : null)
                .build();

        EligibilityEvaluationResponse evalRes = eligibilityRuleService.evaluateEligibility(evalReq);

        String lang = "ta".equalsIgnoreCase(request.getLanguage()) ? "ta" : "en";

        // 4. Handle AI disabled fallback
        if (!aiEnabled) {
            return generateFallbackResponse(conversation, request.getMessage(), scheme, evalRes, lang, true);
        }

        // 5. Construct constrained prompt for Gemini
        String prompt = buildConstrainedChatPrompt(request.getMessage(), scheme, evalReq, evalRes, conversation.getMessages(), lang);

        String aiText = geminiApiClient.generateContent(prompt);

        AiChatResponse response;
        if (aiText != null && !aiText.trim().isEmpty()) {
            response = parseGeminiResponse(aiText, conversationId, scheme != null ? scheme.getSchemeCode() : null, lang);
        } else {
            response = generateFallbackResponse(conversation, request.getMessage(), scheme, evalRes, lang, false);
        }

        // 6. Append message turn to conversation and save to MongoDB
        Instant now = Instant.now();
        conversation.getMessages().add(AiChatConversationDocument.ChatMessage.builder()
                .role("user")
                .content(request.getMessage())
                .timestamp(now)
                .build());

        conversation.getMessages().add(AiChatConversationDocument.ChatMessage.builder()
                .role("assistant")
                .content(response.getMessage())
                .timestamp(now)
                .build());

        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        response.setConversationId(conversationId);
        response.setSchemeId(scheme != null ? scheme.getSchemeCode() : null);

        return response;
    }

    private String buildConstrainedChatPrompt(String userMessage, Scheme scheme, EligibilityEvaluationRequest profile,
                                             EligibilityEvaluationResponse evalRes,
                                             List<AiChatConversationDocument.ChatMessage> history, String lang) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are SchemeAI, the official e-governance assistant for SchemeBridge.\n");
        sb.append("CRITICAL RULES:\n");
        sb.append("1. EligibilityRuleService is the ONLY authoritative source of truth for scheme eligibility and match scores.\n");
        sb.append("2. You CANNOT make, override, approve, reject, or invent legal eligibility decisions.\n");
        sb.append("3. You CANNOT perform administrative actions (submit, approve, or reject applications, modify profiles or documents). If asked to approve or perform actions, explain clearly that you are an informational assistant and cannot mutate application records.\n");
        sb.append("4. Do NOT invent government policies, deadlines, benefits, or document requirements outside verified scheme data.\n");
        sb.append("5. Respond strictly in the requested language: ").append("ta".equals(lang) ? "Tamil (தமிழ்)" : "English").append(".\n\n");

        if (scheme != null) {
            sb.append("CURRENT SCHEME DATA:\n");
            sb.append("- Code: ").append(scheme.getSchemeCode()).append("\n");
            sb.append("- Title: ").append(scheme.getTitleEnglish()).append("\n");
            sb.append("- Description: ").append(scheme.getDescriptionEnglish()).append("\n");
        }

        if (profile != null) {
            sb.append("\nCITIZEN DEMOGRAPHIC CONTEXT (SANITIZED - NO PII):\n");
            sb.append("- Age: ").append(profile.getAge()).append("\n");
            sb.append("- Category: ").append(profile.getCategory()).append("\n");
            sb.append("- Annual Income: ₹").append(profile.getAnnualIncome()).append("\n");
            sb.append("- State: ").append(profile.getState()).append("\n");
        }

        if (evalRes != null) {
            sb.append("\nAUTHORITATIVE ELIGIBILITY EVALUATION:\n");
            sb.append("- Eligibility Score: ").append(String.format("%.1f", evalRes.getEligibilityScore())).append("%\n");
            sb.append("- Evaluation Notes: ").append(evalRes.getEvaluationNotes()).append("\n");
        }

        // Include recent 6 chat history turns
        if (history != null && !history.isEmpty()) {
            sb.append("\nRECENT CONVERSATION HISTORY:\n");
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                AiChatConversationDocument.ChatMessage msg = history.get(i);
                sb.append(msg.getRole().toUpperCase()).append(": ").append(msg.getContent()).append("\n");
            }
        }

        sb.append("\nCITIZEN CURRENT QUESTION: ").append(userMessage).append("\n\n");
        sb.append("Respond ONLY with a valid JSON object in this format:\n");
        sb.append("{\n");
        sb.append("  \"message\": \"<Clear explanation in ").append("ta".equals(lang) ? "Tamil" : "English").append(">\",\n");
        sb.append("  \"suggestions\": [\"<Suggested follow-up query 1>\", \"<Suggested follow-up query 2>\"]\n");
        sb.append("}\n");

        return sb.toString();
    }

    private AiChatResponse parseGeminiResponse(String aiText, String conversationId, String schemeId, String lang) {
        try {
            String cleaned = aiText.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            JsonNode node = objectMapper.readTree(cleaned);
            String message = node.has("message") ? node.get("message").asText() : aiText;
            List<String> suggestions = new ArrayList<>();

            if (node.has("suggestions") && node.get("suggestions").isArray()) {
                for (JsonNode item : node.get("suggestions")) {
                    suggestions.add(item.asText());
                }
            }

            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .schemeId(schemeId)
                    .message(message)
                    .language(lang)
                    .isFallback(false)
                    .suggestions(suggestions)
                    .timestamp(Instant.now())
                    .build();
        } catch (Exception e) {
            log.warn("[AiChatService] Failed to parse Gemini JSON response, returning raw text: {}", e.getMessage());
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .schemeId(schemeId)
                    .message(aiText)
                    .language(lang)
                    .isFallback(false)
                    .suggestions(List.of("Check eligibility score", "Required documents"))
                    .timestamp(Instant.now())
                    .build();
        }
    }

    private AiChatResponse generateFallbackResponse(AiChatConversationDocument conversation, String userMessage, Scheme scheme,
                                                    EligibilityEvaluationResponse evalRes, String lang, boolean isConfigDisabled) {
        String msgLower = userMessage.toLowerCase();
        String replyText;
        List<String> suggestions;

        boolean isTamil = "ta".equalsIgnoreCase(lang);

        if (msgLower.contains("approve") || msgLower.contains("submit application")) {
            replyText = isTamil
                    ? "நான் ஒரு தகவல் தரும் உதவி கருவி மட்டுமே. உங்களது விண்ணப்பங்களை என்னால் அங்கீகரிக்கவோ அல்லது மாற்றவோ முடியாது."
                    : "I am an informational e-governance assistant. I cannot approve, submit, or modify applications directly. Please click 'Apply Now' to complete your application.";
            suggestions = isTamil ? List.of("தகுதி விவரங்கள்", "தேவையான ஆவணங்கள்") : List.of("View eligibility details", "Check missing documents");
        } else if (msgLower.contains("invent") || msgLower.contains("fake")) {
            replyText = isTamil
                    ? "நான் அரசாங்க விதிமுறைகளையோ அல்லது தகுதி நிபந்தனைகளையோ சுயமாக உருவாக்க முடியாது. சரிபார்க்கப்பட்ட அதிகாரப்பூர்வ தரவு மட்டுமே பயன்படுத்தப்படும்."
                    : "I cannot invent eligibility criteria or government policies. SchemeBridge relies strictly on verified backend data and the authoritative EligibilityRuleService.";
            suggestions = isTamil ? List.of("எனது தகுதி மதிப்பெண்", "திட்ட விவரங்கள்") : List.of("My eligibility score", "Scheme details");
        } else if (scheme != null && evalRes != null) {
            Double score = evalRes.getEligibilityScore() != null ? evalRes.getEligibilityScore() : 80.0;
            if (isTamil) {
                replyText = String.format("உங்கள் சரிபார்க்கப்பட்ட சுயவிவர பண்புகளின் அடிப்படையில் %s திட்டத்திற்கான தகுதி மதிப்பெண் %.1f%% ஆகும். " +
                        "விண்ணப்பிப்பதற்கு முன் உங்களது ஆவண பெட்டகத்தை சரிபார்க்கவும்.",
                        scheme.getTitleTamil() != null ? scheme.getTitleTamil() : scheme.getTitleEnglish(),
                        score);
                suggestions = List.of("ஆவணங்களின் பட்டியல்", "விண்ணப்பிக்கும் முறை");
            } else {
                replyText = String.format("Based on your verified profile attributes, your match score for %s is %.1f%%. " +
                        "Please ensure your required documents are uploaded before submitting your application.",
                        scheme.getTitleEnglish(),
                        score);
                suggestions = List.of("Required documents", "Application process");
            }
        } else {
            if (isTamil) {
                replyText = "வணக்கம்! நான் SchemeAI. உங்களின் திட்டம், தகுதி வரம்புகள், மற்றும் தேவையான ஆவணங்கள் குறித்த கேள்விகளை கேட்கலாம்.";
                suggestions = List.of("எனது தகுதி வரம்பு", "தேவையான ஆவணங்கள்");
            } else {
                replyText = "Hello! I am SchemeAI. You can ask me about matching schemes, missing documents, or eligibility criteria based on your verified profile.";
                suggestions = List.of("Am I eligible?", "Which documents are missing?");
            }
        }

        return AiChatResponse.builder()
                .conversationId(conversation != null ? conversation.getConversationId() : "CONV-FALLBACK")
                .schemeId(scheme != null ? scheme.getSchemeCode() : null)
                .message(replyText)
                .language(lang)
                .isFallback(true)
                .suggestions(suggestions)
                .timestamp(Instant.now())
                .build();
    }
}
