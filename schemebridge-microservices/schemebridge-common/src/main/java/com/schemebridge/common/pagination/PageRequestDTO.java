package com.schemebridge.common.pagination;

import com.schemebridge.common.constants.ApiConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDTO {

    @Builder.Default
    private int page = Integer.parseInt(ApiConstants.DEFAULT_PAGE_NUMBER);

    @Builder.Default
    private int size = Integer.parseInt(ApiConstants.DEFAULT_PAGE_SIZE);

    @Builder.Default
    private String sortBy = ApiConstants.DEFAULT_SORT_BY;

    @Builder.Default
    private String sortDirection = ApiConstants.DEFAULT_SORT_DIRECTION;
}
