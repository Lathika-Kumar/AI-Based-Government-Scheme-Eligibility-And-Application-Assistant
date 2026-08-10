package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.OfficerRequest;
import com.schemebridge.adminservice.dto.OfficerResponse;
import com.schemebridge.adminservice.dto.OfficerStatusUpdateRequest;
import com.schemebridge.adminservice.enums.OfficerRole;
import org.springframework.data.domain.Page;

public interface OfficerService {

    OfficerResponse createOfficer(String actorEmail, OfficerRequest request);

    OfficerResponse updateOfficer(String actorEmail, String officerId, OfficerRequest request);

    OfficerResponse updateOfficerStatus(String actorEmail, String officerId, OfficerStatusUpdateRequest request);

    OfficerResponse getOfficerById(String officerId);

    Page<OfficerResponse> getAllOfficers(int page, int size);

    Page<OfficerResponse> getOfficersByRole(OfficerRole role, int page, int size);

    Page<OfficerResponse> searchOfficers(String query, int page, int size);

    void deleteOfficer(String actorEmail, String officerId);
}
