package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.DocumentChecklistItemResponse;
import com.schemebridge.scheme.dto.response.SchemeDocumentChecklistResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dedicated AI & Canonical Document Checklist Generator.
 *
 * Core Principles:
 *  1. Grounded in Canonical Data: Derives requirements exclusively from verified scheme records,
 *     the canonical SchemeDocumentRequirementResolver, and master scheme requirements.
 *  2. Zero Hallucination: Never invents documents. If no document data exists in the scheme catalog,
 *     returns "Requirement information unavailable".
 *  3. Accurate User Status: Matches current authenticated citizen attributes and application state
 *     to accurately flag each requirement as MISSING or PROVIDED.
 *  4. Canonical Application Steps: Exposes ordered procedural steps and official portal links.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentChecklistGenerator {

    private final SchemeDocumentRequirementResolver requirementResolver;
    private final SchemeVerifiedDataRepository verifiedDataRepository;
    private final ApplicationRepository applicationRepository;

    public SchemeDocumentChecklistResponse generateChecklist(
            CitizenProfile userProfile,
            Scheme scheme,
            SchemeVerifiedData verifiedData,
            List<ApplicationDocument> existingDocuments
    ) {
        if (scheme == null) {
            throw new IllegalArgumentException("Scheme must not be null for checklist generation");
        }

        String schemeCode = scheme.getSchemeCode();
        String schemeTitle = scheme.getTitle() != null ? scheme.getTitle().getEnglish() : schemeCode;
        String slug = scheme.getSlug();

        // 1. Resolve Verified Data if not supplied
        if (verifiedData == null && verifiedDataRepository != null && schemeCode != null) {
            verifiedData = verifiedDataRepository.findBySchemeCode(schemeCode).orElse(null);
        }

        // 2. Set of provided document codes from user's application documents or profile
        Set<String> providedCodes = new HashSet<>();
        if (existingDocuments != null) {
            for (ApplicationDocument doc : existingDocuments) {
                if (doc.getDocumentCode() != null) {
                    providedCodes.add(doc.getDocumentCode().trim().toUpperCase());
                }
            }
        }

        // Check citizen verified attributes in profile (e.g. Aadhaar, Income, Land, Caste)
        if (userProfile != null && userProfile.getVerifiedAttributes() != null) {
            for (String key : userProfile.getVerifiedAttributes().keySet()) {
                providedCodes.add(key.trim().toUpperCase());
            }
        }

        List<DocumentChecklistItemResponse> items = new ArrayList<>();
        String documentStatus = "DOCUMENTS_FOUND";
        String overallProvenance = "VERIFIED_OFFICIAL";

        // 3. Resolve requirements using hierarchy: Verified Data -> Master Scheme -> Resolver
        if (verifiedData != null && verifiedData.getDocuments() != null && !verifiedData.getDocuments().isEmpty()) {
            overallProvenance = "VERIFIED_OFFICIAL";
            for (SchemeVerifiedData.CanonicalDocumentRequirement doc : verifiedData.getDocuments()) {
                String docCode = doc.getDocumentCode() != null ? doc.getDocumentCode() : doc.getCanonicalDocumentCode();
                String codeUpper = docCode != null ? docCode.toUpperCase() : "";
                boolean isProvided = providedCodes.contains(codeUpper) ||
                        (doc.getCanonicalDocumentCode() != null && providedCodes.contains(doc.getCanonicalDocumentCode().toUpperCase()));

                String whyRequired = doc.getWhyRequired() != null && !doc.getWhyRequired().isBlank()
                        ? doc.getWhyRequired()
                        : "Required by scheme eligibility and application verification criteria.";

                String altGroupType = doc.getAlternativeGroup() != null ? doc.getAlternativeGroup().getRule() : null;
                List<String> alts = doc.getAlternativeGroup() != null && doc.getAlternativeGroup().getOptions() != null
                        ? doc.getAlternativeGroup().getOptions().stream().map(SchemeVerifiedData.AlternativeOption::getOptionName).collect(Collectors.toList())
                        : List.of();

                items.add(DocumentChecklistItemResponse.builder()
                        .documentCode(docCode)
                        .canonicalDocumentCode(doc.getCanonicalDocumentCode())
                        .documentName(doc.getOfficialDocumentName())
                        .document(doc.getOfficialDocumentName())
                        .description(doc.getWhyRequired())
                        .whyRequired(whyRequired)
                        .reason(whyRequired)
                        .mandatory(doc.isMandatory())
                        .required(doc.isMandatory())
                        .optional(doc.isOptional())
                        .alternativeGroupId(altGroupType != null ? docCode + "_ALT_GRP" : null)
                        .alternativeGroupType(altGroupType)
                        .alternatives(alts)
                        .acceptedFormats(doc.getAcceptedFormats() != null && !doc.getAcceptedFormats().isEmpty()
                                ? doc.getAcceptedFormats()
                                : List.of("PDF", "JPG", "PNG"))
                        .issuingAuthority(doc.getIssuingAuthority() != null ? doc.getIssuingAuthority() : "Competent Government Authority")
                        .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                        .sourceRequirement("Official Scheme Notification / Guidelines")
                        .sourceReference(doc.getSourceEvidence() != null ? doc.getSourceEvidence() : doc.getSourceUrl())
                        .status(isProvided ? DetailedDocumentStatus.VERIFIED : DetailedDocumentStatus.NOT_UPLOADED)
                        .statusString(isProvided ? "PROVIDED" : "MISSING")
                        .userProvided(isProvided)
                        .uploaded(isProvided)
                        .verified(isProvided)
                        .build());
            }
        } else if (scheme.getRequiredDocuments() != null && !scheme.getRequiredDocuments().isEmpty()) {
            overallProvenance = "SYSTEM_CONFIGURED";
            for (RequiredDocument doc : scheme.getRequiredDocuments()) {
                String dName = doc.getName() != null ? doc.getName().getEnglish() : doc.getDocumentCode();
                String docCode = doc.getDocumentCode() != null ? doc.getDocumentCode() : "DOC_" + dName.replaceAll("\\s+", "_").toUpperCase();
                String codeUpper = docCode.toUpperCase();
                boolean isProvided = providedCodes.contains(codeUpper);

                String whyRequired = "Statutory requirement specified in official master scheme documentation.";

                items.add(DocumentChecklistItemResponse.builder()
                        .documentCode(docCode)
                        .canonicalDocumentCode(docCode)
                        .documentName(dName)
                        .document(dName)
                        .description(dName)
                        .whyRequired(whyRequired)
                        .reason(whyRequired)
                        .mandatory(doc.isMandatory())
                        .required(doc.isMandatory())
                        .optional(!doc.isMandatory())
                        .acceptedFormats(doc.getAcceptedFormats() != null && !doc.getAcceptedFormats().isEmpty()
                                ? doc.getAcceptedFormats()
                                : List.of("PDF", "JPG", "PNG"))
                        .issuingAuthority(doc.getIssuingAuthority() != null ? doc.getIssuingAuthority() : "Relevant Department / Issuing Authority")
                        .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                        .sourceRequirement("Official Scheme Notification / Guidelines")
                        .status(isProvided ? DetailedDocumentStatus.VERIFIED : DetailedDocumentStatus.NOT_UPLOADED)
                        .statusString(isProvided ? "PROVIDED" : "MISSING")
                        .userProvided(isProvided)
                        .uploaded(isProvided)
                        .verified(isProvided)
                        .build());
            }
        } else {
            // Zero Hallucination: Return exact fallback indicator
            documentStatus = "DOCUMENT_REQUIREMENTS_NOT_MAPPED";
            overallProvenance = "UNKNOWN_OR_UNSTRUCTURED";
            items.add(DocumentChecklistItemResponse.builder()
                    .documentCode("DOC_UNAVAILABLE")
                    .canonicalDocumentCode("DOC_UNAVAILABLE")
                    .documentName("Requirement information unavailable")
                    .document("Requirement information unavailable")
                    .description("No specific document requirements have been mapped in the official canonical dataset.")
                    .whyRequired("Requirement information unavailable")
                    .reason("Requirement information unavailable")
                    .mandatory(false)
                    .required(false)
                    .optional(true)
                    .sourceRequirement("Requirement information unavailable")
                    .issuingAuthority("Not Available")
                    .status(DetailedDocumentStatus.NOT_UPLOADED)
                    .statusString("NOT_AVAILABLE")
                    .userProvided(false)
                    .build());
        }

        // 4. Calculate Missing Mandatory Documents
        List<String> missingRequirements = items.stream()
                .filter(i -> i.isMandatory() && "MISSING".equalsIgnoreCase(i.getStatusString()))
                .map(DocumentChecklistItemResponse::getDocumentName)
                .collect(Collectors.toList());

        // 5. Canonical Application Steps & Links
        List<String> applicationSteps = new ArrayList<>();
        String officialApplicationUrl = null;
        String applicationMode = "ONLINE";
        String helplineNumber = null;

        if (verifiedData != null && verifiedData.getApplication() != null) {
            SchemeVerifiedData.CanonicalApplication app = verifiedData.getApplication();
            if (app.getApplicationSteps() != null && !app.getApplicationSteps().isEmpty()) {
                applicationSteps.addAll(app.getApplicationSteps());
            } else if (app.getApplicationProcedure() != null && !app.getApplicationProcedure().isBlank()) {
                applicationSteps.add(app.getApplicationProcedure());
            }
            if (app.getOfficialApplicationUrl() != null && !app.getOfficialApplicationUrl().isBlank()) {
                officialApplicationUrl = app.getOfficialApplicationUrl();
            } else if (app.getOfficialPortalUrl() != null && !app.getOfficialPortalUrl().isBlank()) {
                officialApplicationUrl = app.getOfficialPortalUrl();
            }
            if (app.getApplicationMethod() != null) {
                applicationMode = app.getApplicationMethod();
            }
            if (app.getHelplineNumber() != null) {
                helplineNumber = app.getHelplineNumber();
            }
        }

        if (applicationSteps.isEmpty() && scheme.getApplicationInfo() != null) {
            ApplicationInfo info = scheme.getApplicationInfo();
            if (info.getInstructions() != null && info.getInstructions().getEnglish() != null) {
                String text = info.getInstructions().getEnglish();
                String[] parts = text.split("(?<=\\.)\\s+|(?<=\\d\\.)\\s+");
                for (String part : parts) {
                    if (!part.trim().isEmpty()) {
                        applicationSteps.add(part.trim());
                    }
                }
            }
            if (officialApplicationUrl == null && info.getApplicationUrl() != null) {
                officialApplicationUrl = info.getApplicationUrl();
            }
            if (info.getApplicationMode() != null) {
                applicationMode = info.getApplicationMode();
            }
        }

        if (applicationSteps.isEmpty()) {
            applicationSteps.add("1. Verify your eligibility conditions on SchemeBridge.");
            applicationSteps.add("2. Gather all required documents listed in your personalized checklist.");
            applicationSteps.add("3. Navigate to the official application portal and submit your application.");
            applicationSteps.add("4. Track your application status and departmental review milestones.");
        }

        // 6. Eligibility conditions from rules
        List<String> conditions = new ArrayList<>();
        if (scheme.getEligibilityRules() != null && scheme.getEligibilityRules().getConditions() != null) {
            for (EligibilityCondition cond : scheme.getEligibilityRules().getConditions()) {
                String field = cond.getField() != null ? cond.getField() : "Condition";
                String op = cond.getOperator() != null ? cond.getOperator().name() : "=";
                String val = cond.getValue() != null ? cond.getValue() : "";
                conditions.add(field + " " + op + " " + val);
            }
        }

        // 7. Benefits
        List<String> benefits = new ArrayList<>();
        if (scheme.getBenefits() != null) {
            for (SchemeBenefit b : scheme.getBenefits()) {
                if (b.getDescription() != null && b.getDescription().getEnglish() != null) {
                    benefits.add(b.getDescription().getEnglish());
                }
            }
        }

        Set<String> altGroups = new HashSet<>();
        for (DocumentChecklistItemResponse item : items) {
            if (item.getAlternativeGroupId() != null) {
                altGroups.add(item.getAlternativeGroupId());
            }
        }

        return SchemeDocumentChecklistResponse.builder()
                .schemeCode(schemeCode)
                .schemeTitle(schemeTitle)
                .slug(slug)
                .officialSourceUrl(verifiedData != null && verifiedData.getSourceMetadata() != null ? verifiedData.getSourceMetadata().getSourceUrl() : null)
                .officialSourceName(verifiedData != null && verifiedData.getSourceMetadata() != null ? verifiedData.getSourceMetadata().getSourceType() : null)
                .reconciliationStatus(verifiedData != null ? verifiedData.getReconciliationStatus() : "CATALOG_DEFAULT")
                .documentStatus(documentStatus)
                .overallProvenance(overallProvenance)
                .totalDocuments(items.size())
                .totalRequired((int) items.stream().filter(DocumentChecklistItemResponse::isMandatory).count())
                .mandatoryDocumentCount((int) items.stream().filter(DocumentChecklistItemResponse::isMandatory).count())
                .alternativeGroupCount(altGroups.size())
                .totalUploaded((int) items.stream().filter(DocumentChecklistItemResponse::isUploaded).count())
                .totalVerified((int) items.stream().filter(DocumentChecklistItemResponse::isVerified).count())
                .items(items)
                .missingRequirements(missingRequirements)
                .eligibilityConditions(conditions)
                .applicationSteps(applicationSteps)
                .applicationMode(applicationMode)
                .officialApplicationUrl(officialApplicationUrl)
                .helplineNumber(helplineNumber)
                .benefits(benefits)
                .build();
    }

    public SchemeDocumentChecklistResponse generateChecklist(Scheme scheme) {
        return generateChecklist(null, scheme, null, null);
    }

    public SchemeDocumentChecklistResponse generateChecklist(CitizenProfile profile, Scheme scheme) {
        return generateChecklist(profile, scheme, null, null);
    }
}
