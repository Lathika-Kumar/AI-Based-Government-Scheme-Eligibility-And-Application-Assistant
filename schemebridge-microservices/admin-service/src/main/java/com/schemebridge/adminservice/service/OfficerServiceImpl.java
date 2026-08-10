package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.OfficerRequest;
import com.schemebridge.adminservice.dto.OfficerResponse;
import com.schemebridge.adminservice.dto.OfficerStatusUpdateRequest;
import com.schemebridge.adminservice.entity.Officer;
import com.schemebridge.adminservice.enums.AdminActionType;
import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.enums.OfficerStatus;
import com.schemebridge.adminservice.repository.OfficerRepository;
import com.schemebridge.common.exception.BadRequestException;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfficerServiceImpl implements OfficerService {

    private final OfficerRepository officerRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public OfficerResponse createOfficer(String actorEmail, OfficerRequest request) {
        log.info("Creating new officer: {} ({}) by {}", request.getFullName(), request.getEmail(), actorEmail);

        if (officerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Officer with email " + request.getEmail() + " already exists.");
        }

        String officerId = AdminConstants.OFFICER_ID_PREFIX + UUID.randomUUID().toString().substring(0, 13).toUpperCase();

        Officer officer = Officer.builder()
                .officerId(officerId)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .status(request.getStatus() != null ? request.getStatus() : OfficerStatus.ACTIVE)
                .department(request.getDepartment())
                .jurisdictionState(request.getJurisdictionState())
                .jurisdictionDistrict(request.getJurisdictionDistrict())
                .build();

        Officer saved = officerRepository.save(officer);

        auditLogService.logActivity(actorEmail, AdminActionType.OFFICER_CREATED, "Officer", saved.getOfficerId(),
                "Created officer " + saved.getFullName() + " with role " + saved.getRole());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public OfficerResponse updateOfficer(String actorEmail, String officerId, OfficerRequest request) {
        Officer officer = findOfficerEntity(officerId);

        officer.setFullName(request.getFullName());
        officer.setPhoneNumber(request.getPhoneNumber());
        if (request.getRole() != null) officer.setRole(request.getRole());
        if (request.getStatus() != null) officer.setStatus(request.getStatus());
        officer.setDepartment(request.getDepartment());
        officer.setJurisdictionState(request.getJurisdictionState());
        officer.setJurisdictionDistrict(request.getJurisdictionDistrict());

        Officer updated = officerRepository.save(officer);

        auditLogService.logActivity(actorEmail, AdminActionType.OFFICER_UPDATED, "Officer", updated.getOfficerId(),
                "Updated officer details for " + updated.getFullName());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public OfficerResponse updateOfficerStatus(String actorEmail, String officerId, OfficerStatusUpdateRequest request) {
        Officer officer = findOfficerEntity(officerId);
        officer.setStatus(request.getStatus());
        Officer updated = officerRepository.save(officer);

        auditLogService.logActivity(actorEmail, AdminActionType.OFFICER_STATUS_CHANGED, "Officer", updated.getOfficerId(),
                "Changed officer status to " + request.getStatus());

        return mapToResponse(updated);
    }

    @Override
    public OfficerResponse getOfficerById(String officerId) {
        return mapToResponse(findOfficerEntity(officerId));
    }

    @Override
    public Page<OfficerResponse> getAllOfficers(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return officerRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<OfficerResponse> getOfficersByRole(OfficerRole role, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return officerRepository.findByRoleAndActiveTrue(role, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<OfficerResponse> searchOfficers(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return officerRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndActiveTrue(query, query, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteOfficer(String actorEmail, String officerId) {
        Officer officer = findOfficerEntity(officerId);
        officer.setActive(false);
        officer.setStatus(OfficerStatus.DEACTIVATED);
        officerRepository.save(officer);

        auditLogService.logActivity(actorEmail, AdminActionType.OFFICER_STATUS_CHANGED, "Officer", officerId,
                "Soft-deleted officer " + officer.getFullName());
    }

    private Officer findOfficerEntity(String officerId) {
        return officerRepository.findByOfficerId(officerId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_OFFICER_NOT_FOUND + officerId));
    }

    private OfficerResponse mapToResponse(Officer officer) {
        return OfficerResponse.builder()
                .id(officer.getId())
                .officerId(officer.getOfficerId())
                .fullName(officer.getFullName())
                .email(officer.getEmail())
                .phoneNumber(officer.getPhoneNumber())
                .role(officer.getRole())
                .status(officer.getStatus())
                .department(officer.getDepartment())
                .jurisdictionState(officer.getJurisdictionState())
                .jurisdictionDistrict(officer.getJurisdictionDistrict())
                .lastLoginAt(officer.getLastLoginAt())
                .createdAt(officer.getCreatedAt())
                .updatedAt(officer.getUpdatedAt())
                .build();
    }
}
