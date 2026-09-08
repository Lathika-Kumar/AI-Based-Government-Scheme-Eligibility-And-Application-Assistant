package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.PagedSchemeResponse;
import com.schemebridge.scheme.dto.response.SchemeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemeSearchService {

    private final MongoTemplate mongoTemplate;
    private final SchemeService schemeService;

    public PagedSchemeResponse search(
            String q,
            String categoryCode,
            String schemeLevel,
            String stateOrUt,
            String beneficiaryType,
            String schemeType,
            String status,
            String tags,
            int page,
            int size,
            String sort,
            String direction
    ) {
        // 1. Validation
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero.");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("Page size must be between 1 and 50.");
        }

        List<String> sortWhitelist = List.of("schemeCode", "slug", "createdAt", "updatedAt", "ministry", "schemeLevel");
        if (!sortWhitelist.contains(sort)) {
            throw new IllegalArgumentException("Sorting by field '" + sort + "' is not supported. Allowed fields: " + sortWhitelist);
        }

        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Sort direction must be either 'asc' or 'desc'.");
        }

        // 2. Query Criteria Construction
        List<Criteria> criteriaList = new ArrayList<>();

        // Role-based Status checks
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = false;
        if (auth != null) {
            isPrivileged = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") 
                                || a.getAuthority().equals("ROLE_SCHEME_MANAGER"));
        }

        List<SchemeStatus> allowedStatuses = new ArrayList<>();
        if (!isPrivileged) {
            if (status != null && !status.trim().isEmpty()) {
                List<SchemeStatus> requested = parseSchemeStatuses(status);
                for (SchemeStatus s : requested) {
                    if (s != SchemeStatus.ACTIVE) {
                        throw new SecurityException("Access denied to requested status: " + s);
                    }
                }
            }
            allowedStatuses.add(SchemeStatus.ACTIVE);
        } else {
            if (status != null && !status.trim().isEmpty()) {
                allowedStatuses.addAll(parseSchemeStatuses(status));
            }
        }

        if (!allowedStatuses.isEmpty()) {
            criteriaList.add(Criteria.where("status").in(allowedStatuses));
        }

        // Text Search (q)
        if (q != null && !q.trim().isEmpty()) {
            String cleanQ = q.trim();
            String regexPattern = ".*" + java.util.regex.Pattern.quote(cleanQ) + ".*";
            
            List<Criteria> textCriteria = new ArrayList<>();
            textCriteria.add(Criteria.where("title.english").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("title.tamil").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("description.english").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("description.tamil").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("shortDescription.english").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("shortDescription.tamil").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("ministry").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("beneficiaryType").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("schemeType").regex(regexPattern, "i"));
            textCriteria.add(Criteria.where("tags").regex(regexPattern, "i"));

            criteriaList.add(new Criteria().orOperator(textCriteria.toArray(new Criteria[0])));
        }

        // Composable filters
        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            List<String> codes = Arrays.stream(categoryCode.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!codes.isEmpty()) {
                criteriaList.add(Criteria.where("category.code").in(codes));
            }
        }

        if (schemeLevel != null && !schemeLevel.trim().isEmpty()) {
            List<SchemeLevel> levels = parseSchemeLevels(schemeLevel);
            if (!levels.isEmpty()) {
                criteriaList.add(Criteria.where("schemeLevel").in(levels));
            }
        }

        if (stateOrUt != null && !stateOrUt.trim().isEmpty()) {
            List<String> states = Arrays.stream(stateOrUt.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!states.isEmpty()) {
                criteriaList.add(Criteria.where("stateOrUt").in(states));
            }
        }

        if (beneficiaryType != null && !beneficiaryType.trim().isEmpty()) {
            List<String> types = Arrays.stream(beneficiaryType.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!types.isEmpty()) {
                criteriaList.add(Criteria.where("beneficiaryType").in(types));
            }
        }

        if (schemeType != null && !schemeType.trim().isEmpty()) {
            List<String> types = Arrays.stream(schemeType.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!types.isEmpty()) {
                criteriaList.add(Criteria.where("schemeType").in(types));
            }
        }

        if (tags != null && !tags.trim().isEmpty()) {
            List<String> tagList = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!tagList.isEmpty()) {
                criteriaList.add(Criteria.where("tags").in(tagList));
            }
        }

        // Apply criteria
        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // 3. Database offset count and pagination execution
        long totalElements = mongoTemplate.count(query, Scheme.class);
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        query.with(PageRequest.of(page, size));
        query.with(Sort.by(direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sort));

        List<Scheme> schemes = mongoTemplate.find(query, Scheme.class);
        List<SchemeResponse> content = schemes.stream()
                .map(schemeService::mapToResponse)
                .collect(Collectors.toList());

        return PagedSchemeResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private List<SchemeLevel> parseSchemeLevels(String input) {
        if (input == null || input.trim().isEmpty()) return List.of();
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return SchemeLevel.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid schemeLevel value: " + s);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<SchemeStatus> parseSchemeStatuses(String input) {
        if (input == null || input.trim().isEmpty()) return List.of();
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return SchemeStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid status value: " + s);
                    }
                })
                .collect(Collectors.toList());
    }
}
