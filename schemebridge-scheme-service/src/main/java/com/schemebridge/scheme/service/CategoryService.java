package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.SchemeCategory;
import com.schemebridge.scheme.dto.request.CategoryCreateRequest;
import com.schemebridge.scheme.dto.response.CategoryResponse;
import com.schemebridge.scheme.exception.DuplicateResourceException;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.SchemeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final SchemeCategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Category code already exists: " + request.getCode());
        }

        SchemeCategory category = SchemeCategory.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .status("ACTIVE")
                .build();

        SchemeCategory saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByCode(String code) {
        SchemeCategory category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with code: " + code));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(String status) {
        List<SchemeCategory> categories;
        if (status != null && !status.trim().isEmpty()) {
            categories = categoryRepository.findAllByStatus(status);
        } else {
            categories = categoryRepository.findAll();
        }
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse mapToResponse(SchemeCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
