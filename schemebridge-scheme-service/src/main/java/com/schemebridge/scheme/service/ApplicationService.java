package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.exception.DuplicateResourceException;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.Setter
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final SchemeRepository schemeRepository;
    private final EligibilityEngine eligibilityEngine;
    private final DocumentStorageService documentStorageService;
    private final MongoOperations mongoOperations;
    private final ApplicationEventService applicationEventService;
    private final ApplicationStatusTransitionService applicationStatusTransitionService;
    private final DetailedDocumentStatusTransitionService detailedDocumentStatusTransitionService;
    private final NotificationService notificationService;
    private final CitizenProfileService citizenProfileService;

    @Autowired(required = false)
    private SchemeDocumentRequirementResolver schemeDocumentRequirementResolver;

    @Autowired(required = false)
    private com.schemebridge.scheme.repository.SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Autowired(required = false)
    private DocumentChecklistGenerator documentChecklistGenerator;

    private String generateApplicationNumber() {
        Query query = new Query(Criteria.where("_id").is("application_sequence"));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);
        DatabaseSequence seq = mongoOperations.findAndModify(query, update, options, DatabaseSequence.class);
        long sequenceNumber = seq != null ? seq.getSeq() : 1;
        return String.format("SB-APP-2026-%06d", sequenceNumber);
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request, String userId) {
        // 1. Verify scheme exists
        Scheme scheme = schemeRepository.findBySchemeCode(request.getSchemeCode())
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with code: " + request.getSchemeCode()));

        // 2. Verify scheme is ACTIVE
        if (scheme.getStatus() != SchemeStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot apply for an inactive scheme.");
        }

        // 3. Verify duplicate application
        List<ApplicationStatus> activeStatuses = List.of(
                ApplicationStatus.DRAFT,
                ApplicationStatus.DOCUMENTS_PENDING,
                ApplicationStatus.READY_FOR_SUBMISSION,
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.UNDER_REVIEW
        );
        if (applicationRepository.findByUserIdAndSchemeCodeAndStatusIn(userId, request.getSchemeCode(), activeStatuses).isPresent()) {
            throw new DuplicateResourceException("An active application already exists for this scheme.");
        }

        // 4. Resolve eligibility profile
        CitizenEligibilityProfile evalProfile = request.getProfile();
        if (evalProfile == null) {
            CitizenProfile persistedProfile = citizenProfileService.findProfile(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Please complete your profile before applying."));
            if (!Boolean.TRUE.equals(persistedProfile.getOnboardingComplete())) {
                throw new IllegalArgumentException("Please complete your profile before applying.");
            }
            evalProfile = citizenProfileService.toCitizenEligibilityProfile(persistedProfile);
        }

        // 5. Verify eligibility
        EligibilityEvaluationResponse evalRes = eligibilityEngine.evaluateScheme(scheme, evalProfile);
        if (evalRes.getStatus() == EvaluationStatus.NOT_ELIGIBLE) {
            throw new IllegalArgumentException("Applicant is not eligible for this scheme: " + evalRes.getFailedConditions());
        } else if (evalRes.getStatus() == EvaluationStatus.INDETERMINATE) {
            throw new IllegalArgumentException("Applicant eligibility is indeterminate. Missing required attributes: " + evalRes.getMissingInformation());
        }

        // 6. Generate application number
        String appNumber = generateApplicationNumber();

        // 7. Create application record
        Application app = Application.builder()
                .applicationNumber(appNumber)
                .userId(userId)
                .schemeId(scheme.getId())
                .schemeCode(scheme.getSchemeCode())
                .status(ApplicationStatus.DOCUMENTS_PENDING)
                .build();

        Application savedApp = applicationRepository.save(app);

        // Record APPLICATION_CREATED event
        applicationEventService.recordEvent(
                savedApp.getId(),
                userId,
                ApplicationEventType.APPLICATION_CREATED,
                null,
                ApplicationStatus.DOCUMENTS_PENDING,
                "Application record created.",
                Map.of()
        );

        // 8. Copy required documents
        List<SchemeDocumentRequirementResolver.ResolvedRequirement> resolvedReqs =
                schemeDocumentRequirementResolver != null ?
                        schemeDocumentRequirementResolver.resolveRequirements(scheme) :
                        (scheme.getRequiredDocuments() != null ?
                                scheme.getRequiredDocuments().stream().map(rd -> SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                                        .documentCode(rd.getDocumentCode())
                                        .documentName(rd.getName() != null ? rd.getName().getEnglish() : rd.getDocumentCode())
                                        .mandatory(rd.isMandatory())
                                        .build()).collect(Collectors.toList()) : List.of());

        List<ApplicationDocument> appDocs = new ArrayList<>();
        for (SchemeDocumentRequirementResolver.ResolvedRequirement rd : resolvedReqs) {
            ApplicationDocument ad = ApplicationDocument.builder()
                    .applicationId(savedApp.getId())
                    .userId(userId)
                    .schemeCode(scheme.getSchemeCode())
                    .documentCode(rd.getDocumentCode())
                    .documentName(rd.getDocumentName())
                    .mandatory(rd.isMandatory())
                    .uploaded(false)
                    .version(1)
                    .detailedStatus(DetailedDocumentStatus.NOT_UPLOADED)
                    .verificationStatus(DocumentVerificationStatus.PENDING)
                    .versionHistory(new ArrayList<>())
                    .build();
            appDocs.add(ad);
        }

        if (!appDocs.isEmpty()) {
            applicationDocumentRepository.saveAll(appDocs);
        }

        // 9. Update initial status if no documents or no mandatory documents
        updateApplicationReadinessStatus(savedApp);

        return mapToResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationDetails(String applicationId, String userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = false;
        if (authentication != null) {
            isPrivileged = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") 
                                   || auth.getAuthority().equals("ROLE_SCHEME_MANAGER")
                                   || auth.getAuthority().equals("ROLE_VERIFICATION_OFFICER"));
        }

        if (!app.getUserId().equals(userId) && !isPrivileged) {
            throw new SecurityException("You do not have permission to access this application.");
        }

        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String userId) {
        return applicationRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationDocumentResponse> getApplicationDocuments(String applicationId, String userId, boolean isPrivileged) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!app.getUserId().equals(userId) && !isPrivileged) {
            throw new SecurityException("You do not have permission to access documents for this application.");
        }

        return applicationDocumentRepository.findAllByApplicationId(applicationId).stream()
                .map(this::mapToDocResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationDocumentResponse getApplicationDocument(String applicationId, String documentCode, String userId, boolean isPrivileged) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!app.getUserId().equals(userId) && !isPrivileged) {
            throw new SecurityException("You do not have permission to access this document.");
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(applicationId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with code: " + documentCode));

        return mapToDocResponse(doc);
    }

    public DocumentDownloadDto getDocumentDownload(String applicationId, String documentCode, String userId, boolean isPrivileged) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!app.getUserId().equals(userId) && !isPrivileged) {
            throw new SecurityException("You do not have permission to download documents for this application.");
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(applicationId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with code: " + documentCode));

        if (!doc.isUploaded() || doc.getStorageReference() == null) {
            throw new ResourceNotFoundException("Document is not yet uploaded.");
        }

        InputStream stream = documentStorageService.retrieve(doc.getStorageReference());
        return DocumentDownloadDto.builder()
                .fileName(doc.getFileName())
                .contentType(doc.getContentType() != null ? doc.getContentType() : "application/octet-stream")
                .fileSize(doc.getFileSize())
                .inputStream(stream)
                .build();
    }

    @Transactional
    public ApplicationDocumentResponse uploadDocument(String applicationId, String documentCode, MultipartFile file, String userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!app.getUserId().equals(userId)) {
            throw new SecurityException("You do not have permission to upload documents for this application.");
        }

        if (app.getStatus() != ApplicationStatus.DRAFT &&
            app.getStatus() != ApplicationStatus.DOCUMENTS_PENDING &&
            app.getStatus() != ApplicationStatus.READY_FOR_SUBMISSION &&
            app.getStatus() != ApplicationStatus.CORRECTION_REQUIRED) {
            throw new IllegalStateException("Cannot upload documents in current application state: " + app.getStatus());
        }

        ApplicationDocument appDoc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(applicationId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Required document not found for code: " + documentCode));

        Scheme scheme = schemeRepository.findBySchemeCode(app.getSchemeCode()).orElse(null);

        SchemeDocumentRequirementResolver.ResolvedRequirement reqDoc = null;
        if (schemeDocumentRequirementResolver != null && scheme != null) {
            reqDoc = schemeDocumentRequirementResolver.resolveRequirements(scheme).stream()
                    .filter(rd -> rd.getDocumentCode().equalsIgnoreCase(documentCode))
                    .findFirst()
                    .orElse(null);
        }

        // Size check (max 5MB)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }

        // Format check
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = "";
        int idx = originalFilename.lastIndexOf('.');
        if (idx >= 0) {
            extension = originalFilename.substring(idx + 1).toUpperCase();
        }

        List<String> acceptedFormats = reqDoc != null && reqDoc.getAcceptedFormats() != null && !reqDoc.getAcceptedFormats().isEmpty()
                ? reqDoc.getAcceptedFormats()
                : List.of("PDF", "JPG", "JPEG", "PNG");

        boolean accepted = false;
        for (String format : acceptedFormats) {
            if (format.equalsIgnoreCase(extension)) {
                accepted = true;
                break;
            }
        }

        if (!accepted) {
            throw new IllegalArgumentException("File format " + extension + " not accepted. Allowed: " + acceptedFormats);
        }

        // Store previous version in version history if already uploaded
        if (appDoc.isUploaded()) {
            ApplicationDocumentVersion oldVersion = ApplicationDocumentVersion.builder()
                    .version(appDoc.getVersion() != null ? appDoc.getVersion() : 1)
                    .fileName(appDoc.getFileName())
                    .contentType(appDoc.getContentType())
                    .fileSize(appDoc.getFileSize())
                    .storageReference(appDoc.getStorageReference())
                    .gridFsFileId(appDoc.getGridFsFileId())
                    .status(appDoc.getDetailedStatus() != null ? appDoc.getDetailedStatus() :
                            (appDoc.getVerificationStatus() == DocumentVerificationStatus.REJECTED ? DetailedDocumentStatus.REJECTED : DetailedDocumentStatus.UPLOADED))
                    .verificationStatus(appDoc.getVerificationStatus())
                    .uploadedAt(appDoc.getUploadedAt())
                    .rejectedAt(appDoc.getRejectedAt())
                    .rejectionReason(appDoc.getRejectionReason())
                    .verifiedAt(appDoc.getVerifiedAt())
                    .verifiedBy(appDoc.getVerifiedBy())
                    .build();

            if (appDoc.getVersionHistory() == null) {
                appDoc.setVersionHistory(new ArrayList<>());
            }
            appDoc.getVersionHistory().add(oldVersion);
        }

        // Store new item in GridFS
        String ref = documentStorageService.store(applicationId, documentCode, file);

        // Increment version
        int nextVersion = appDoc.isUploaded() ? (appDoc.getVersion() != null ? appDoc.getVersion() + 1 : 2) : 1;
        appDoc.setVersion(nextVersion);
        appDoc.setSchemeCode(app.getSchemeCode());
        appDoc.setGridFsFileId(ref);
        appDoc.setUploaded(true);
        appDoc.setFileName(originalFilename);
        appDoc.setContentType(file.getContentType());
        appDoc.setFileSize(file.getSize());
        appDoc.setStorageReference(ref);
        appDoc.setUploadedAt(Instant.now());
        appDoc.setRejectionReason(null);
        appDoc.setRejectedAt(null);
        appDoc.setVerifiedAt(null);
        appDoc.setVerifiedBy(null);
        appDoc.setVerificationStatus(DocumentVerificationStatus.PENDING);
        appDoc.setDetailedStatus(DetailedDocumentStatus.UPLOADED);

        // Transition status via state machine
        if (detailedDocumentStatusTransitionService != null) {
            detailedDocumentStatusTransitionService.transitionDocumentStatus(
                    appDoc, DetailedDocumentStatus.UPLOADED, userId, "Uploaded document version " + nextVersion);
        } else {
            applicationDocumentRepository.save(appDoc);
        }

        // Record DOCUMENT_UPLOADED event
        applicationEventService.recordEvent(
                applicationId,
                userId,
                ApplicationEventType.DOCUMENT_UPLOADED,
                app.getStatus(),
                app.getStatus(),
                "Document " + documentCode + " uploaded.",
                Map.of("fileName", originalFilename, "documentCode", documentCode, "version", nextVersion)
        );

        // Update application state
        updateApplicationReadinessStatus(app);

        return mapToDocResponse(appDoc);
    }

    @Transactional
    public ApplicationResponse submitApplication(String applicationId, String userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!app.getUserId().equals(userId)) {
            throw new SecurityException("You do not have permission to submit this application.");
        }

        if (app.getStatus() == ApplicationStatus.CANCELLED ||
            app.getStatus() == ApplicationStatus.APPROVED ||
            app.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Cannot submit application in terminal state: " + app.getStatus());
        }

        Scheme scheme = schemeRepository.findBySchemeCode(app.getSchemeCode()).orElse(null);
        List<SchemeDocumentRequirementResolver.ResolvedRequirement> resolvedReqs =
                schemeDocumentRequirementResolver != null && scheme != null ?
                        schemeDocumentRequirementResolver.resolveRequirements(scheme) : List.of();

        List<ApplicationDocument> docs = applicationDocumentRepository.findAllByApplicationId(applicationId);
        Map<String, ApplicationDocument> docMap = docs.stream()
                .collect(Collectors.toMap(d -> d.getDocumentCode().toUpperCase(), d -> d, (a, b) -> a));

        Map<String, Boolean> altGroupSatisfiedMap = new HashMap<>();
        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
            if (req.getAlternativeGroupId() != null) {
                ApplicationDocument d = docMap.get(req.getDocumentCode().toUpperCase());
                boolean isValidUpload = d != null && d.isUploaded() &&
                        d.getDetailedStatus() != DetailedDocumentStatus.NOT_UPLOADED &&
                        d.getDetailedStatus() != DetailedDocumentStatus.REJECTED &&
                        d.getDetailedStatus() != DetailedDocumentStatus.REUPLOAD_REQUIRED;
                if (isValidUpload) {
                    altGroupSatisfiedMap.put(req.getAlternativeGroupId(), true);
                }
            }
        }

        List<String> missingMandatory = new ArrayList<>();
        Set<String> failedAltGroups = new HashSet<>();
        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
            if (req.isMandatory()) {
                if (req.getAlternativeGroupId() != null) {
                    if (!Boolean.TRUE.equals(altGroupSatisfiedMap.get(req.getAlternativeGroupId()))) {
                        if (!failedAltGroups.contains(req.getAlternativeGroupId())) {
                            failedAltGroups.add(req.getAlternativeGroupId());
                            missingMandatory.add(req.getDocumentName() + " (Any ONE of: " + (req.getAlternatives() != null && !req.getAlternatives().isEmpty() ? String.join(", ", req.getAlternatives()) : req.getDocumentCode()) + ")");
                        }
                    }
                } else {
                    ApplicationDocument d = docMap.get(req.getDocumentCode().toUpperCase());
                    if (d == null || !d.isUploaded() ||
                        d.getDetailedStatus() == DetailedDocumentStatus.NOT_UPLOADED ||
                        d.getDetailedStatus() == DetailedDocumentStatus.REJECTED ||
                        d.getDetailedStatus() == DetailedDocumentStatus.REUPLOAD_REQUIRED) {
                        missingMandatory.add(req.getDocumentCode());
                    }
                }
            }
        }

        if (!missingMandatory.isEmpty()) {
            throw new IllegalArgumentException("Cannot submit application. Mandatory documents are missing or rejected: " + missingMandatory);
        }

        ApplicationStatus currentStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(currentStatus, ApplicationStatus.SUBMITTED)) {
            throw new IllegalStateException("Cannot transition from " + currentStatus + " to SUBMITTED.");
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(Instant.now());
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        applicationEventService.recordEvent(
                applicationId,
                userId,
                ApplicationEventType.APPLICATION_SUBMITTED,
                currentStatus,
                ApplicationStatus.SUBMITTED,
                "Application submitted by citizen.",
                Map.of("submittedAt", app.getSubmittedAt().toString())
        );

        notificationService.sendNotification(
                null,
                "ROLE_ADMIN",
                NotificationType.APPLICATION_SUBMITTED,
                "New Scheme Application Submitted: " + app.getApplicationNumber(),
                "A new application for scheme " + app.getSchemeCode() + " has been submitted by citizen.",
                "IN_APP",
                "APPLICATION",
                applicationId,
                userId,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        return mapToResponse(savedApp);
    }

    @Transactional
    public ApplicationResponse cancelApplication(String applicationId, String userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!app.getUserId().equals(userId)) {
            throw new SecurityException("You do not have permission to cancel this application.");
        }

        ApplicationStatus currentStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(currentStatus, ApplicationStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel application in " + currentStatus + " state.");
        }

        app.setStatus(ApplicationStatus.CANCELLED);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        applicationEventService.recordEvent(
                applicationId,
                userId,
                ApplicationEventType.CANCELLED,
                currentStatus,
                ApplicationStatus.CANCELLED,
                "Application cancelled by citizen.",
                Map.of()
        );

        return mapToResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public SchemeDocumentChecklistResponse getDocumentChecklist(String applicationId, String userId, boolean isPrivileged) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!isPrivileged && !app.getUserId().equals(userId)) {
            throw new SecurityException("You do not have permission to access this application.");
        }

        Scheme scheme = schemeRepository.findBySchemeCode(app.getSchemeCode())
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with code: " + app.getSchemeCode()));

        // Check canonical knowledge base (scheme_verified_data)
        com.schemebridge.scheme.document.SchemeVerifiedData canonical = null;
        if (schemeVerifiedDataRepository != null) {
            canonical = schemeVerifiedDataRepository.findBySchemeCode(scheme.getSchemeCode()).orElse(null);
        }

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> resolvedReqs =
                schemeDocumentRequirementResolver != null ?
                        schemeDocumentRequirementResolver.resolveRequirements(scheme) : List.of();

        List<ApplicationDocument> appDocs = applicationDocumentRepository.findAllByApplicationId(applicationId);
        Map<String, ApplicationDocument> docMap = appDocs.stream()
                .collect(Collectors.toMap(d -> d.getDocumentCode().toUpperCase(), d -> d, (a, b) -> a));

        List<DocumentChecklistItemResponse> items = new ArrayList<>();
        int totalReq = 0;
        int totalUp = 0;
        int totalVer = 0;
        int totalRej = 0;
        int mandatoryDocCount = 0;
        Set<String> altGroupsEncountered = new HashSet<>();
        Map<String, Boolean> altGroupSatisfiedMap = new HashMap<>();
        List<String> missingReqs = new ArrayList<>();

        // First pass: identify alternative groups
        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
            if (req.getAlternativeGroupId() != null) {
                altGroupsEncountered.add(req.getAlternativeGroupId());
                altGroupSatisfiedMap.putIfAbsent(req.getAlternativeGroupId(), false);
            }
        }

        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
            if (req.isMandatory()) {
                totalReq++;
                mandatoryDocCount++;
            }

            ApplicationDocument doc = docMap.get(req.getDocumentCode().toUpperCase());
            boolean uploaded = doc != null && doc.isUploaded();
            if (uploaded) totalUp++;

            DetailedDocumentStatus status = DetailedDocumentStatus.NOT_UPLOADED;
            if (doc != null && doc.getDetailedStatus() != null) {
                status = doc.getDetailedStatus();
            } else if (uploaded) {
                if (doc.getVerificationStatus() == DocumentVerificationStatus.VERIFIED) {
                    status = DetailedDocumentStatus.VERIFIED;
                } else if (doc.getVerificationStatus() == DocumentVerificationStatus.REJECTED) {
                    status = DetailedDocumentStatus.REJECTED;
                } else {
                    status = DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION;
                }
            }

            boolean isVerified = status == DetailedDocumentStatus.VERIFIED;
            if (isVerified) totalVer++;
            if (status == DetailedDocumentStatus.REJECTED || status == DetailedDocumentStatus.REUPLOAD_REQUIRED) totalRej++;

            boolean isValidUpload = uploaded && status != DetailedDocumentStatus.REJECTED && status != DetailedDocumentStatus.REUPLOAD_REQUIRED;

            if (req.getAlternativeGroupId() != null && isValidUpload) {
                altGroupSatisfiedMap.put(req.getAlternativeGroupId(), true);
            }

            boolean ocrCompleted = doc != null && doc.isUploaded() && (status == DetailedDocumentStatus.OCR_COMPLETED || status == DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION || status == DetailedDocumentStatus.VERIFIED);
            boolean ocrAvailable = doc != null && doc.isUploaded();

            items.add(DocumentChecklistItemResponse.builder()
                    .documentCode(req.getDocumentCode())
                    .canonicalDocumentCode(req.getCanonicalDocumentCode())
                    .documentName(req.getDocumentName())
                    .description(req.getDescription())
                    .whyRequired(req.getWhyRequired())
                    .mandatory(req.isMandatory())
                    .required(req.isMandatory())
                    .optional(req.isOptional())
                    .alternativeGroupId(req.getAlternativeGroupId())
                    .alternativeGroupType(req.getAlternativeGroupType())
                    .alternatives(req.getAlternatives())
                    .acceptedFormats(req.getAcceptedFormats())
                    .maxSizeBytes(req.getMaxSizeBytes())
                    .issuingAuthority(req.getIssuingAuthority())
                    .provenance(req.getProvenance())
                    .officialSourceUrl(req.getOfficialSourceUrl())
                    .sourceReference(req.getSourceReference())
                    .sourceLastVerified(req.getSourceLastVerified())
                    .status(status)
                    .uploaded(uploaded)
                    .fileName(doc != null ? doc.getFileName() : null)
                    .version(doc != null ? doc.getVersion() : null)
                    .uploadedAt(doc != null ? doc.getUploadedAt() : null)
                    .ocrAvailable(ocrAvailable)
                    .ocrCompleted(ocrCompleted)
                    .ocrStatus(ocrCompleted ? "OCR_COMPLETED" : (ocrAvailable ? "OCR_AVAILABLE" : "NOT_PROCESSED"))
                    .verificationStatus(doc != null && doc.getVerificationStatus() != null ? doc.getVerificationStatus().name() : "PENDING")
                    .verified(isVerified)
                    .rejectionReason(doc != null ? doc.getRejectionReason() : null)
                    .verifiedBy(doc != null ? doc.getVerifiedBy() : null)
                    .canSubmit(isValidUpload)
                    .canApprove(isVerified)
                    .alternativeSatisfied(req.getAlternativeGroupId() != null && Boolean.TRUE.equals(altGroupSatisfiedMap.get(req.getAlternativeGroupId())))
                    .versionHistory(doc != null ? doc.getVersionHistory() : List.of())
                    .build());
        }

        // Evaluation of submission readiness taking ONE_OF alternatives into account
        boolean allMandatorySatisfied = true;
        boolean allMandatoryVerified = true;

        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
            if (req.isMandatory()) {
                if (req.getAlternativeGroupId() != null) {
                    if (!Boolean.TRUE.equals(altGroupSatisfiedMap.get(req.getAlternativeGroupId()))) {
                        allMandatorySatisfied = false;
                        if (!missingReqs.contains("Alternative group: " + req.getDocumentName())) {
                            missingReqs.add("Alternative group: " + req.getDocumentName());
                        }
                    }
                } else {
                    ApplicationDocument doc = docMap.get(req.getDocumentCode().toUpperCase());
                    boolean uploaded = doc != null && doc.isUploaded();
                    DetailedDocumentStatus status = doc != null ? doc.getDetailedStatus() : DetailedDocumentStatus.NOT_UPLOADED;
                    if (!uploaded || status == DetailedDocumentStatus.NOT_UPLOADED || status == DetailedDocumentStatus.REJECTED || status == DetailedDocumentStatus.REUPLOAD_REQUIRED) {
                        allMandatorySatisfied = false;
                        missingReqs.add(req.getDocumentName());
                    }
                    if (doc == null || doc.getVerificationStatus() != DocumentVerificationStatus.VERIFIED) {
                        allMandatoryVerified = false;
                    }
                }
            }
        }

        boolean canSubmit = allMandatorySatisfied && app.getStatus() == ApplicationStatus.READY_FOR_SUBMISSION;
        boolean canApprove = allMandatoryVerified && app.getStatus() == ApplicationStatus.UNDER_REVIEW;

        String docStatus = canonical != null ? canonical.getDocumentStatus() : (resolvedReqs.isEmpty() ? "DOCUMENT_REQUIREMENTS_NOT_MAPPED" : "DOCUMENTS_FOUND");
        String overallProv = canonical != null && canonical.getOverallProvenance() != null ? canonical.getOverallProvenance().name() : (resolvedReqs.isEmpty() ? "UNKNOWN_OR_UNSTRUCTURED" : "SYSTEM_CONFIGURED");

        return SchemeDocumentChecklistResponse.builder()
                .schemeCode(scheme.getSchemeCode())
                .schemeTitle(scheme.getTitle() != null ? scheme.getTitle().getEnglish() : scheme.getSchemeCode())
                .slug(canonical != null ? canonical.getSlug() : scheme.getSlug())
                .officialSourceUrl(canonical != null && canonical.getSourceMetadata() != null ? canonical.getSourceMetadata().getSourceUrl() : (scheme.getSource() != null ? scheme.getSource().getSourceUrl() : null))
                .officialSourceName(canonical != null && canonical.getSourceMetadata() != null ? canonical.getSourceMetadata().getSourceType() : "Official Government Source")
                .sourceLastUpdated(canonical != null && canonical.getSourceMetadata() != null && canonical.getSourceMetadata().getLastVerifiedAt() != null ? canonical.getSourceMetadata().getLastVerifiedAt().toString() : null)
                .reconciliationStatus(canonical != null ? canonical.getReconciliationStatus() : "MATCHED")
                .documentStatus(docStatus)
                .overallProvenance(overallProv)
                .applicationId(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .applicationStatus(app.getStatus().name())
                .canSubmit(canSubmit)
                .canApprove(canApprove)
                .totalDocuments(resolvedReqs.size())
                .totalRequired(totalReq)
                .mandatoryDocumentCount(mandatoryDocCount)
                .alternativeGroupCount(altGroupsEncountered.size())
                .totalUploaded(totalUp)
                .totalVerified(totalVer)
                .totalRejected(totalRej)
                .items(items)
                .missingRequirements(missingReqs)
                .build();
    }

    @Transactional(readOnly = true)
    public SchemeDocumentChecklistResponse getSchemeDocumentChecklist(String schemeCode) {
        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .or(() -> schemeRepository.findBySchemeCode(schemeCode.toUpperCase()))
                .or(() -> schemeRepository.findById(schemeCode))
                .or(() -> schemeRepository.findBySlug(schemeCode.toLowerCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with code/ID/slug: " + schemeCode));

        // Check canonical knowledge base (scheme_verified_data)
        com.schemebridge.scheme.document.SchemeVerifiedData canonical = null;
        if (schemeVerifiedDataRepository != null) {
            canonical = schemeVerifiedDataRepository.findBySchemeCode(scheme.getSchemeCode()).orElse(null);
        }

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> resolvedReqs =
                schemeDocumentRequirementResolver != null ?
                        schemeDocumentRequirementResolver.resolveRequirements(scheme) : List.of();

        List<DocumentChecklistItemResponse> items = new ArrayList<>();
        int totalReq = 0;
        int mandatoryDocCount = 0;
        Set<String> altGroups = new HashSet<>();

        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
            if (req.isMandatory()) {
                totalReq++;
                mandatoryDocCount++;
            }
            if (req.getAlternativeGroupId() != null) {
                altGroups.add(req.getAlternativeGroupId());
            }

            items.add(DocumentChecklistItemResponse.builder()
                    .documentCode(req.getDocumentCode())
                    .canonicalDocumentCode(req.getCanonicalDocumentCode())
                    .documentName(req.getDocumentName())
                    .description(req.getDescription())
                    .whyRequired(req.getWhyRequired())
                    .mandatory(req.isMandatory())
                    .required(req.isMandatory())
                    .optional(req.isOptional())
                    .alternativeGroupId(req.getAlternativeGroupId())
                    .alternativeGroupType(req.getAlternativeGroupType())
                    .alternatives(req.getAlternatives())
                    .acceptedFormats(req.getAcceptedFormats())
                    .maxSizeBytes(req.getMaxSizeBytes())
                    .issuingAuthority(req.getIssuingAuthority())
                    .provenance(req.getProvenance())
                    .officialSourceUrl(req.getOfficialSourceUrl())
                    .sourceReference(req.getSourceReference())
                    .sourceLastVerified(req.getSourceLastVerified())
                    .status(DetailedDocumentStatus.NOT_UPLOADED)
                    .uploaded(false)
                    .version(1)
                    .verificationStatus("PENDING")
                    .verified(false)
                    .canSubmit(false)
                    .canApprove(false)
                    .build());
        }

        String docStatus = canonical != null ? canonical.getDocumentStatus() : (resolvedReqs.isEmpty() ? "DOCUMENT_REQUIREMENTS_NOT_MAPPED" : "DOCUMENTS_FOUND");
        String overallProv = canonical != null && canonical.getOverallProvenance() != null ? canonical.getOverallProvenance().name() : (resolvedReqs.isEmpty() ? "UNKNOWN_OR_UNSTRUCTURED" : "SYSTEM_CONFIGURED");

        List<String> eligConditions = new ArrayList<>();
        if (canonical != null && canonical.getEligibility() != null && canonical.getEligibility().getEligibilityText() != null && !canonical.getEligibility().getEligibilityText().isBlank()) {
            eligConditions.add(canonical.getEligibility().getEligibilityText());
        } else if (scheme.getEligibilityRules() != null && scheme.getEligibilityRules().getConditions() != null) {
            for (EligibilityCondition cond : scheme.getEligibilityRules().getConditions()) {
                if (cond.getField() != null) {
                    eligConditions.add(cond.getField() + " " + (cond.getOperator() != null ? cond.getOperator() : "==") + " " + (cond.getValue() != null ? cond.getValue() : ""));
                }
            }
        }

        List<String> appSteps = new ArrayList<>();
        String appMode = null;
        String appUrl = null;
        String helpline = null;
        if (canonical != null && canonical.getApplication() != null) {
            if (canonical.getApplication().getApplicationSteps() != null && !canonical.getApplication().getApplicationSteps().isEmpty()) {
                appSteps.addAll(canonical.getApplication().getApplicationSteps());
            }
            appMode = canonical.getApplication().getApplicationMethod();
            appUrl = canonical.getApplication().getOfficialApplicationUrl();
            helpline = canonical.getApplication().getHelplineNumber();
        }
        if (appSteps.isEmpty() && scheme.getApplicationInfo() != null && scheme.getApplicationInfo().getInstructions() != null && scheme.getApplicationInfo().getInstructions().getEnglish() != null) {
            appSteps.add(scheme.getApplicationInfo().getInstructions().getEnglish());
        }
        if (appMode == null && scheme.getApplicationInfo() != null) {
            appMode = scheme.getApplicationInfo().getApplicationMode();
        }
        if (appUrl == null && scheme.getApplicationInfo() != null) {
            appUrl = scheme.getApplicationInfo().getApplicationUrl();
        }

        List<String> benefitList = new ArrayList<>();
        if (canonical != null && canonical.getBenefits() != null && !canonical.getBenefits().isEmpty()) {
            for (SchemeVerifiedData.CanonicalBenefit cb : canonical.getBenefits()) {
                if (cb.getBenefitType() != null) {
                    benefitList.add(cb.getBenefitType() + (cb.getAmountCurrency() != null ? ": " + cb.getAmountCurrency() : ""));
                }
            }
        }
        if (benefitList.isEmpty() && scheme.getBenefits() != null) {
            for (SchemeBenefit b : scheme.getBenefits()) {
                if (b.getDescription() != null && b.getDescription().getEnglish() != null) {
                    benefitList.add(b.getDescription().getEnglish());
                }
            }
        }

        return SchemeDocumentChecklistResponse.builder()
                .schemeCode(scheme.getSchemeCode())
                .schemeTitle(scheme.getTitle() != null ? scheme.getTitle().getEnglish() : scheme.getSchemeCode())
                .slug(canonical != null ? canonical.getSlug() : scheme.getSlug())
                .officialSourceUrl(canonical != null && canonical.getSourceMetadata() != null ? canonical.getSourceMetadata().getSourceUrl() : (scheme.getSource() != null ? scheme.getSource().getSourceUrl() : null))
                .officialSourceName(canonical != null && canonical.getSourceMetadata() != null ? canonical.getSourceMetadata().getSourceType() : "Official Government Source")
                .sourceLastUpdated(canonical != null && canonical.getSourceMetadata() != null && canonical.getSourceMetadata().getLastVerifiedAt() != null ? canonical.getSourceMetadata().getLastVerifiedAt().toString() : null)
                .reconciliationStatus(canonical != null ? canonical.getReconciliationStatus() : "MATCHED")
                .documentStatus(docStatus)
                .overallProvenance(overallProv)
                .totalDocuments(resolvedReqs.size())
                .totalRequired(totalReq)
                .mandatoryDocumentCount(mandatoryDocCount)
                .alternativeGroupCount(altGroups.size())
                .items(items)
                .missingRequirements(List.of())
                .eligibilityConditions(eligConditions)
                .applicationSteps(appSteps)
                .applicationMode(appMode)
                .officialApplicationUrl(appUrl)
                .helplineNumber(helpline)
                .benefits(benefitList)
                .build();
    }


    @Transactional(readOnly = true)
    public ApplicationTimelineResponse getTimeline(String applicationId, String userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = false;
        if (authentication != null) {
            isPrivileged = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") 
                                   || auth.getAuthority().equals("ROLE_SCHEME_MANAGER")
                                   || auth.getAuthority().equals("ROLE_VERIFICATION_OFFICER"));
        }

        if (!app.getUserId().equals(userId) && !isPrivileged) {
            throw new SecurityException("You do not have permission to access this application.");
        }

        return applicationEventService.getTimeline(app);
    }

    public InputStream downloadDocument(String applicationId, String documentCode, String userId) {
        DocumentDownloadDto dto = getDocumentDownload(applicationId, documentCode, userId, false);
        return dto.getInputStream();
    }

    private void updateApplicationReadinessStatus(Application app) {
        List<ApplicationDocument> docs = applicationDocumentRepository.findAllByApplicationId(app.getId());
        
        Scheme scheme = schemeRepository.findBySchemeCode(app.getSchemeCode()).orElse(null);
        List<SchemeDocumentRequirementResolver.ResolvedRequirement> resolvedReqs =
                schemeDocumentRequirementResolver != null && scheme != null ?
                        schemeDocumentRequirementResolver.resolveRequirements(scheme) : List.of();

        boolean isReady = true;

        if (!resolvedReqs.isEmpty()) {
            Map<String, ApplicationDocument> docMap = docs.stream()
                    .collect(Collectors.toMap(d -> d.getDocumentCode().toUpperCase(), d -> d, (a, b) -> a));

            Map<String, Boolean> altGroupSatisfiedMap = new HashMap<>();
            for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
                if (req.getAlternativeGroupId() != null) {
                    ApplicationDocument d = docMap.get(req.getDocumentCode().toUpperCase());
                    boolean isValidUpload = d != null && d.isUploaded() &&
                            d.getDetailedStatus() != DetailedDocumentStatus.NOT_UPLOADED &&
                            d.getDetailedStatus() != DetailedDocumentStatus.REJECTED &&
                            d.getDetailedStatus() != DetailedDocumentStatus.REUPLOAD_REQUIRED;
                    if (isValidUpload) {
                        altGroupSatisfiedMap.put(req.getAlternativeGroupId(), true);
                    }
                }
            }

            for (SchemeDocumentRequirementResolver.ResolvedRequirement req : resolvedReqs) {
                if (req.isMandatory()) {
                    if (req.getAlternativeGroupId() != null) {
                        if (!Boolean.TRUE.equals(altGroupSatisfiedMap.get(req.getAlternativeGroupId()))) {
                            isReady = false;
                            break;
                        }
                    } else {
                        ApplicationDocument d = docMap.get(req.getDocumentCode().toUpperCase());
                        if (d == null || !d.isUploaded() ||
                            d.getDetailedStatus() == DetailedDocumentStatus.NOT_UPLOADED ||
                            d.getDetailedStatus() == DetailedDocumentStatus.REJECTED ||
                            d.getDetailedStatus() == DetailedDocumentStatus.REUPLOAD_REQUIRED) {
                            isReady = false;
                            break;
                        }
                    }
                }
            }
        } else {
            long mandatoryCount = docs.stream().filter(ApplicationDocument::isMandatory).count();
            long uploadedMandatoryCount = docs.stream().filter(d -> d.isMandatory() && d.isUploaded() &&
                    d.getDetailedStatus() != DetailedDocumentStatus.REJECTED &&
                    d.getDetailedStatus() != DetailedDocumentStatus.REUPLOAD_REQUIRED).count();
            isReady = (mandatoryCount == 0 || uploadedMandatoryCount == mandatoryCount);
        }

        ApplicationStatus newStatus = isReady ? ApplicationStatus.READY_FOR_SUBMISSION : ApplicationStatus.DOCUMENTS_PENDING;

        if (app.getStatus() != newStatus) {
            ApplicationStatus oldStatus = app.getStatus();
            if (applicationStatusTransitionService.isValidTransition(oldStatus, newStatus)) {
                app.setStatus(newStatus);
                app.setUpdatedAt(Instant.now());
                applicationRepository.save(app);

                ApplicationEventType eventType = newStatus == ApplicationStatus.READY_FOR_SUBMISSION ?
                        ApplicationEventType.READY_FOR_SUBMISSION : ApplicationEventType.DOCUMENTS_PENDING;

                applicationEventService.recordEvent(
                        app.getId(),
                        app.getUserId(),
                        eventType,
                        oldStatus,
                        newStatus,
                        "Status changed after document upload.",
                        Map.of()
                );
            }
        }
    }

    public ApplicationResponse mapToResponse(Application app) {
        List<ApplicationDocument> docs = applicationDocumentRepository.findAllByApplicationId(app.getId());
        List<ApplicationDocumentResponse> docResponses = docs.stream()
                .map(this::mapToDocResponse)
                .collect(Collectors.toList());

        long mandatoryCount = docs.stream().filter(ApplicationDocument::isMandatory).count();
        long uploadedMandatoryCount = docs.stream().filter(d -> d.isMandatory() && d.isUploaded() &&
                d.getDetailedStatus() != DetailedDocumentStatus.REJECTED &&
                d.getDetailedStatus() != DetailedDocumentStatus.REUPLOAD_REQUIRED).count();
        int totalDocs = docs.size();
        int uploadedDocs = (int) docs.stream().filter(ApplicationDocument::isUploaded).count();
        int verifiedDocs = (int) docs.stream().filter(d -> d.getVerificationStatus() == DocumentVerificationStatus.VERIFIED || d.getDetailedStatus() == DetailedDocumentStatus.VERIFIED).count();
        int rejectedDocs = (int) docs.stream().filter(d -> d.getVerificationStatus() == DocumentVerificationStatus.REJECTED || d.getDetailedStatus() == DetailedDocumentStatus.REJECTED || d.getDetailedStatus() == DetailedDocumentStatus.REUPLOAD_REQUIRED).count();
        int missingMandatory = (int) (mandatoryCount - uploadedMandatoryCount);
        int percentage = totalDocs > 0 ? (int) Math.round(((double) uploadedDocs / totalDocs) * 100) : 100;

        DocumentReadinessResponse readiness = DocumentReadinessResponse.builder()
                .total(totalDocs)
                .uploaded(uploadedDocs)
                .mandatoryMissing(missingMandatory)
                .verified(verifiedDocs)
                .rejected(rejectedDocs)
                .percentage(percentage)
                .build();

        MultilingualText schemeTitle = schemeRepository.findBySchemeCode(app.getSchemeCode())
                .map(Scheme::getTitle)
                .orElse(MultilingualText.builder().english(app.getSchemeCode()).build());

        return ApplicationResponse.builder()
                .id(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .userId(app.getUserId())
                .schemeCode(app.getSchemeCode())
                .schemeTitle(schemeTitle)
                .status(app.getStatus().name())
                .submittedAt(app.getSubmittedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .documentReadiness(readiness)
                .documents(docResponses)
                .build();
    }

    public ApplicationDocumentResponse mapToDocResponse(ApplicationDocument doc) {
        DetailedDocumentStatus st = doc.getDetailedStatus();
        if (st == null) {
            if (!doc.isUploaded()) {
                st = DetailedDocumentStatus.NOT_UPLOADED;
            } else if (doc.getVerificationStatus() == DocumentVerificationStatus.VERIFIED) {
                st = DetailedDocumentStatus.VERIFIED;
            } else if (doc.getVerificationStatus() == DocumentVerificationStatus.REJECTED) {
                st = DetailedDocumentStatus.REJECTED;
            } else {
                st = DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION;
            }
        }

        return ApplicationDocumentResponse.builder()
                .id(doc.getId())
                .applicationId(doc.getApplicationId())
                .documentCode(doc.getDocumentCode())
                .documentName(doc.getDocumentName())
                .mandatory(doc.isMandatory())
                .uploaded(doc.isUploaded())
                .fileName(doc.getFileName())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .version(doc.getVersion())
                .uploadedAt(doc.getUploadedAt())
                .verifiedAt(doc.getVerifiedAt())
                .rejectedAt(doc.getRejectedAt())
                .rejectionReason(doc.getRejectionReason())
                .verificationStatus(doc.getVerificationStatus() != null ? doc.getVerificationStatus().name() : "PENDING")
                .status(st)
                .versionHistory(doc.getVersionHistory() != null ? doc.getVersionHistory() : List.of())
                .build();
    }
}
