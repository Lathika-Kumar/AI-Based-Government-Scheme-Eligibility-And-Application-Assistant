package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.AssignGrievanceRequest;
import com.schemebridge.scheme.dto.request.CreateGrievanceRequest;
import com.schemebridge.scheme.dto.request.GrievanceReplyRequest;
import com.schemebridge.scheme.dto.request.ResolveGrievanceRequest;
import com.schemebridge.scheme.dto.response.GrievanceResponse;
import com.schemebridge.scheme.dto.response.PagedGrievanceResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.GrievanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationService notificationService;
    private final AdminAuditService adminAuditService;

    @Transactional
    public GrievanceResponse createGrievance(CreateGrievanceRequest request, String userId) {
        String grievanceNumber = "GRV-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));
        String subject = (request.getSubject() != null && !request.getSubject().isBlank())
                ? request.getSubject()
                : request.getCategory();

        GrievanceTimelineEntry initialEntry = GrievanceTimelineEntry.builder()
                .id(UUID.randomUUID().toString())
                .authorId(userId)
                .authorRole("ROLE_USER")
                .action("GRIEVANCE_CREATED")
                .message("Grievance lodged: " + subject)
                .timestamp(Instant.now())
                .internalOnly(false)
                .build();

        Grievance grievance = Grievance.builder()
                .grievanceNumber(grievanceNumber)
                .userId(userId)
                .applicationId(request.getApplicationId())
                .schemeCode(request.getSchemeCode())
                .category(request.getCategory())
                .subject(subject)
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : GrievancePriority.MEDIUM)
                .status(GrievanceStatus.OPEN)
                .timeline(new ArrayList<>(List.of(initialEntry)))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Grievance saved = grievanceRepository.save(grievance);

        // Notify Admins of new grievance
        notificationService.sendNotification(
                null,
                "ROLE_ADMIN",
                NotificationType.GRIEVANCE_CREATED,
                "New Grievance Lodged: " + grievanceNumber,
                "Citizen lodged a grievance under " + request.getCategory() + ": " + request.getSubject(),
                "IN_APP",
                "GRIEVANCE",
                saved.getId(),
                userId,
                Map.of("grievanceNumber", grievanceNumber, "category", request.getCategory())
        );

        log.info("Created grievance: id={}, number={}, userId={}", saved.getId(), grievanceNumber, userId);
        return toResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public List<GrievanceResponse> getMyGrievances(String userId) {
        List<Grievance> list = grievanceRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(g -> toResponse(g, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GrievanceResponse getGrievanceById(String id, String userId, boolean isPrivileged) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with ID: " + id));

        if (!isPrivileged && !g.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access to grievance record");
        }

        return toResponse(g, isPrivileged);
    }

    @Transactional
    public GrievanceResponse replyToGrievance(String id, GrievanceReplyRequest request, String authorId, String authorRole) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with ID: " + id));

        boolean isPrivileged = "ROLE_ADMIN".equals(authorRole) || "ROLE_SCHEME_MANAGER".equals(authorRole) || "ROLE_VERIFICATION_OFFICER".equals(authorRole);
        if (!isPrivileged) {
            if (!g.getUserId().equals(authorId)) {
                throw new SecurityException("Unauthorized to reply to this grievance");
            }
            if (g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED || g.getStatus() == GrievanceStatus.REJECTED) {
                throw new IllegalStateException("This grievance has been resolved. Further replies are closed.");
            }
        }

        GrievanceTimelineEntry entry = GrievanceTimelineEntry.builder()
                .id(UUID.randomUUID().toString())
                .authorId(authorId)
                .authorRole(authorRole)
                .action(isPrivileged ? "ADMIN_REPLY" : "CITIZEN_REPLY")
                .message(request.getMessage())
                .timestamp(Instant.now())
                .internalOnly(isPrivileged && request.isInternalOnly())
                .build();

        g.getTimeline().add(entry);
        g.setUpdatedAt(Instant.now());

        if (isPrivileged) {
            g.setStatus(GrievanceStatus.RESOLVED);
            g.setResolution(request.getMessage());
            g.setResolvedAt(Instant.now());

            GrievanceTimelineEntry resolvedEntry = GrievanceTimelineEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .authorId(authorId)
                    .authorRole(authorRole)
                    .action("RESOLVED")
                    .message("Resolution: " + request.getMessage())
                    .timestamp(Instant.now())
                    .internalOnly(false)
                    .build();
            g.getTimeline().add(resolvedEntry);

            // Notify citizen
            if (!request.isInternalOnly()) {
                notificationService.sendNotification(
                        g.getUserId(),
                        null,
                        NotificationType.GRIEVANCE_UPDATED,
                        "Grievance Resolved: " + g.getGrievanceNumber(),
                        "Your grievance has been resolved: " + (request.getMessage().length() > 80 ? request.getMessage().substring(0, 77) + "..." : request.getMessage()),
                        "IN_APP",
                        "GRIEVANCE",
                        g.getId(),
                        authorId,
                        Map.of("grievanceNumber", g.getGrievanceNumber(), "resolution", request.getMessage())
                );
            }

            adminAuditService.recordAction(authorId, authorRole, "GRIEVANCE_RESOLVED", "GRIEVANCE", id,
                    Map.of("status", "IN_PROGRESS"),
                    Map.of("status", "RESOLVED", "resolution", request.getMessage()),
                    null, null, Map.of("grievanceNumber", g.getGrievanceNumber()));
        } else {
            // Citizen replied, change to IN_PROGRESS if was waiting
            if (g.getStatus() == GrievanceStatus.WAITING_FOR_CITIZEN) {
                g.setStatus(GrievanceStatus.IN_PROGRESS);
            }
            // Notify admin / assigned officer
            notificationService.sendNotification(
                    g.getAssignedTo(),
                    "ROLE_ADMIN",
                    NotificationType.GRIEVANCE_UPDATED,
                    "Citizen Reply on Grievance " + g.getGrievanceNumber(),
                    request.getMessage(),
                    "IN_APP",
                    "GRIEVANCE",
                    g.getId(),
                    authorId,
                    Map.of("grievanceNumber", g.getGrievanceNumber())
            );
        }

        Grievance saved = grievanceRepository.save(g);
        return toResponse(saved, isPrivileged);
    }

    @Transactional
    public GrievanceResponse assignGrievance(String id, AssignGrievanceRequest request, String actorId, String actorRole) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with ID: " + id));

        String prevAssigned = g.getAssignedTo();
        g.setAssignedTo(request.getAssignedTo());
        if (g.getStatus() == GrievanceStatus.OPEN) {
            g.setStatus(GrievanceStatus.IN_PROGRESS);
        }
        g.setUpdatedAt(Instant.now());

        GrievanceTimelineEntry entry = GrievanceTimelineEntry.builder()
                .id(UUID.randomUUID().toString())
                .authorId(actorId)
                .authorRole(actorRole)
                .action("ASSIGNED")
                .message("Grievance assigned to officer: " + request.getAssignedTo())
                .timestamp(Instant.now())
                .internalOnly(true)
                .build();
        g.getTimeline().add(entry);

        Grievance saved = grievanceRepository.save(g);

        adminAuditService.recordAction(actorId, actorRole, "GRIEVANCE_ASSIGNED", "GRIEVANCE", id,
                Map.of("assignedTo", prevAssigned != null ? prevAssigned : "none"),
                Map.of("assignedTo", request.getAssignedTo()),
                null, null, Map.of("grievanceNumber", g.getGrievanceNumber()));

        return toResponse(saved, true);
    }

    @Transactional
    public GrievanceResponse resolveGrievance(String id, ResolveGrievanceRequest request, String actorId, String actorRole) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with ID: " + id));

        g.setStatus(GrievanceStatus.RESOLVED);
        g.setResolution(request.getResolution());
        g.setResolvedAt(Instant.now());
        g.setUpdatedAt(Instant.now());

        GrievanceTimelineEntry entry = GrievanceTimelineEntry.builder()
                .id(UUID.randomUUID().toString())
                .authorId(actorId)
                .authorRole(actorRole)
                .action("RESOLVED")
                .message("Resolution: " + request.getResolution())
                .timestamp(Instant.now())
                .internalOnly(false)
                .build();
        g.getTimeline().add(entry);

        Grievance saved = grievanceRepository.save(g);

        // Notify Citizen of resolution
        notificationService.sendNotification(
                g.getUserId(),
                null,
                NotificationType.GRIEVANCE_UPDATED,
                "Grievance Resolved: " + g.getGrievanceNumber(),
                "Your grievance has been resolved: " + request.getResolution(),
                "IN_APP",
                "GRIEVANCE",
                g.getId(),
                actorId,
                Map.of("grievanceNumber", g.getGrievanceNumber(), "resolution", request.getResolution())
        );

        adminAuditService.recordAction(actorId, actorRole, "GRIEVANCE_RESOLVED", "GRIEVANCE", id,
                Map.of("status", "IN_PROGRESS"),
                Map.of("status", "RESOLVED", "resolution", request.getResolution()),
                null, null, Map.of("grievanceNumber", g.getGrievanceNumber()));

        return toResponse(saved, true);
    }

    @Transactional
    public GrievanceResponse closeGrievance(String id, String actorId, String actorRole) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with ID: " + id));

        g.setStatus(GrievanceStatus.CLOSED);
        g.setUpdatedAt(Instant.now());

        GrievanceTimelineEntry entry = GrievanceTimelineEntry.builder()
                .id(UUID.randomUUID().toString())
                .authorId(actorId)
                .authorRole(actorRole)
                .action("CLOSED")
                .message("Grievance closed by administrative officer.")
                .timestamp(Instant.now())
                .internalOnly(false)
                .build();
        g.getTimeline().add(entry);

        Grievance saved = grievanceRepository.save(g);

        adminAuditService.recordAction(actorId, actorRole, "GRIEVANCE_CLOSED", "GRIEVANCE", id,
                null, Map.of("status", "CLOSED"),
                null, null, Map.of("grievanceNumber", g.getGrievanceNumber()));

        return toResponse(saved, true);
    }

    public PagedGrievanceResponse getAdminGrievances(String status, String category, String priority, String assignedTo, String search,
                                                     int page, int size, String sortField, String sortDir) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                criteriaList.add(Criteria.where("status").is(GrievanceStatus.valueOf(status.trim().toUpperCase())));
            } catch (IllegalArgumentException ignored) {}
        }
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            criteriaList.add(Criteria.where("category").regex(category.trim(), "i"));
        }
        if (priority != null && !priority.isBlank() && !"all".equalsIgnoreCase(priority)) {
            try {
                criteriaList.add(Criteria.where("priority").is(GrievancePriority.valueOf(priority.trim().toUpperCase())));
            } catch (IllegalArgumentException ignored) {}
        }
        if (assignedTo != null && !assignedTo.isBlank() && !"all".equalsIgnoreCase(assignedTo)) {
            criteriaList.add(Criteria.where("assignedTo").is(assignedTo.trim()));
        }
        if (search != null && !search.isBlank()) {
            String regex = search.trim();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("grievanceNumber").regex(regex, "i"),
                    Criteria.where("subject").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i"),
                    Criteria.where("userId").regex(regex, "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalElements = mongoTemplate.count(query, Grievance.class);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "DESC"), sortField != null ? sortField : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        query.with(pageable);

        List<Grievance> list = mongoTemplate.find(query, Grievance.class);
        List<GrievanceResponse> content = list.stream()
                .map(g -> toResponse(g, true))
                .collect(Collectors.toList());

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return PagedGrievanceResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private GrievanceResponse toResponse(Grievance g, boolean isPrivileged) {
        List<GrievanceTimelineEntry> visibleTimeline = g.getTimeline().stream()
                .filter(t -> isPrivileged || !t.isInternalOnly())
                .collect(Collectors.toList());

        return GrievanceResponse.builder()
                .id(g.getId())
                .grievanceNumber(g.getGrievanceNumber())
                .userId(g.getUserId())
                .applicationId(g.getApplicationId())
                .schemeCode(g.getSchemeCode())
                .category(g.getCategory())
                .subject(g.getSubject())
                .description(g.getDescription())
                .priority(g.getPriority())
                .status(g.getStatus())
                .assignedTo(g.getAssignedTo())
                .resolution(g.getResolution())
                .resolvedAt(g.getResolvedAt())
                .timeline(visibleTimeline)
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}
