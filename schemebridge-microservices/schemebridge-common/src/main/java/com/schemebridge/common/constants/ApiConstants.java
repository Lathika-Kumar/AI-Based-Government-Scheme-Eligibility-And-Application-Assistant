package com.schemebridge.common.constants;

public final class ApiConstants {

    private ApiConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String API_V1_PREFIX = "/api/v1";
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
}
