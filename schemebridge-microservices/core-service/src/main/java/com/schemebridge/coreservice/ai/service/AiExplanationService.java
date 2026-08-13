package com.schemebridge.coreservice.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.coreservice.ai.client.GeminiApiClient;
import com.schemebridge.coreservice.ai.dto.AiExplanationRequest;
import com.schemebridge.coreservice.ai.dto.AiExplanationResponse;
import com.schemebridge.coreservice.citizen.model.CitizenDocument;
import com.schemebridge.coreservice.citizen.repository.CitizenRepository;
import com.schemebridge.coreservice.scheme.dto.EligibilityEvaluationRequest;
import com.schemebridge.coreservice.scheme.dto.EligibilityEvaluationResponse;
import com.schemebridge.coreservice.scheme.service.EligibilityRuleService;

import com.schemebridge.coreservice.scheme.entity.Scheme;
import com.schemebridge.coreservice.scheme.repository.SchemeRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiExplanationService {

    private final GeminiApiClient geminiClient;
    private final CitizenRepository citizenRepository;
    private final SchemeRepository schemeRepository;
    private final EligibilityRuleService eligibilityRuleService;
    private final ObjectMapper objectMapper;

    @Value("${feature.ai.enabled:true}")
    private boolean aiEnabled;

    public AiExplanationResponse generateExplanation(String authUserId, AiExplanationRequest request) {
        String language = (request.getLanguage() != null && request.getLanguage().equalsIgnoreCase("ta")) ? "ta" : "en";

        // 1. Fetch trusted Scheme metadata from Oracle DB
        Scheme scheme = schemeRepository.findBySchemeCodeAndStatus(request.getSchemeId(), "ACTIVE")
                .or(() -> schemeRepository.findById(request.getSchemeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", "idOrCode", request.getSchemeId()));

        // 2. Fetch Citizen Profile from MongoDB
        CitizenDocument citizen = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId)
                .orElse(null);


        // 3. Build sanitized eligibility evaluation request
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

        // 4. Run authoritative deterministic eligibility evaluation
        EligibilityEvaluationResponse evalRes = eligibilityRuleService.evaluateEligibility(evalReq);

        boolean isMatched = evalRes.getMatchedSchemes() != null &&
                evalRes.getMatchedSchemes().stream().anyMatch(s -> s.getSchemeCode().equalsIgnoreCase(scheme.getSchemeCode()));

        List<String> schemeNotes = evalRes.getEvaluationNotes() != null ?
                evalRes.getEvaluationNotes().stream().filter(n -> n.contains(scheme.getSchemeCode())).toList() : List.of();

        // If AI is disabled or key missing, return deterministic fallback
        if (!aiEnabled) {
            return buildFallbackResponse(scheme, isMatched, evalRes.getEligibilityScore(), schemeNotes, language);
        }

        // 5. Build constrained system prompt (Data Minimization: NO PII!)
        String prompt = buildConstrainedPrompt(scheme, evalReq, isMatched, evalRes.getEligibilityScore(), schemeNotes, language);

        // 6. Invoke Gemini API Client
        String jsonText = geminiClient.generateContent(prompt);

        if (jsonText != null && !jsonText.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(jsonText);
                String explanation = root.has("explanation") ? root.get("explanation").asText() : null;

                List<String> takeaways = new ArrayList<>();
                if (root.has("keyTakeaways") && root.get("keyTakeaways").isArray()) {
                    root.get("keyTakeaways").forEach(node -> takeaways.add(node.asText()));
                }

                List<String> nextSteps = new ArrayList<>();
                if (root.has("suggestedNextSteps") && root.get("suggestedNextSteps").isArray()) {
                    root.get("suggestedNextSteps").forEach(node -> nextSteps.add(node.asText()));
                }

                if (explanation != null && !explanation.trim().isEmpty()) {
                    return AiExplanationResponse.builder()
                            .schemeCode(scheme.getSchemeCode())
                            .schemeName(scheme.getTitleEnglish())
                            .language(language)
                            .isEligible(isMatched)
                            .matchScore(evalRes.getEligibilityScore())
                            .explanation(explanation)
                            .confidence("HIGH")
                            .keyTakeaways(takeaways)
                            .suggestedNextSteps(nextSteps)
                            .isFallback(false)
                            .timestamp(LocalDateTime.now().toString())
                            .build();
                }
            } catch (Exception e) {
                log.warn("[AiExplanationService] JSON parse error from Gemini response: {}", e.getMessage());
            }
        }

        // Fallback if AI unavailable or response unparseable
        return buildFallbackResponse(scheme, isMatched, evalRes.getEligibilityScore(), schemeNotes, language);
    }

    private String buildConstrainedPrompt(Scheme scheme, EligibilityEvaluationRequest profile, boolean isMatched, Double score, List<String> notes, String language) {
        String langInstruction = language.equalsIgnoreCase("ta")
                ? "Provide your output text (explanation, keyTakeaways, suggestedNextSteps) in Tamil."
                : "Provide your output text in simple English.";

        return """
                You are the SchemeBridge citizen assistance system.
                Use ONLY the verified scheme metadata and authoritative eligibility evaluation notes supplied below.
                Do NOT invent eligibility criteria, benefits, deadlines, documents, or government policies.
                The backend eligibility engine is authoritative. You MUST NOT change or override eligibility results.
                %s

                Return ONLY a JSON object matching this exact format:
                {
                  "explanation": "Clear explanation of why citizen matches or fails...",
                  "keyTakeaways": ["Benefit or feature 1", "Benefit or feature 2"],
                  "suggestedNextSteps": ["Action step 1", "Action step 2"]
                }

                VERIFIED SCHEME METADATA:
                Scheme Code: %s
                Scheme Title: %s
                Description: %s

                AUTHORITATIVE ELIGIBILITY RESULT:
                Is Eligible: %b
                Eligibility Score: %.1f%%
                Rule Notes: %s

                SANITIZED CITIZEN PROFILE SUMMARY:
                Age: %s
                Social Category: %s
                Annual Household Income: %s
                State: %s
                Occupation: %s
                """.formatted(
                langInstruction,
                scheme.getSchemeCode(),
                scheme.getTitleEnglish(),
                scheme.getDescriptionEnglish() != null ? scheme.getDescriptionEnglish() : "N/A",
                isMatched,
                score != null ? score : 0.0,
                notes != null && !notes.isEmpty() ? String.join("; ", notes) : "Criteria evaluated against scheme rules",
                profile.getAge() != null ? profile.getAge() : "N/A",
                profile.getCategory() != null ? profile.getCategory() : "N/A",
                profile.getAnnualIncome() != null ? "Rs. " + profile.getAnnualIncome() : "N/A",
                profile.getState() != null ? profile.getState() : "N/A",
                profile.getOccupationType() != null ? profile.getOccupationType() : "N/A"
        );
    }

    private AiExplanationResponse buildFallbackResponse(Scheme scheme, boolean isMatched, Double score, List<String> notes, String language) {
        boolean isTa = language.equalsIgnoreCase("ta");

        String explanation;
        List<String> takeaways = new ArrayList<>();
        List<String> nextSteps = new ArrayList<>();

        if (isMatched) {
            explanation = isTa
                    ? "உங்கள் சரிபார்க்கப்பட்ட சுயவிவர பண்புகளின் அடிப்படையில் நீங்கள் " + scheme.getTitleEnglish() + " திட்டத்திற்கு தகுதியுடையவர்."
                    : "You qualify for " + scheme.getTitleEnglish() + " based on your verified profile attributes. Matched rules: " +
                    (notes != null && !notes.isEmpty() ? String.join("; ", notes) : "Income, age, and category criteria satisfied.");

            takeaways.add(isTa ? "உங்கள் வருமானம் மற்றும் வயது வரம்புகள் தகுதியான பிரிவில் உள்ளன." : "Your income and age brackets satisfy the official scheme guidelines.");
            takeaways.add(isTa ? "நேரடி பயன் பரிமாற்றம் தகுதியானது." : "Eligible for Direct Benefit Transfer (DBT) upon application submission.");

            nextSteps.add(isTa ? "உங்கள் ஆவணங்களை சரிபார்க்கவும்." : "Verify your required documents in your Document Vault.");
            nextSteps.add(isTa ? "விண்ணப்பிக்கவும் பொத்தானைக் கிளிக் செய்யவும்." : "Click 'Apply Now' to submit your application.");
        } else {
            explanation = isTa
                    ? "உங்கள் சுயவிவரம் " + scheme.getTitleEnglish() + " திட்டத்திற்கான அனைத்து கட்டாய தகுதிகளையும் தற்போது பூர்த்தி செய்யவில்லை."
                    : "Your profile does not currently satisfy all mandatory eligibility criteria for " + scheme.getTitleEnglish() + ". " +
                    (notes != null && !notes.isEmpty() ? "Details: " + String.join("; ", notes) : "Please review scheme guidelines.");

            takeaways.add(isTa ? "கட்டாய விதிமுறைகள் பூர்த்தி செய்யப்படவில்லை." : "Mandatory rule constraints were not met by current profile attributes.");
            nextSteps.add(isTa ? "உங்கள் சுயவிவரத்தை புதுப்பிக்கவும்." : "Update your profile details or review alternative recommended schemes.");
        }

        return AiExplanationResponse.builder()
                .schemeCode(scheme.getSchemeCode())
                .schemeName(scheme.getTitleEnglish())
                .language(language)
                .isEligible(isMatched)
                .matchScore(score != null ? score : 0.0)
                .explanation(explanation)
                .confidence("HIGH")
                .keyTakeaways(takeaways)
                .suggestedNextSteps(nextSteps)
                .isFallback(true)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
