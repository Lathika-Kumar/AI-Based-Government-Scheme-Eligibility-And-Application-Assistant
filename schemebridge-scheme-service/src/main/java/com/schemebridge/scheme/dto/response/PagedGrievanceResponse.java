package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedGrievanceResponse {
    private List<GrievanceResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
