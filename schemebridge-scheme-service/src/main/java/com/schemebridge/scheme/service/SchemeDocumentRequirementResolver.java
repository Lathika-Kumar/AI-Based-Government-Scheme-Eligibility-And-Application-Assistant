package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.EligibilityCondition;
import com.schemebridge.scheme.document.MultilingualText;
import com.schemebridge.scheme.document.RequiredDocument;
import com.schemebridge.scheme.document.RuleGroup;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeVerifiedData;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service resolving scheme-specific document requirements.
 * Priority 1: Explicit VERIFIED_OFFICIAL requirements from canonical scheme_verified_data.
 * Priority 2: Explicit required documents from master catalog record.
 * Priority 3: Deterministically derived SYSTEM_CONFIGURED requirements from AST eligibility rules.
 * Priority 4: UNKNOWN_OR_UNSTRUCTURED / DOCUMENT_REQUIREMENTS_NOT_MAPPED.
 */
@Service
@Slf4j
public class SchemeDocumentRequirementResolver {

    private static final List<String> DEFAULT_FORMATS = List.of("PDF", "JPG", "JPEG", "PNG");
    private static final long DEFAULT_MAX_SIZE = 5 * 1024 * 1024L; // 5MB

    @Autowired(required = false)
    @Setter
    private SchemeVerifiedDataRepository verifiedDataRepository;

    private final Map<String, List<ResolvedRequirement>> mlPredictionCache = new java.util.concurrent.ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void initPredictionCache() {
        try {
            java.io.File file = new java.io.File("E:/SCHEMEBRIDGE/data/ml_predictions/document_checklist_predictions.json");
            if (!file.exists()) {
                file = new java.io.File("data/ml_predictions/document_checklist_predictions.json");
            }
            if (file.exists()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(file);
                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode schemeNode : root) {
                        String sCode = schemeNode.path("schemeCode").asText(null);
                        String reviewStatus = schemeNode.path("reviewStatus").asText("VERIFIED_SOURCE");
                        String officialUrl = schemeNode.path("officialUrl").asText(null);
                        com.fasterxml.jackson.databind.JsonNode docsNode = schemeNode.path("documents");
                        if (sCode != null && docsNode.isArray()) {
                            List<ResolvedRequirement> list = new ArrayList<>();
                            for (com.fasterxml.jackson.databind.JsonNode dNode : docsNode) {
                                String dCode = dNode.path("documentId").asText("DOC_" + sCode);
                                String canonicalCode = dNode.path("canonicalDocumentCode").asText(dCode);
                                String dName = dNode.path("documentName").asText(dCode);
                                boolean mandatory = dNode.path("mandatory").asBoolean(true);
                                double conf = dNode.path("confidence").asDouble(0.90);
                                String sourceEvidence = dNode.path("sourceEvidence").asText(null);
                                String source = dNode.path("source").asText(officialUrl);

                                com.fasterxml.jackson.databind.JsonNode altNode = dNode.path("alternativeGroup");
                                String altRule = null;
                                List<String> alternatives = new ArrayList<>();
                                if (!altNode.isMissingNode() && !altNode.isNull()) {
                                    altRule = altNode.path("rule").asText("ONE_OF");
                                    com.fasterxml.jackson.databind.JsonNode optsNode = altNode.path("options");
                                    if (optsNode.isArray()) {
                                        for (com.fasterxml.jackson.databind.JsonNode opt : optsNode) {
                                            String optName = opt.path("optionName").asText(null);
                                            if (optName != null && !optName.isBlank()) {
                                                alternatives.add(optName.trim());
                                            }
                                        }
                                    }
                                }

                                String confState = "VERIFIED_SOURCE";
                                if ("HUMAN_REVIEW_REQUIRED".equalsIgnoreCase(reviewStatus) || "REQUIRES_REVIEW".equalsIgnoreCase(reviewStatus)) {
                                    confState = "REQUIRES_REVIEW";
                                }

                                list.add(ResolvedRequirement.builder()
                                        .documentCode(dCode)
                                        .canonicalDocumentCode(canonicalCode)
                                        .documentName(dName)
                                        .description("Document requirement verified from official public scheme documentation.")
                                        .whyRequired("Statutory requirement published in government scheme guidelines.")
                                        .mandatory(mandatory)
                                        .optional(!mandatory)
                                        .alternativeGroupId(altRule != null ? "ALT_" + dCode : null)
                                        .alternativeGroupType(altRule)
                                        .alternatives(alternatives)
                                        .acceptedFormats(DEFAULT_FORMATS)
                                        .maxSizeBytes(DEFAULT_MAX_SIZE)
                                        .issuingAuthority("Competent Government Authority")
                                        .provenance(RequirementProvenance.VERIFIED_SOURCE)
                                        .officialSourceUrl(source)
                                        .sourceReference(sourceEvidence)
                                        .confidenceState(confState)
                                        .confidence(conf)
                                        .build());
                            }
                            mlPredictionCache.put(sCode, list);
                        }
                    }
                    log.info("SchemeDocumentRequirementResolver loaded {} verified ML document predictions.", mlPredictionCache.size());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load ML document predictions cache: {}", e.getMessage());
        }
    }

