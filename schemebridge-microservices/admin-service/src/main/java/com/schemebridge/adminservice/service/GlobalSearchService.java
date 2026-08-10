package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.GlobalSearchResponse;

public interface GlobalSearchService {

    GlobalSearchResponse searchAll(String query);
}
