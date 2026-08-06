package com.schemebridge.mapper;

import com.schemebridge.dto.GrievanceResponse;
import com.schemebridge.entity.Grievance;
import org.springframework.stereotype.Component;

@Component
public class GrievanceMapper {

    public GrievanceResponse toResponse(Grievance grievance) {
        if (grievance == null) {
            return null;
        }

        return GrievanceResponse.builder()
                .id(grievance.getId())
                .userId(grievance.getUserId())
                .subject(grievance.getSubject())
                .description(grievance.getDescription())
                .category(grievance.getCategory())
                .status(grievance.getStatus())
                .resolutionNotes(grievance.getResolutionNotes())
                .createdAt(grievance.getCreatedAt())
                .resolvedAt(grievance.getResolvedAt())
                .build();
    }
}
