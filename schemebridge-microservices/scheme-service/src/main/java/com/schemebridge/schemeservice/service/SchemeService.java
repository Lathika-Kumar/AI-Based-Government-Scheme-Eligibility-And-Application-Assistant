package com.schemebridge.schemeservice.service;

import com.schemebridge.schemeservice.dto.*;
import com.schemebridge.schemeservice.entity.*;
import com.schemebridge.schemeservice.repository.*;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeCategoryRepository categoryRepository;
    private final SchemeDepartmentRepository departmentRepository;

    @Transactional
    public SchemeResponse createScheme(SchemeRequest request) {
        Scheme scheme = Scheme.builder()
                .schemeCode(request.getSchemeCode())
                .titleEnglish(request.getTitleEnglish())
                .titleTamil(request.getTitleTamil())
                .descriptionEnglish(request.getDescriptionEnglish())
                .descriptionTamil(request.getDescriptionTamil())
                .schemeType(request.getSchemeType())
                .launchYear(request.getLaunchYear())
                .schemeUrl(request.getSchemeUrl())
                .applicationUrl(request.getApplicationUrl())
                .helplineNumber(request.getHelplineNumber())
                .stateSpecific(request.getStateSpecific())
                .applicableStates(request.getApplicableStates())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .priority(request.getPriority())
                .popularityScore(request.getPopularityScore())
                .featured(request.getFeatured())
                .newlyAdded(request.getNewlyAdded())
                .build();

        if (request.getCategoryCode() != null) {
            categoryRepository.findByCode(request.getCategoryCode()).ifPresent(scheme::setCategory);
        }
        if (request.getDepartmentCode() != null) {
            departmentRepository.findByCode(request.getDepartmentCode()).ifPresent(scheme::setDepartment);
        }

        Scheme saved = schemeRepository.save(scheme);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public SchemeResponse getSchemeById(String id) {
        Scheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", "id", id));
        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public SchemeResponse getSchemeByCode(String schemeCode) {
        Scheme scheme = schemeRepository.findBySchemeCodeAndStatus(schemeCode, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", "schemeCode", schemeCode));
        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public Page<SchemeResponse> searchSchemes(String keyword, String categoryCode, String schemeType,
                                              Boolean featured, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return schemeRepository.searchSchemes(keyword, categoryCode, schemeType, featured, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<SchemeResponse> getFeaturedSchemes() {
        return schemeRepository.findByStatusAndFeaturedTrue("ACTIVE")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SchemeResponse> getNewlyAddedSchemes() {
        return schemeRepository.findByStatusAndNewlyAddedTrue("ACTIVE")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SchemeResponse> getAllActiveSchemes() {
        return schemeRepository.findByStatusOrderByPriorityAscPopularityScoreDesc("ACTIVE")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SchemeResponse mapToResponse(Scheme s) {
        List<EligibilityRuleResponse> rules = s.getEligibilityRules() != null
                ? s.getEligibilityRules().stream()
                    .filter(r -> "ACTIVE".equals(r.getStatus()))
                    .map(r -> EligibilityRuleResponse.builder()
                            .id(r.getId())
                            .ruleType(r.getRuleType())
                            .operator(r.getOperator())
                            .valueString(r.getValueString())
                            .valueNumberMin(r.getValueNumberMin())
                            .valueNumberMax(r.getValueNumberMax())
                            .description(r.getDescription())
                            .mandatory(r.getMandatory())
                            .displayOrder(r.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList())
                : List.of();

        List<BenefitResponse> benefits = s.getBenefits() != null
                ? s.getBenefits().stream()
                    .filter(b -> "ACTIVE".equals(b.getStatus()))
                    .map(b -> BenefitResponse.builder()
                            .id(b.getId()).benefitType(b.getBenefitType()).title(b.getTitle())
                            .descriptionEnglish(b.getDescriptionEnglish()).descriptionTamil(b.getDescriptionTamil())
                            .amountMin(b.getAmountMin()).amountMax(b.getAmountMax())
                            .frequency(b.getFrequency()).currency(b.getCurrency()).displayOrder(b.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList())
                : List.of();

        List<DocumentRequirementResponse> docs = s.getDocuments() != null
                ? s.getDocuments().stream()
                    .filter(d -> "ACTIVE".equals(d.getStatus()))
                    .map(d -> DocumentRequirementResponse.builder()
                            .id(d.getId()).documentName(d.getDocumentName())
                            .documentType(d.getDocumentType()).description(d.getDescription())
                            .mandatory(d.getMandatory()).displayOrder(d.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList())
                : List.of();

        List<String> tags = s.getTags() != null
                ? s.getTags().stream().map(SchemeTag::getTag).collect(Collectors.toList())
                : List.of();

        List<FaqResponse> faqs = s.getFaqs() != null
                ? s.getFaqs().stream()
                    .filter(f -> "ACTIVE".equals(f.getStatus()))
                    .map(f -> FaqResponse.builder()
                            .id(f.getId()).questionEnglish(f.getQuestionEnglish())
                            .questionTamil(f.getQuestionTamil()).answerEnglish(f.getAnswerEnglish())
                            .answerTamil(f.getAnswerTamil()).displayOrder(f.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList())
                : List.of();

        return SchemeResponse.builder()
                .id(s.getId())
                .schemeCode(s.getSchemeCode())
                .titleEnglish(s.getTitleEnglish())
                .titleTamil(s.getTitleTamil())
                .descriptionEnglish(s.getDescriptionEnglish())
                .descriptionTamil(s.getDescriptionTamil())
                .categoryCode(s.getCategory() != null ? s.getCategory().getCode() : null)
                .categoryName(s.getCategory() != null ? s.getCategory().getName() : null)
                .departmentName(s.getDepartment() != null ? s.getDepartment().getName() : null)
                .ministry(s.getDepartment() != null ? s.getDepartment().getMinistry() : null)
                .schemeType(s.getSchemeType())
                .launchYear(s.getLaunchYear())
                .schemeUrl(s.getSchemeUrl())
                .applicationUrl(s.getApplicationUrl())
                .helplineNumber(s.getHelplineNumber())
                .stateSpecific(s.getStateSpecific())
                .applicableStates(s.getApplicableStates())
                .version(s.getVersion())
                .effectiveFrom(s.getEffectiveFrom())
                .effectiveTo(s.getEffectiveTo())
                .priority(s.getPriority())
                .popularityScore(s.getPopularityScore())
                .featured(s.getFeatured())
                .newlyAdded(s.getNewlyAdded())
                .status(s.getStatus())
                .eligibilityRules(rules)
                .benefits(benefits)
                .documents(docs)
                .tags(tags)
                .faqs(faqs)
                .build();
    }
}