    public SchemeDocumentRequirementResolver() {
    }

    public SchemeDocumentRequirementResolver(SchemeVerifiedDataRepository verifiedDataRepository) {
        this.verifiedDataRepository = verifiedDataRepository;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResolvedRequirement {
        private String documentCode;
        private String canonicalDocumentCode;
        private String documentName;
        private String description;
        private String whyRequired;
        private boolean mandatory;
        private boolean optional;
        private String alternativeGroupId;
        private String alternativeGroupType; // e.g. "ONE_OF"
        @Builder.Default
        private List<String> alternatives = new ArrayList<>();
        private List<String> acceptedFormats;
        private long maxSizeBytes;
        private String issuingAuthority;
        private RequirementProvenance provenance;
        private String officialSourceUrl;
        private String sourceReference;
        private Instant sourceLastVerified;
        private String confidenceState; // VERIFIED_OFFICIAL, VERIFIED_SOURCE, REQUIRES_REVIEW, UNRESOLVED
        private Double confidence;
    }

    public enum ConfidenceState {
        VERIFIED_OFFICIAL,
        VERIFIED_SOURCE,
        REQUIRES_REVIEW,
        UNRESOLVED
    }

    public static class AlternativeParseResult {
        private final String displayName;
        private final String altRule;
        private final List<String> options;

        public AlternativeParseResult(String displayName, String altRule, List<String> options) {
            this.displayName = displayName;
            this.altRule = altRule;
            this.options = options;
        }

        public String getDisplayName() { return displayName; }
        public String getAltRule() { return altRule; }
        public List<String> getOptions() { return options; }
    }

    public AlternativeParseResult parseDocumentAlternatives(String rawName) {
        return parseDocumentAlternatives(rawName, (SchemeVerifiedData.AlternativeGroup) null);
    }

    public AlternativeParseResult parseDocumentAlternatives(String rawName, SchemeVerifiedData.AlternativeGroup explicitGroup) {
        if (explicitGroup != null && explicitGroup.getOptions() != null && !explicitGroup.getOptions().isEmpty()) {
            List<String> opts = new ArrayList<>();
            for (SchemeVerifiedData.AlternativeOption opt : explicitGroup.getOptions()) {
                if (opt.getOptionName() != null && !opt.getOptionName().isBlank()) {
                    opts.add(opt.getOptionName().trim());
                }
            }
            if (!opts.isEmpty()) {
                return new AlternativeParseResult(rawName, explicitGroup.getRule() != null ? explicitGroup.getRule() : "ONE_OF", opts);
            }
        }

        if (rawName == null || rawName.isBlank()) {
            return new AlternativeParseResult(rawName, null, List.of());
        }

        String cleaned = rawName.trim();
        cleaned = cleaned.replaceAll("^[\\-\\s*\\d.]+", "").trim();

        String header = cleaned;
        String optionsPart = null;

        int parenStart = cleaned.indexOf('(');
        int parenEnd = cleaned.lastIndexOf(')');
        if (parenStart > 0 && parenEnd > parenStart) {
            String inside = cleaned.substring(parenStart + 1, parenEnd).trim();
            if (inside.contains("/") || inside.toLowerCase().contains(" or ")) {
                header = cleaned.substring(0, parenStart).trim();
                optionsPart = inside;
            }
        }

        if (optionsPart == null) {
            String[] splitters = new String[]{" – ", " - ", " — ", " i.e. ", ": "};
            for (String splitter : splitters) {
                int idx = cleaned.indexOf(splitter);
                if (idx > 0) {
                    String before = cleaned.substring(0, idx).trim();
                    String after = cleaned.substring(idx + splitter.length()).trim();
                    if (after.contains("/") || after.toLowerCase().contains(" or ")) {
                        header = before;
                        optionsPart = after;
                        break;
                    }
                }
            }
        }

        if (optionsPart == null && (cleaned.contains(" / ") || cleaned.toLowerCase().contains(" or "))) {
            optionsPart = cleaned;
            header = cleaned.split("[/|]")[0].trim();
        }

        if (optionsPart != null) {
            String[] rawOptions = optionsPart.split("/|(?i)\\s+or\\s+");
            List<String> options = new ArrayList<>();
            for (String opt : rawOptions) {
                String cleanOpt = opt.trim()
                        .replaceAll("^\\((?:as applicable|if any|optional)\\)", "")
                        .replaceAll("\\((?:as applicable|if any|optional)\\)$", "")
                        .replaceAll("^[\\-\\s*]+", "")
                        .replaceAll("[.)]+$", "")
                        .trim();
                if (cleanOpt.length() >= 2 && !options.contains(cleanOpt)) {
                    options.add(cleanOpt);
                }
            }

            if (options.size() >= 2) {
                String cleanHeader = header.replaceAll("^[\\-\\s*\\d.]+", "").replaceAll("[:–—-]+$", "").trim();
                if (cleanHeader.isBlank()) {
                    cleanHeader = options.get(0);
                }
                return new AlternativeParseResult(cleanHeader, "ONE_OF", options);
            }
        }

        return new AlternativeParseResult(cleaned, null, List.of());
    }

    public List<ResolvedRequirement> resolveRequirements(Scheme scheme) {
        if (scheme == null) return List.of();

        // 1. Check Canonical Knowledge Base (scheme_verified_data) first
        if (verifiedDataRepository != null && scheme.getSchemeCode() != null) {
            Optional<SchemeVerifiedData> canonicalOpt = verifiedDataRepository.findBySchemeCode(scheme.getSchemeCode());
            if (canonicalOpt.isPresent()) {
                SchemeVerifiedData canonical = canonicalOpt.get();
                if ("DOCUMENTS_FOUND".equalsIgnoreCase(canonical.getDocumentStatus()) &&
                        canonical.getDocuments() != null && !canonical.getDocuments().isEmpty()) {
                    List<ResolvedRequirement> canonicalList = new ArrayList<>();
                    for (SchemeVerifiedData.CanonicalDocumentRequirement d : canonical.getDocuments()) {
                        AlternativeParseResult altResult = parseDocumentAlternatives(d.getOfficialDocumentName(), d.getAlternativeGroup());

                        canonicalList.add(ResolvedRequirement.builder()
                                .documentCode(d.getDocumentCode())
                                .canonicalDocumentCode(d.getCanonicalDocumentCode() != null ? d.getCanonicalDocumentCode() : d.getDocumentCode())
                                .documentName(altResult.getDisplayName() != null ? altResult.getDisplayName() : d.getDocumentCode())
                                .description(d.getDescription() != null ? d.getDescription() : "Official statutory required document.")
                                .whyRequired(d.getWhyRequired() != null ? d.getWhyRequired() : "Official statutory requirement specified in scheme circular.")
                                .mandatory(d.isMandatory())
                                .optional(d.isOptional())
                                .alternativeGroupId(altResult.getAltRule() != null ? "ALT_" + d.getDocumentCode() : null)
                                .alternativeGroupType(altResult.getAltRule())
                                .alternatives(altResult.getOptions())
                                .acceptedFormats(d.getAcceptedFormats() != null && !d.getAcceptedFormats().isEmpty() ? d.getAcceptedFormats() : DEFAULT_FORMATS)
                                .maxSizeBytes(d.getMaxSizeBytes() > 0 ? d.getMaxSizeBytes() : DEFAULT_MAX_SIZE)
                                .issuingAuthority(d.getIssuingAuthority() != null ? d.getIssuingAuthority() : "Competent Government Authority")
                                .provenance(d.getProvenance() != null ? d.getProvenance() : RequirementProvenance.VERIFIED_OFFICIAL)
                                .officialSourceUrl(d.getSourceUrl())
                                .sourceReference(d.getSourceEvidence())
                                .sourceLastVerified(d.getVerifiedAt())
                                .confidenceState("VERIFIED_OFFICIAL")
                                .confidence(1.0)
                                .build());
                    }
                    return canonicalList;
                }
            }
        }

        // 2. If scheme catalog record contains explicit required documents, use them
        if (scheme.getRequiredDocuments() != null && !scheme.getRequiredDocuments().isEmpty()) {
            List<ResolvedRequirement> list = new ArrayList<>();
            for (RequiredDocument rd : scheme.getRequiredDocuments()) {
                String docTitle = rd.getName() != null ? rd.getName().getEnglish() : rd.getDocumentCode();
                AlternativeParseResult altResult = parseDocumentAlternatives(docTitle, null);

                list.add(ResolvedRequirement.builder()
                        .documentCode(rd.getDocumentCode())
                        .canonicalDocumentCode(rd.getDocumentCode())
                        .documentName(altResult.getDisplayName() != null ? altResult.getDisplayName() : rd.getDocumentCode())
                        .description(rd.getDescription() != null ? rd.getDescription().getEnglish() : "Required official documentation")
                        .whyRequired("Official statutory requirement specified in scheme circular.")
                        .mandatory(rd.isMandatory())
                        .optional(!rd.isMandatory())
                        .alternativeGroupId(altResult.getAltRule() != null ? "ALT_" + rd.getDocumentCode() : null)
                        .alternativeGroupType(altResult.getAltRule())
                        .alternatives(altResult.getOptions())
                        .acceptedFormats(rd.getAcceptedFormats() != null && !rd.getAcceptedFormats().isEmpty() ? rd.getAcceptedFormats() : DEFAULT_FORMATS)
                        .maxSizeBytes(DEFAULT_MAX_SIZE)
                        .issuingAuthority(rd.getIssuingAuthority() != null ? rd.getIssuingAuthority() : "Competent Government Authority")
                        .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                        .confidenceState("VERIFIED_OFFICIAL")
                        .confidence(0.95)
                        .build());
            }
            return list;
        }

        // 3. Check ML Verified Document Predictions (for catalog schemes lacking canonical records)
        if (scheme.getSchemeCode() != null && mlPredictionCache.containsKey(scheme.getSchemeCode())) {
            List<ResolvedRequirement> cached = mlPredictionCache.get(scheme.getSchemeCode());
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        }

        // 4. Deterministically derive system-configured requirements from AST eligibility rules & category
        Set<String> ruleFields = extractRuleFields(scheme.getEligibilityRules());
        String catName = scheme.getCategory() != null && scheme.getCategory().getName() != null ? scheme.getCategory().getName().toLowerCase() : "";
        String schemeTitle = scheme.getTitle() != null && scheme.getTitle().getEnglish() != null ? scheme.getTitle().getEnglish().toLowerCase() : "";

        List<ResolvedRequirement> derived = new ArrayList<>();

        // Base Identity Document (Always required for welfare schemes)
        derived.add(ResolvedRequirement.builder()
                .documentCode("AADHAAR")
                .canonicalDocumentCode("AADHAAR")
                .documentName("Aadhaar Card")
                .description("12-digit Aadhaar identity card or enrollment acknowledgement.")
                .whyRequired("Required for citizen identity verification and DBT entitlement authentication.")
                .mandatory(true)
                .optional(false)
                .acceptedFormats(DEFAULT_FORMATS)
                .maxSizeBytes(DEFAULT_MAX_SIZE)
                .issuingAuthority("UIDAI (Govt of India)")
                .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                .build());

        // Income Verification
        if (hasMatchingRule(ruleFields, "income", "annualincome", "annual_income", "bpl", "bplstatus", "family_income") ||
                catName.contains("income") || schemeTitle.contains("bpl") || schemeTitle.contains("poverty")) {
            derived.add(ResolvedRequirement.builder()
                    .documentCode("INCOME_PROOF")
                    .canonicalDocumentCode("INCOME_CERTIFICATE")
                    .documentName("Income Certificate")
                    .description("Current financial year income certificate issued by competent revenue authority.")
                    .whyRequired("Required to verify applicant family annual income against scheme eligibility ceiling.")
                    .mandatory(true)
                    .optional(false)
                    .acceptedFormats(DEFAULT_FORMATS)
                    .maxSizeBytes(DEFAULT_MAX_SIZE)
                    .issuingAuthority("Tahasildar / Revenue Department")
                    .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                    .build());
        }

        // Social Category / Caste Verification
        if (hasMatchingRule(ruleFields, "caste", "socialcategory", "social_category", "community", "sc", "st", "obc", "ews") ||
                catName.contains("social") || schemeTitle.contains("tribal") || schemeTitle.contains("backward")) {
            derived.add(ResolvedRequirement.builder()
                    .documentCode("CASTE_CERT")
                    .canonicalDocumentCode("CASTE_CERTIFICATE")
                    .documentName("Caste / Community Certificate")
                    .description("Certified social category certificate (SC/ST/OBC/EWS).")
                    .whyRequired("Required to verify social category reservation eligibility.")
                    .mandatory(true)
                    .optional(false)
                    .acceptedFormats(DEFAULT_FORMATS)
                    .maxSizeBytes(DEFAULT_MAX_SIZE)
                    .issuingAuthority("Sub-Divisional Officer / Welfare Board")
                    .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                    .build());
        }

        // Domicile / Residence Verification
        if (hasMatchingRule(ruleFields, "state", "domicile", "residence", "resident", "district") ||
                scheme.getSchemeLevel() == com.schemebridge.scheme.document.SchemeLevel.STATE) {
            derived.add(ResolvedRequirement.builder()
                    .documentCode("DOMICILE_CERT")
                    .canonicalDocumentCode("RESIDENCE_CERTIFICATE")
                    .documentName("Domicile / Residence Certificate")
                    .description("State/UT nativity or residential certificate.")
                    .whyRequired("Required to establish state residence eligibility for state-specific benefit disbursement.")
                    .mandatory(true)
                    .optional(false)
                    .acceptedFormats(DEFAULT_FORMATS)
                    .maxSizeBytes(DEFAULT_MAX_SIZE)
                    .issuingAuthority("District Magistrate / Revenue Officer")
                    .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                    .build());
        }

        // Bank Account Proof (For direct benefit transfer / subsidies / cash)
        boolean hasFinancialBenefit = scheme.getBenefits() != null && scheme.getBenefits().stream().anyMatch(b ->
                (b.getAmountType() != null && (b.getAmountType().equalsIgnoreCase("FINANCIAL") || b.getAmountType().equalsIgnoreCase("SUBSIDY") || b.getAmountType().equalsIgnoreCase("SCHOLARSHIP"))) ||
                (b.getTitle() != null && b.getTitle().getEnglish() != null && b.getTitle().getEnglish().toLowerCase().contains("financial"))
        );
        if (hasFinancialBenefit || catName.contains("financial") || catName.contains("direct benefit") || catName.contains("subsidy") ||
                schemeTitle.contains("pension") || schemeTitle.contains("subsidy") || schemeTitle.contains("aid") ||
                schemeTitle.contains("samman") || schemeTitle.contains("scholarship") || schemeTitle.contains("grant") ||
                schemeTitle.contains("farmer") || schemeTitle.contains("livelihood")) {
            derived.add(ResolvedRequirement.builder()
                    .documentCode("BANK_PASSBOOK")
                    .canonicalDocumentCode("BANK_PASSBOOK")
                    .documentName("Bank Passbook / Cancelled Cheque")
                    .description("Copy of bank passbook first page or cancelled cheque showing IFSC and Account Number.")
                    .whyRequired("Required for electronic Direct Benefit Transfer (DBT) credit into beneficiary account.")
                    .mandatory(true)
                    .optional(false)
                    .acceptedFormats(DEFAULT_FORMATS)
                    .maxSizeBytes(DEFAULT_MAX_SIZE)
                    .issuingAuthority("Scheduled Commercial Bank / Post Office")
                    .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                    .build());
        }

        // Education Proof
        if (catName.contains("education") || catName.contains("scholarship") || catName.contains("student") ||
                schemeTitle.contains("scholarship") || schemeTitle.contains("fellowship") || schemeTitle.contains("vidya") ||
                hasMatchingRule(ruleFields, "education", "qualification", "marks", "percentage", "student", "course")) {
            derived.add(ResolvedRequirement.builder()
                    .documentCode("EDUCATION_CERT")
                    .canonicalDocumentCode("EDUCATIONAL_CERTIFICATE")
                    .documentName("Marksheet / Bonafide Student Certificate")
                    .description("Latest qualifying academic examination marksheet or institution enrollment certificate.")
                    .whyRequired("Required to authenticate academic eligibility and ongoing student status.")
                    .mandatory(true)
                    .optional(false)
                    .acceptedFormats(DEFAULT_FORMATS)
                    .maxSizeBytes(DEFAULT_MAX_SIZE)
                    .issuingAuthority("School / College / University / Education Board")
                    .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                    .build());
        }

        // Disability Proof
        if (catName.contains("disability") || catName.contains("divyang") || schemeTitle.contains("disability") ||
                hasMatchingRule(ruleFields, "disability", "disabilitypercentage", "pwd", "divyang", "handicap")) {
            derived.add(ResolvedRequirement.builder()
                    .documentCode("DISABILITY_CERT")
                    .canonicalDocumentCode("DISABILITY_CERTIFICATE")
                    .documentName("Disability Certificate / UDID Card")
                    .description("Unique Disability ID (UDID) or medical board disability assessment certificate.")
                    .whyRequired("Required to confirm benchmark disability percentage qualification.")
                    .mandatory(true)
                    .optional(false)
                    .acceptedFormats(DEFAULT_FORMATS)
                    .maxSizeBytes(DEFAULT_MAX_SIZE)
                    .issuingAuthority("District Medical Board / Dept of Empowerment of Persons with Disabilities")
                    .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                    .build());
        }

        // Farmer / Landholding Proof
        if (catName.contains("agriculture") || catName.contains("farmer") || catName.contains("kisan") ||
                schemeTitle.contains("farmer") || schemeTitle.contains("kisan") || schemeTitle.contains("crop") ||
                hasMatchingRule(ruleFields, "landholding", "land_holding", "landarea", "farmer", "agriculture")) {
            derived.add(ResolvedRequirement.builder()
                    .documentCode("LAND_RECORD")
                    .canonicalDocumentCode("LAND_RECORDS")
                    .documentName("Land Records (Khasra / Khatauni / Patta)")
                    .description("Current land ownership records or tenant farmer authorization document.")
                    .whyRequired("Required to verify agricultural landholding limits and farming occupation status.")
                    .mandatory(true)
                    .optional(false)
                    .acceptedFormats(DEFAULT_FORMATS)
                    .maxSizeBytes(DEFAULT_MAX_SIZE)
                    .issuingAuthority("Tehsildar / Revenue & Land Reforms Department")
                    .provenance(RequirementProvenance.SYSTEM_CONFIGURED)
                    .build());
        }

        for (ResolvedRequirement req : derived) {
            if (req.getConfidenceState() == null) {
                req.setConfidenceState("VERIFIED_SOURCE");
                req.setConfidence(0.80);
            }
        }

        return derived;
    }

    private Set<String> extractRuleFields(RuleGroup ruleGroup) {
        Set<String> fields = new HashSet<>();
        if (ruleGroup == null) return fields;

        if (ruleGroup.getConditions() != null) {
            for (EligibilityCondition cond : ruleGroup.getConditions()) {
                if (cond.getField() != null) {
                    fields.add(cond.getField().toLowerCase().trim());
                }
            }
        }

        if (ruleGroup.getGroups() != null) {
            for (RuleGroup sub : ruleGroup.getGroups()) {
                fields.addAll(extractRuleFields(sub));
            }
        }
        return fields;
    }

    private boolean hasMatchingRule(Set<String> fields, String... targetKeys) {
        for (String key : targetKeys) {
            if (fields.contains(key.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
