package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.SchemeCreateRequest;
import com.schemebridge.scheme.dto.request.SchemeUpdateRequest;
import com.schemebridge.scheme.dto.response.BulkEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.SchemeEvaluationSummary;
import com.schemebridge.scheme.dto.response.SchemeResponse;
import com.schemebridge.scheme.exception.DuplicateResourceException;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.SchemeCategoryRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeCategoryRepository categoryRepository;
    private final EligibilityEngine eligibilityEngine;

    @Transactional
    public SchemeResponse createScheme(SchemeCreateRequest request) {
        // Validate unique code
        if (schemeRepository.findBySchemeCode(request.getSchemeCode()).isPresent()) {
            throw new DuplicateResourceException("Scheme code already exists: " + request.getSchemeCode());
        }
        // Validate unique slug
        if (schemeRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new DuplicateResourceException("Scheme slug already exists: " + request.getSlug());
        }

        // Validate category exists
        SchemeCategory category = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with code: " + request.getCategoryCode()));

        SchemeStatus status = SchemeStatus.DRAFT;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                status = SchemeStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + request.getStatus());
            }
        }

        SchemeLevel level = null;
        if (request.getSchemeLevel() != null && !request.getSchemeLevel().trim().isEmpty()) {
            try {
                level = SchemeLevel.valueOf(request.getSchemeLevel().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid scheme level: " + request.getSchemeLevel());
            }
        }

        int version = 1;
        if (request.getVersion() != null) {
            version = request.getVersion();
        }

        java.time.Instant lastVerifiedAt = null;
        if (request.getSource() != null) {
            lastVerifiedAt = request.getSource().getLastVerified();
        }

        Scheme scheme = Scheme.builder()
                .schemeCode(request.getSchemeCode())
                .slug(request.getSlug())
                .title(request.getTitle())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .category(SchemeCategoryRef.builder()
                        .code(category.getCode())
                        .name(category.getName())
                        .build())
                .department(request.getDepartment())
                .ministry(request.getMinistry())
                .schemeLevel(level)
                .stateOrUt(request.getStateOrUt())
                .beneficiaryType(request.getBeneficiaryType())
                .schemeType(request.getSchemeType())
                .eligibilityRules(request.getEligibilityRules())
                .benefits(request.getBenefits() != null ? request.getBenefits() : new ArrayList<>())
                .requiredDocuments(request.getRequiredDocuments() != null ? request.getRequiredDocuments() : new ArrayList<>())
                .applicationInfo(request.getApplicationInfo())
                .source(request.getSource())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .faqs(request.getFaqs() != null ? request.getFaqs() : new ArrayList<>())
                .status(status)
                .version(version)
                .lastVerifiedAt(lastVerifiedAt)
                .build();

        Scheme saved = schemeRepository.save(scheme);
        return mapToResponse(saved);
    }

    @Transactional
    public SchemeResponse updateScheme(String id, SchemeUpdateRequest request) {
        Scheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + id));

        // Update category if provided
        if (request.getCategoryCode() != null && !request.getCategoryCode().trim().isEmpty() &&
                (scheme.getCategory() == null || !request.getCategoryCode().equals(scheme.getCategory().getCode()))) {
            SchemeCategory category = categoryRepository.findByCode(request.getCategoryCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with code: " + request.getCategoryCode()));
            scheme.setCategory(SchemeCategoryRef.builder()
                    .code(category.getCode())
                    .name(category.getName())
                    .build());
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                scheme.setStatus(SchemeStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + request.getStatus());
            }
        }

        SchemeLevel level = null;
        if (request.getSchemeLevel() != null && !request.getSchemeLevel().trim().isEmpty()) {
            try {
                level = SchemeLevel.valueOf(request.getSchemeLevel().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid scheme level: " + request.getSchemeLevel());
            }
        }

        scheme.setTitle(request.getTitle());
        scheme.setDescription(request.getDescription());
        scheme.setShortDescription(request.getShortDescription());
        scheme.setDepartment(request.getDepartment());
        scheme.setMinistry(request.getMinistry());
        scheme.setSchemeLevel(level);
        scheme.setStateOrUt(request.getStateOrUt());
        scheme.setBeneficiaryType(request.getBeneficiaryType());
        scheme.setSchemeType(request.getSchemeType());
        scheme.setEligibilityRules(request.getEligibilityRules());
        scheme.setBenefits(request.getBenefits() != null ? request.getBenefits() : new ArrayList<>());
        scheme.setRequiredDocuments(request.getRequiredDocuments() != null ? request.getRequiredDocuments() : new ArrayList<>());
        scheme.setApplicationInfo(request.getApplicationInfo());
        scheme.setSource(request.getSource());
        if (request.getSource() != null) {
            scheme.setLastVerifiedAt(request.getSource().getLastVerified());
        }
        scheme.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        scheme.setFaqs(request.getFaqs() != null ? request.getFaqs() : new ArrayList<>());
        if (request.getVersion() != null) {
            scheme.setVersion(request.getVersion());
        }

        Scheme saved = schemeRepository.save(scheme);
        return mapToResponse(saved);
    }

    @Transactional
    public SchemeResponse updateStatus(String id, String statusStr) {
        Scheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + id));

        try {
            SchemeStatus status = SchemeStatus.valueOf(statusStr.toUpperCase());
            scheme.setStatus(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + statusStr);
        }

        Scheme saved = schemeRepository.save(scheme);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteScheme(String id) {
        Scheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + id));
        schemeRepository.delete(scheme);
    }

    @Transactional(readOnly = true)
    public SchemeResponse getSchemeById(String id) {
        Scheme scheme = schemeRepository.findById(id)
                .or(() -> schemeRepository.findBySchemeCode(id))
                .or(() -> schemeRepository.findBySchemeCode(id.toUpperCase()))
                .or(() -> schemeRepository.findBySlug(id.toLowerCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID/Code/Slug: " + id));
        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public SchemeResponse getSchemeByCode(String code) {
        Scheme scheme = schemeRepository.findBySchemeCode(code)
                .or(() -> schemeRepository.findBySchemeCode(code.toUpperCase()))
                .or(() -> schemeRepository.findById(code))
                .or(() -> schemeRepository.findBySlug(code.toLowerCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with code: " + code));
        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public EligibilityEvaluationResponse evaluateEligibilityById(String id, CitizenEligibilityProfile profile) {
        Scheme scheme = schemeRepository.findById(id)
                .or(() -> schemeRepository.findBySchemeCode(id))
                .or(() -> schemeRepository.findBySchemeCode(id.toUpperCase()))
                .or(() -> schemeRepository.findBySlug(id.toLowerCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID/Code/Slug: " + id));
        return eligibilityEngine.evaluateScheme(scheme, profile);
    }

    @Transactional(readOnly = true)
    public EligibilityEvaluationResponse evaluateEligibilityByCode(String code, CitizenEligibilityProfile profile) {
        Scheme scheme = schemeRepository.findBySchemeCode(code)
                .or(() -> schemeRepository.findBySchemeCode(code.toUpperCase()))
                .or(() -> schemeRepository.findById(code))
                .or(() -> schemeRepository.findBySlug(code.toLowerCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with code: " + code));
        return eligibilityEngine.evaluateScheme(scheme, profile);
    }

    @Transactional(readOnly = true)
    public BulkEligibilityEvaluationResponse evaluateEligibilityForAll(
            CitizenEligibilityProfile profile,
            SchemeStatus status
    ) {
        SchemeStatus allowedStatus = SchemeStatus.ACTIVE;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = false;
        if (authentication != null) {
            isPrivileged = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") 
                                   || auth.getAuthority().equals("ROLE_SCHEME_MANAGER"));
        }

        if (status != null) {
            if (isPrivileged) {
                allowedStatus = status;
            } else {
                allowedStatus = SchemeStatus.ACTIVE;
            }
        } else {
            allowedStatus = SchemeStatus.ACTIVE;
        }

        List<Scheme> schemes = schemeRepository.findAllByStatus(allowedStatus);
        BulkEligibilityEvaluationResponse response = new BulkEligibilityEvaluationResponse();

        for (Scheme scheme : schemes) {
            EligibilityEvaluationResponse evalRes = eligibilityEngine.evaluateScheme(scheme, profile);

            SchemeEvaluationSummary summary = SchemeEvaluationSummary.builder()
                    .schemeId(scheme.getId())
                    .schemeCode(scheme.getSchemeCode())
                    .slug(scheme.getSlug())
                    .title(scheme.getTitle())
                    .description(scheme.getDescription())
                    .matchedConditions(evalRes.getMatchedConditions())
                    .failedConditions(evalRes.getFailedConditions())
                    .missingInformation(evalRes.getMissingInformation())
                    .details(evalRes.getDetails())
                    .build();

            if (evalRes.getStatus() == EvaluationStatus.ELIGIBLE) {
                response.getEligible().add(summary);
            } else if (evalRes.getStatus() == EvaluationStatus.NOT_ELIGIBLE) {
                response.getIneligible().add(summary);
            } else {
                response.getIndeterminate().add(summary);
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<SchemeResponse> getAllSchemes(SchemeStatus status, String categoryCode) {
        List<Scheme> schemes;
        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            if (status != null) {
                schemes = schemeRepository.findAllByCategoryCodeAndStatus(categoryCode, status);
            } else {
                schemes = schemeRepository.findAllByCategoryCode(categoryCode);
            }
        } else {
            if (status != null) {
                schemes = schemeRepository.findAllByStatus(status);
            } else {
                schemes = schemeRepository.findAll();
            }
        }
        return schemes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SchemeResponse mapToResponse(Scheme scheme) {
        return SchemeResponse.builder()
                .id(scheme.getId())
                .schemeCode(scheme.getSchemeCode())
                .slug(scheme.getSlug())
                .title(scheme.getTitle())
                .description(scheme.getDescription())
                .shortDescription(scheme.getShortDescription())
                .category(scheme.getCategory())
                .department(scheme.getDepartment())
                .ministry(scheme.getMinistry())
                .schemeLevel(scheme.getSchemeLevel())
                .stateOrUt(scheme.getStateOrUt())
                .beneficiaryType(scheme.getBeneficiaryType())
                .schemeType(scheme.getSchemeType())
                .eligibilityRules(scheme.getEligibilityRules())
                .benefits(scheme.getBenefits())
                .requiredDocuments(scheme.getRequiredDocuments())
                .applicationInfo(scheme.getApplicationInfo())
                .source(scheme.getSource())
                .tags(scheme.getTags())
                .faqs(scheme.getFaqs())
                .status(scheme.getStatus())
                .version(scheme.getVersion())
                .lastVerifiedAt(scheme.getLastVerifiedAt())
                .createdAt(scheme.getCreatedAt())
                .updatedAt(scheme.getUpdatedAt())
                .deadline(scheme.getApplicationInfo() != null && scheme.getApplicationInfo().getDeadline() != null
                        ? scheme.getApplicationInfo().getDeadline().toString()
                        : (scheme.getApplicationInfo() != null && scheme.getApplicationInfo().getApplicationEndDate() != null
                                ? scheme.getApplicationInfo().getApplicationEndDate().toString()
                                : null))
                .sourceUrl(scheme.getSource() != null ? scheme.getSource().getSourceUrl() : null)
                .applicationUrl(scheme.getApplicationInfo() != null ? scheme.getApplicationInfo().getApplicationUrl() : null)
                .build();
    }
}
