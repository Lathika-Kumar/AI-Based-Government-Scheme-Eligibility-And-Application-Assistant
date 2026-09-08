package com.schemebridge.scheme.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedSchemeResponse {
    private List<SchemeResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
