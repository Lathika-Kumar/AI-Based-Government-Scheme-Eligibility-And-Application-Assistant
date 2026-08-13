package com.schemebridge.coreservice.citizen.service;

import com.schemebridge.coreservice.citizen.client.EligibilityServiceClient;
import com.schemebridge.coreservice.citizen.model.*;
import com.schemebridge.coreservice.citizen.dto.CitizenDashboardResponse;
import com.schemebridge.coreservice.citizen.dto.CitizenProfileRequest;
import com.schemebridge.coreservice.citizen.dto.CitizenProfileResponse;
import com.schemebridge.coreservice.citizen.repository.CitizenRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CitizenService {

    private final CitizenRepository citizenRepository;
    private final ProfileCompletionService profileCompletionService;
    private final EligibilityServiceClient eligibilityServiceClient;

    public CitizenService(CitizenRepository citizenRepository,
                          ProfileCompletionService profileCompletionService,
                          EligibilityServiceClient eligibilityServiceClient) {
        this.citizenRepository = citizenRepository;
        this.profileCompletionService = profileCompletionService;
        this.eligibilityServiceClient = eligibilityServiceClient;
    }

    public CitizenProfileResponse createOrUpdateProfile(String authUserId, CitizenProfileRequest request, String clientIp, String device) {
        Optional<CitizenDocument> existingOpt = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId);
        CitizenDocument doc;
        boolean isNew = false;

        if (existingOpt.isPresent()) {
            doc = existingOpt.get();
        } else {
            doc = new CitizenDocument();
            doc.setId(UUID.randomUUID().toString());
            doc.setAuthUserId(authUserId);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setCreatedBy(authUserId);
            doc.setActive(true);
            doc.setDeleted(false);
            isNew = true;
        }

        if (request.getPersonalDetails() != null) doc.setPersonalDetails(request.getPersonalDetails());
        if (request.getContactDetails() != null) doc.setContactDetails(request.getContactDetails());
        if (request.getAddressDetails() != null) doc.setAddressDetails(request.getAddressDetails());
        if (request.getFamilyDetails() != null) doc.setFamilyDetails(request.getFamilyDetails());
        if (request.getIncomeDetails() != null) doc.setIncomeDetails(request.getIncomeDetails());
        if (request.getEducationDetails() != null) doc.setEducationDetails(request.getEducationDetails());
        if (request.getOccupationDetails() != null) doc.setOccupationDetails(request.getOccupationDetails());
        if (request.getSpecialCategoryDetails() != null) doc.setSpecialCategoryDetails(request.getSpecialCategoryDetails());
        if (request.getPreferences() != null) doc.setPreferences(request.getPreferences());

        doc.setUpdatedAt(LocalDateTime.now());
        doc.setUpdatedBy(authUserId);

        // Recalculate completion
        ProfileCompletion completion = profileCompletionService.calculateCompletion(doc);
        doc.setProfileCompletion(completion);

        // Recalculate eligibility snapshot using EligibilityServiceClient
        EligibilitySnapshot eligibility = eligibilityServiceClient.evaluateEligibilityFromSchemeService(doc);
        doc.setEligibilitySnapshot(eligibility);

        // Initialize document readiness if missing
        if (doc.getDocumentReadiness() == null) {
            doc.setDocumentReadiness(DocumentReadiness.builder()
                    .uploadedDocumentCount(0)
                    .requiredDocumentCount(5)
                    .verifiedDocuments(0)
                    .pendingDocuments(0)
                    .missingDocuments(5)
                    .readinessPercentage(0.0)
                    .build());
        } else {
            // Ensure non-null fields for existing documents
            DocumentReadiness dr = doc.getDocumentReadiness();
            if (dr.getVerifiedDocuments() == null) dr.setVerifiedDocuments(0);
            if (dr.getPendingDocuments() == null) dr.setPendingDocuments(0);
            if (dr.getMissingDocuments() == null) dr.setMissingDocuments(dr.getRequiredDocumentCount() != null ? dr.getRequiredDocumentCount() : 5);
        }


        doc.setDashboardLastCalculated(LocalDateTime.now());
        doc.setDashboardVersion(doc.getDashboardVersion() != null ? doc.getDashboardVersion() + 1 : 1L);

        // Record activity history event with severity
        String action = isNew ? "PROFILE_CREATED" : (completion.getIsComplete() ? "PROFILE_COMPLETED" : "PROFILE_UPDATED");
        String desc = isNew ? "Citizen profile created" : "Citizen demographic details updated";
        String severity = isNew ? "IMPORTANT" : "NORMAL";
        addActivity(doc, action, desc, severity, authUserId, clientIp, device);

        CitizenDocument saved = citizenRepository.save(doc);
        return mapToResponse(saved);
    }

    public CitizenProfileResponse getProfileByAuthUserId(String authUserId) {
        CitizenDocument doc = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen Profile", "authUserId", authUserId));
        return mapToResponse(doc);
    }

    public CitizenDashboardResponse getDashboardSummary(String authUserId) {
        CitizenDocument doc = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId)
                .orElseGet(() -> createEmptyDefaultDoc(authUserId));

        int savedCount = doc.getSavedSchemes() != null ? doc.getSavedSchemes().size() : 0;
        int matchedCount = doc.getEligibilitySnapshot() != null ? doc.getEligibilitySnapshot().getMatchedSchemes() : 0;
        double eligibilityScore = doc.getEligibilitySnapshot() != null ? doc.getEligibilitySnapshot().getEligibilityScore() : 0.0;

        List<ActivityHistoryItem> recent = doc.getActivityHistory() != null ?
                doc.getActivityHistory().stream().sorted(Comparator.comparing(ActivityHistoryItem::getPerformedAt).reversed()).limit(5).collect(Collectors.toList())
                : new ArrayList<>();

        return CitizenDashboardResponse.builder()
                .profileCompletion(doc.getProfileCompletion())
                .eligibilityScore(eligibilityScore)
                .savedSchemesCount(savedCount)
                .matchedSchemes(matchedCount)
                .documentReadiness(doc.getDocumentReadiness())
                .recentActivities(recent)
                .notificationsCount(3)
                .build();
    }

    public void saveScheme(String authUserId, String schemeId, String source) {
        CitizenDocument doc = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId)
                .orElseGet(() -> createEmptyDefaultDoc(authUserId));

        if (doc.getSavedSchemes() == null) doc.setSavedSchemes(new HashSet<>());
        if (doc.getSavedSchemeObjects() == null) doc.setSavedSchemeObjects(new ArrayList<>());

        doc.getSavedSchemes().add(schemeId);

        SavedScheme savedScheme = SavedScheme.builder()
                .schemeId(schemeId)
                .savedAt(LocalDateTime.now())
                .favorite(true)
                .source(source != null ? source : "DIRECT_SEARCH")
                .build();

        doc.getSavedSchemeObjects().removeIf(s -> s.getSchemeId().equals(schemeId));
        doc.getSavedSchemeObjects().add(savedScheme);

        addActivity(doc, "SCHEME_SAVED", "Saved scheme to vault: " + schemeId, "NORMAL", authUserId, "127.0.0.1", "Web Client");
        citizenRepository.save(doc);
    }

    public void unsaveScheme(String authUserId, String schemeId) {
        CitizenDocument doc = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen Profile", "authUserId", authUserId));

        if (doc.getSavedSchemes() != null) {
            doc.getSavedSchemes().remove(schemeId);
        }
        if (doc.getSavedSchemeObjects() != null) {
            doc.getSavedSchemeObjects().removeIf(s -> s.getSchemeId().equals(schemeId));
        }

        addActivity(doc, "SCHEME_UNSAVED", "Removed scheme from saved list: " + schemeId, "NORMAL", authUserId, "127.0.0.1", "Web Client");
        citizenRepository.save(doc);
    }

    public Set<String> getSavedSchemes(String authUserId) {
        CitizenDocument doc = citizenRepository.findByAuthUserIdAndDeletedFalse(authUserId)
                .orElseGet(() -> createEmptyDefaultDoc(authUserId));
        return doc.getSavedSchemes() != null ? doc.getSavedSchemes() : Collections.emptySet();
    }

    public List<CitizenProfileResponse> searchCitizens(String state, String district, String category, String occupationType,
                                                       Boolean farmer, Boolean minority, Boolean disability) {
        List<CitizenDocument> list = citizenRepository.searchCitizens(state, district, category, occupationType, farmer, minority, disability);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private CitizenDocument createEmptyDefaultDoc(String authUserId) {
        CitizenDocument doc = new CitizenDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setAuthUserId(authUserId);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setCreatedBy(authUserId);
        doc.setActive(true);
        doc.setDeleted(false);
        doc.setSavedSchemes(new HashSet<>());
        doc.setSavedSchemeObjects(new ArrayList<>());
        doc.setProfileCompletion(profileCompletionService.calculateCompletion(doc));
        doc.setEligibilitySnapshot(eligibilityServiceClient.evaluateEligibilityFromSchemeService(doc));
        doc.setDocumentReadiness(DocumentReadiness.builder().uploadedDocumentCount(0).requiredDocumentCount(5).verifiedDocuments(0).pendingDocuments(0).missingDocuments(5).readinessPercentage(0.0).build());
        doc.setDashboardLastCalculated(LocalDateTime.now());
        doc.setDashboardVersion(1L);
        return citizenRepository.save(doc);
    }

    private void addActivity(CitizenDocument doc, String action, String description, String severity, String performedBy, String ip, String device) {
        if (doc.getActivityHistory() == null) {
            doc.setActivityHistory(new ArrayList<>());
        }
        ActivityHistoryItem item = ActivityHistoryItem.builder()
                .action(action)
                .description(description)
                .severity(severity != null ? severity : "NORMAL")
                .performedAt(LocalDateTime.now())
                .performedBy(performedBy)
                .ipAddress(ip)
                .device(device)
                .build();
        doc.getActivityHistory().add(item);
    }

    private CitizenProfileResponse mapToResponse(CitizenDocument doc) {
        return CitizenProfileResponse.builder()
                .id(doc.getId())
                .authUserId(doc.getAuthUserId())
                .personalDetails(doc.getPersonalDetails())
                .contactDetails(doc.getContactDetails())
                .addressDetails(doc.getAddressDetails())
                .familyDetails(doc.getFamilyDetails())
                .incomeDetails(doc.getIncomeDetails())
                .educationDetails(doc.getEducationDetails())
                .occupationDetails(doc.getOccupationDetails())
                .specialCategoryDetails(doc.getSpecialCategoryDetails())
                .preferences(doc.getPreferences())
                .eligibilitySnapshot(doc.getEligibilitySnapshot())
                .profileCompletion(doc.getProfileCompletion())
                .documentReadiness(doc.getDocumentReadiness())
                .savedSchemes(doc.getSavedSchemes())
                .activityHistory(doc.getActivityHistory())
                .active(doc.getActive())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
