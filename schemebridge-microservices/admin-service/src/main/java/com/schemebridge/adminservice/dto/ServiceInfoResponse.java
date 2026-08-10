package com.schemebridge.adminservice.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceInfoResponse {

    private String serviceName;
    private String version;
    private String port;
    private String status;
    private String database;
    private Map<String, Object> metrics;
}
