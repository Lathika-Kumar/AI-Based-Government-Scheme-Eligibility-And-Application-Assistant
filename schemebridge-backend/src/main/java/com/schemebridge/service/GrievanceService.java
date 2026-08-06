package com.schemebridge.service;

import com.schemebridge.dto.GrievanceRequest;
import com.schemebridge.dto.GrievanceResponse;

import java.util.List;

public interface GrievanceService {
    GrievanceResponse createGrievance(String userEmail, GrievanceRequest request);
    List<GrievanceResponse> getGrievances(String userEmail);
    List<GrievanceResponse> getAllGrievancesForAdmin();
    GrievanceResponse resolveGrievance(String id, String adminEmail, String resolutionNotes);
}
