package com.schemebridge.service.impl;

import com.schemebridge.dto.CategoryResponse;
import com.schemebridge.dto.EligibilityCheckResponse;
import com.schemebridge.dto.SchemeCardResponse;
import com.schemebridge.dto.SchemeRecommendationResponse;
import com.schemebridge.dto.SchemeRequest;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.SchemeAuditLog;
import com.schemebridge.entity.User;
import com.schemebridge.enums.SchemeAuditAction;
import com.schemebridge.enums.SchemeStatus;
import com.schemebridge.enums.SchemeType;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.SchemeMapper;
import com.schemebridge.repository.CitizenProfileRepository;
import com.schemebridge.repository.SchemeAuditLogRepository;
import com.schemebridge.repository.SchemeRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.SchemeService;
import com.schemebridge.util.EligibilityEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeServiceImpl implements SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final SchemeMapper schemeMapper;
    private final EligibilityEngine eligibilityEngine;
    private final MongoTemplate mongoTemplate;

    @Override
    public SchemeResponse createScheme(SchemeRequest request, String adminEmail) {
        log.info("Creating new scheme: {} by admin: {}", request.getSchemeName(), adminEmail);

        if (schemeRepository.findBySchemeCode(request.getSchemeCode().trim().toUpperCase()).isPresent()) {
            throw new BadRequestException("Scheme code already exists: " + request.getSchemeCode());
        }

        Scheme scheme = schemeMapper.toEntity(request, adminEmail);
        Scheme saved = schemeRepository.save(scheme);

        // Audit Log
        SchemeAuditLog auditLog = SchemeAuditLog.builder()
                .schemeId(saved.getId())
                .action(SchemeAuditAction.CREATE)
                .updatedFields(new HashSet<>(List.of("schemeName", "schemeCode", "status")))
                .updatedBy(adminEmail)
                .updatedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return schemeMapper.toSchemeResponse(saved);
    }

    @Override
    public SchemeResponse updateScheme(String id, SchemeRequest request, String adminEmail) {
        log.info("Updating scheme ID: {} by admin: {}", id, adminEmail);
        Scheme scheme = findSchemeById(id);

        scheme.setSchemeName(request.getSchemeName().trim());
        scheme.setDescription(request.getDescription());
        scheme.setShortDescription(request.getShortDescription());
        scheme.setBenefits(request.getBenefits());
        scheme.setCategory(request.getCategory());
        scheme.setSubcategory(request.getSubcategory());
        scheme.setSchemeType(request.getSchemeType());
        scheme.setApplicableStates(request.getApplicableStates());
        scheme.setMinimumAge(request.getMinimumAge());
        scheme.setMaximumAge(request.getMaximumAge());
        scheme.setMinimumIncome(request.getMinimumIncome());
        scheme.setMaximumIncome(request.getMaximumIncome());
        scheme.setAllowedGenders(request.getAllowedGenders());
        scheme.setAllowedCategories(request.getAllowedCategories());
        scheme.setAllowedOccupations(request.getAllowedOccupations());
        scheme.setRequiredDocuments(request.getRequiredDocuments());
        scheme.setWebsiteUrl(request.getWebsiteUrl());
        scheme.setApplicationMode(request.getApplicationMode());
        scheme.setApplicationStartDate(request.getApplicationStartDate());
        scheme.setApplicationEndDate(request.getApplicationEndDate());
        scheme.setAlwaysOpen(request.getAlwaysOpen() != null ? request.getAlwaysOpen() : true);
        scheme.setLaunchDate(request.getLaunchDate());
        scheme.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        scheme.setPriority(request.getPriority() != null ? request.getPriority() : 1);
        scheme.setThumbnailUrl(request.getThumbnailUrl());
        scheme.setGalleryImages(request.getGalleryImages());
        scheme.setTags(request.getTags());
        scheme.setMaxBeneficiaries(request.getMaxBeneficiaries());

        if (request.getStatus() != null) {
            scheme.setStatus(request.getStatus());
        }

        scheme.setSchemeVersion((scheme.getSchemeVersion() != null ? scheme.getSchemeVersion() : 1) + 1);
        scheme.setLastUpdatedAt(LocalDateTime.now());
        scheme.setLastUpdatedBy(adminEmail);

        Scheme updated = schemeRepository.save(scheme);

        // Audit Log
        SchemeAuditLog auditLog = SchemeAuditLog.builder()
                .schemeId(updated.getId())
                .action(SchemeAuditAction.UPDATE)
                .updatedFields(new HashSet<>(List.of("all_updated_fields")))
                .updatedBy(adminEmail)
                .updatedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return schemeMapper.toSchemeResponse(updated);
    }

    @Override
    public void softDeleteScheme(String id, String adminEmail) {
        log.info("Soft deleting scheme ID: {} by admin: {}", id, adminEmail);
        Scheme scheme = findSchemeById(id);
        scheme.setDeleted(true);
        scheme.setDeletedAt(LocalDateTime.now());
        scheme.setDeletedBy(adminEmail);
        scheme.setStatus(SchemeStatus.ARCHIVED);
        scheme.setSchemeVersion((scheme.getSchemeVersion() != null ? scheme.getSchemeVersion() : 1) + 1);
        scheme.setLastUpdatedAt(LocalDateTime.now());
        scheme.setLastUpdatedBy(adminEmail);
        schemeRepository.save(scheme);

        SchemeAuditLog auditLog = SchemeAuditLog.builder()
                .schemeId(id)
                .action(SchemeAuditAction.ARCHIVE)
                .updatedFields(new HashSet<>(List.of("deleted", "status")))
                .updatedBy(adminEmail)
                .updatedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    @Override
    public SchemeResponse publishScheme(String id, String adminEmail) {
        log.info("Publishing scheme ID: {} by admin: {}", id, adminEmail);
        Scheme scheme = findSchemeById(id);
        scheme.setStatus(SchemeStatus.PUBLISHED);
        scheme.setSchemeVersion((scheme.getSchemeVersion() != null ? scheme.getSchemeVersion() : 1) + 1);
        scheme.setLastUpdatedAt(LocalDateTime.now());
        scheme.setLastUpdatedBy(adminEmail);
        Scheme saved = schemeRepository.save(scheme);

        SchemeAuditLog auditLog = SchemeAuditLog.builder()
                .schemeId(id)
                .action(SchemeAuditAction.PUBLISH)
                .updatedFields(new HashSet<>(List.of("status")))
                .updatedBy(adminEmail)
                .updatedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return schemeMapper.toSchemeResponse(saved);
    }

    @Override
    public SchemeResponse getScheme(String id) {
        Scheme scheme = schemeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + id));

        if (scheme.getStatus() != SchemeStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Scheme is not published.");
        }

        // Increment view count
        scheme.setViewCount((scheme.getViewCount() != null ? scheme.getViewCount() : 0L) + 1L);
        schemeRepository.save(scheme);

        return schemeMapper.toSchemeResponse(scheme);
    }

    @Override
    public SchemeResponse getSchemePreview(String id) {
        Scheme scheme = findSchemeById(id);
        return schemeMapper.toSchemeResponse(scheme);
    }

    @Override
    public Page<SchemeCardResponse> listSchemes(String category, SchemeType type, String state, Boolean featured, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(SchemeStatus.PUBLISHED));
        query.addCriteria(Criteria.where("deleted").is(false));

        if (category != null && !category.isBlank()) {
            query.addCriteria(Criteria.where("category").is(category.trim()));
        }
        if (type != null) {
            query.addCriteria(Criteria.where("schemeType").is(type));
        }
        if (featured != null) {
            query.addCriteria(Criteria.where("featured").is(featured));
        }
        if (state != null && !state.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("schemeType").is(SchemeType.CENTRAL),
                    Criteria.where("applicableStates").is(state.trim()),
                    Criteria.where("applicableStates").is("All India")
            ));
        }

        long total = mongoTemplate.count(query, Scheme.class);
        query.with(pageable);
        List<Scheme> schemes = mongoTemplate.find(query, Scheme.class);

        List<SchemeCardResponse> cards = schemes.stream()
                .map(schemeMapper::toSchemeCardResponse)
                .toList();

        return new PageImpl<>(cards, pageable, total);
    }

    @Override
    public List<CategoryResponse> getCategories() {
        List<Scheme> publishedSchemes = schemeRepository.findByStatusAndDeletedFalse(SchemeStatus.PUBLISHED);
        Map<String, Long> categoryCounts = publishedSchemes.stream()
                .filter(s -> s.getCategory() != null)
                .collect(groupingBy(Scheme::getCategory, counting()));

        return categoryCounts.entrySet().stream()
                .map(e -> CategoryResponse.builder()
                        .category(e.getKey())
                        .count(e.getValue())
                        .build())
                .sorted(Comparator.comparing(CategoryResponse::getCategory))
                .toList();
    }

    @Override
    public List<SchemeCardResponse> getFeaturedSchemes() {
        Page<Scheme> page = schemeRepository.findByFeaturedTrueAndStatusAndDeletedFalse(SchemeStatus.PUBLISHED, PageRequest.of(0, 10));
        return page.getContent().stream()
                .map(schemeMapper::toSchemeCardResponse)
                .toList();
    }

    @Override
    public List<SchemeCardResponse> getTrendingSchemes() {
        List<Scheme> trending = schemeRepository.findTop10ByStatusAndDeletedFalseOrderByRecommendationCountDescViewCountDesc(SchemeStatus.PUBLISHED);
        return trending.stream()
                .map(schemeMapper::toSchemeCardResponse)
                .toList();
    }

    @Override
    public List<SchemeCardResponse> getLatestSchemes() {
        List<Scheme> latest = schemeRepository.findTop10ByStatusAndDeletedFalseOrderByCreatedAtDesc(SchemeStatus.PUBLISHED);
        return latest.stream()
                .map(schemeMapper::toSchemeCardResponse)
                .toList();
    }

    @Override
    public Page<SchemeCardResponse> searchSchemes(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return listSchemes(null, null, null, null, pageable);
        }
        Page<Scheme> schemes = schemeRepository.searchPublishedSchemes(SchemeStatus.PUBLISHED, query.trim(), pageable);
        List<SchemeCardResponse> cards = schemes.getContent().stream()
                .map(schemeMapper::toSchemeCardResponse)
                .toList();
        return new PageImpl<>(cards, pageable, schemes.getTotalElements());
    }

    @Override
    public List<SchemeRecommendationResponse> getRecommendations(String userEmail) {
        log.info("Generating personalized scheme recommendations for user: {}", userEmail);
        User user = getUserByEmail(userEmail);
        CitizenProfile profile = getOrCreateProfile(user.getId());

        List<Scheme> publishedSchemes = schemeRepository.findByStatusAndDeletedFalse(SchemeStatus.PUBLISHED);
        LocalDate today = LocalDate.now();

        List<SchemeRecommendationResponse> recommendations = new ArrayList<>();

        for (Scheme scheme : publishedSchemes) {
            // Check expiry & beneficiary quota
            if (Boolean.FALSE.equals(scheme.getAlwaysOpen()) && scheme.getApplicationEndDate() != null && scheme.getApplicationEndDate().isBefore(today)) {
                continue;
            }
            if (scheme.getMaxBeneficiaries() != null && scheme.getCurrentBeneficiaries() != null
                    && scheme.getCurrentBeneficiaries() >= scheme.getMaxBeneficiaries()) {
                continue;
            }

            SchemeCardResponse card = schemeMapper.toSchemeCardResponse(scheme);
            SchemeRecommendationResponse rec = eligibilityEngine.evaluateRecommendation(user, profile, scheme, card);
            recommendations.add(rec);
        }

        // Sort descending by priorityScore
        recommendations.sort((r1, r2) -> Double.compare(r2.getPriorityScore(), r1.getPriorityScore()));

        // Increment recommendation count for top 5 schemes
        recommendations.stream().limit(5).forEach(rec -> {
            if (rec.getScheme() != null && rec.getScheme().getId() != null) {
                schemeRepository.findById(rec.getScheme().getId()).ifPresent(s -> {
                    s.setRecommendationCount((s.getRecommendationCount() != null ? s.getRecommendationCount() : 0L) + 1L);
                    schemeRepository.save(s);
                });
            }
        });

        return recommendations;
    }

    @Override
    public EligibilityCheckResponse checkEligibility(String schemeId, String userEmail) {
        log.info("Running detailed eligibility check for user: {} on scheme: {}", userEmail, schemeId);
        User user = getUserByEmail(userEmail);
        CitizenProfile profile = getOrCreateProfile(user.getId());
        Scheme scheme = schemeRepository.findByIdAndDeletedFalse(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + schemeId));

        return eligibilityEngine.evaluateEligibility(user, profile, scheme);
    }

    private Scheme findSchemeById(String id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private CitizenProfile getOrCreateProfile(String userId) {
        return citizenProfileRepository.findByUserId(userId)
                .orElseGet(() -> CitizenProfile.builder()
                        .userId(userId)
                        .profileVersion(1)
                        .deleted(false)
                        .build());
    }
}
