package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.ApplicationDocument;
import com.schemebridge.scheme.document.DocumentVerificationStatus;
import com.schemebridge.scheme.dto.response.ApplicationDocumentResponse;
import com.schemebridge.scheme.dto.response.PagedApplicationDocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
@Tag(name = "Admin Document Repository", description = "Administrative endpoints for cross-application document verification repository")
@SecurityRequirement(name = "BearerAuth")
public class AdminDocumentController {

    private final MongoTemplate mongoTemplate;

    @GetMapping
    @Operation(summary = "Get paginated cross-application document repository queue (Admin only)")
    public ResponseEntity<PagedApplicationDocumentResponse> getDocumentsQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String schemeCode,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadedAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Only include uploaded documents
        criteriaList.add(Criteria.where("uploaded").is(true));

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                criteriaList.add(Criteria.where("verificationStatus").is(DocumentVerificationStatus.valueOf(status.trim().toUpperCase())));
            } catch (IllegalArgumentException ignored) {}
        }

        if (schemeCode != null && !schemeCode.isBlank() && !"all".equalsIgnoreCase(schemeCode)) {
            criteriaList.add(Criteria.where("schemeCode").is(schemeCode.trim()));
        }

        if (search != null && !search.isBlank()) {
            String term = search.trim();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("fileName").regex(term, "i"),
                    Criteria.where("documentCode").regex(term, "i"),
                    Criteria.where("documentName").regex(term, "i"),
                    Criteria.where("applicationId").regex(term, "i"),
                    Criteria.where("userId").regex(term, "i")
            ));
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));

        long totalElements = mongoTemplate.count(query, ApplicationDocument.class);

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction != null ? direction : "DESC"), sort != null ? sort : "uploadedAt");
        Pageable pageable = PageRequest.of(page, size, sortObj);
        query.with(pageable);

        List<ApplicationDocument> docs = mongoTemplate.find(query, ApplicationDocument.class);
        List<ApplicationDocumentResponse> content = docs.stream()
                .map(d -> ApplicationDocumentResponse.builder()
                        .id(d.getId())
                        .applicationId(d.getApplicationId())
                        .documentCode(d.getDocumentCode())
                        .documentName(d.getDocumentName())
                        .schemeCode(d.getSchemeCode())
                        .mandatory(d.isMandatory())
                        .uploaded(d.isUploaded())
                        .fileName(d.getFileName())
                        .contentType(d.getContentType())
                        .fileSize(d.getFileSize())
                        .uploadedAt(d.getUploadedAt())
                        .verifiedAt(d.getVerifiedAt())
                        .rejectedAt(d.getRejectedAt())
                        .version(d.getVersion())
                        .downloadUrl("/api/applications/" + d.getApplicationId() + "/documents/" + d.getDocumentCode() + "/download")
                        .rejectionReason(d.getRejectionReason())
                        .verificationStatus(d.getVerificationStatus() != null ? d.getVerificationStatus().name() : null)
                        .build())
                .collect(Collectors.toList());

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        PagedApplicationDocumentResponse response = PagedApplicationDocumentResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
