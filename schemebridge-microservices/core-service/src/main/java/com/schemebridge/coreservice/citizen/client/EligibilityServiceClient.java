package com.schemebridge.coreservice.citizen.client;

import com.schemebridge.coreservice.citizen.model.CitizenDocument;
import com.schemebridge.coreservice.citizen.model.EligibilitySnapshot;
import com.schemebridge.coreservice.scheme.dto.EligibilityEvaluationRequest;
import com.schemebridge.coreservice.scheme.dto.EligibilityEvaluationResponse;
import com.schemebridge.coreservice.scheme.service.EligibilityRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * EligibilityServiceClient — In-process component connecting Citizen Domain directly
 * to Scheme Domain EligibilityRuleService within Core Service.
 *
 * BUG FIX (Phase 2):
 *  - Age is now calculated from dateOfBirth using Period.between(), not passed as an arbitrary integer.
 *  - All supported EligibilityEvaluationRequest fields are now populated from CitizenDocument:
 *    category, gender, age (calculated), state, district, annualIncome, occupationType,
 *    religion, community, maritalStatus, isFarmer, isDisabled, isMinority, isStudent (null—not in model),
 *    isWidow (null—not in model), isSeniorCitizen (auto-derived from age >= 60).
 */
@Component
@Slf4j
public class EligibilityServiceClient {

    private final EligibilityRuleService eligibilityRuleService;

    public EligibilityServiceClient(@Lazy EligibilityRuleService eligibilityRuleService) {
        this.eligibilityRuleService = eligibilityRuleService;
    }

    public EligibilitySnapshot evaluateEligibilityFromSchemeService(CitizenDocument doc) {
        try {
            if (eligibilityRuleService != null && doc != null) {

                // ----------------------------------------------------------------
                // Calculate age from dateOfBirth (FIX: was never set before)
                // ----------------------------------------------------------------
                Integer age = null;
                Boolean isSeniorCitizen = null;
                if (doc.getPersonalDetails() != null && doc.getPersonalDetails().getDateOfBirth() != null) {
                    LocalDate dob = doc.getPersonalDetails().getDateOfBirth();
                    age = Period.between(dob, LocalDate.now()).getYears();
                    isSeniorCitizen = age >= 60;
                    log.debug("[EligibilityServiceClient] DOB={}, Calculated age={}", dob, age);
                }

                // ----------------------------------------------------------------
                // Build full EligibilityEvaluationRequest with all available fields
                // ----------------------------------------------------------------
                EligibilityEvaluationRequest req = EligibilityEvaluationRequest.builder()
                        .authUserId(doc.getAuthUserId())
                        // Personal
                        .category(doc.getPersonalDetails() != null ? doc.getPersonalDetails().getCategory() : null)
                        .gender(doc.getPersonalDetails() != null ? doc.getPersonalDetails().getGender() : null)
                        .age(age)
                        .religion(doc.getPersonalDetails() != null ? doc.getPersonalDetails().getReligion() : null)
                        .community(doc.getPersonalDetails() != null ? doc.getPersonalDetails().getCommunity() : null)
                        .maritalStatus(doc.getPersonalDetails() != null ? doc.getPersonalDetails().getMaritalStatus() : null)
                        // Address
                        .state(doc.getAddressDetails() != null ? doc.getAddressDetails().getState() : null)
                        .district(doc.getAddressDetails() != null ? doc.getAddressDetails().getDistrict() : null)
                        // Income
                        .annualIncome(doc.getIncomeDetails() != null && doc.getIncomeDetails().getAnnualIncome() != null
                                ? doc.getIncomeDetails().getAnnualIncome() : null)
                        // Occupation
                        .occupationType(doc.getOccupationDetails() != null ? doc.getOccupationDetails().getOccupationType() : null)
                        // Special categories
                        .isFarmer(doc.getSpecialCategoryDetails() != null ? doc.getSpecialCategoryDetails().getIsFarmer() : null)
                        .isDisabled(doc.getSpecialCategoryDetails() != null ? doc.getSpecialCategoryDetails().getIsDisabled() : null)
                        .isMinority(doc.getSpecialCategoryDetails() != null ? doc.getSpecialCategoryDetails().getIsMinority() : null)
                        // isStudent and isWidow are not currently in SpecialCategoryDetails model
                        .isStudent(null)
                        .isWidow(null)
                        // Senior citizen is auto-derived from age
                        .isSeniorCitizen(isSeniorCitizen)
                        .build();

                EligibilityEvaluationResponse res = eligibilityRuleService.evaluateEligibility(req);
                return EligibilitySnapshot.builder()
                        .eligibilityScore(res.getEligibilityScore())
                        .matchedSchemes(res.getMatchedSchemesCount())
                        .lastCalculated(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.warn("[EligibilityServiceClient] Direct evaluation fallback due to: {}", e.getMessage());
        }

        // Fallback calculation when eligibility engine fails
        int matchedSchemesCount = 0;
        double score = 0.0;
        if (doc != null && doc.getProfileCompletion() != null) {
            double completion = doc.getProfileCompletion().getCompletionPercentage();
            if (completion < 50.0) { score = 0.0; matchedSchemesCount = 0; }
            else if (completion < 80.0) { score = 40.0; matchedSchemesCount = 4; }
            else { score = 60.0; matchedSchemesCount = 9; }
        }

        return EligibilitySnapshot.builder()
                .eligibilityScore(score)
                .matchedSchemes(matchedSchemesCount)
                .lastCalculated(LocalDateTime.now())
                .build();
    }
}
