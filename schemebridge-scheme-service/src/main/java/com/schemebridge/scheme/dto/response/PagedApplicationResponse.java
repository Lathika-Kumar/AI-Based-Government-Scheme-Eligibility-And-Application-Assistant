package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedApplicationResponse {
    private List<ApplicationResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
