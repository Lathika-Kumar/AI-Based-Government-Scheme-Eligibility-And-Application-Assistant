package com.schemebridge.citizenservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceInfoResponse {
    private String service;
    private String version;
    private Integer port;
    private String database;
    private String status;
    private String buildTime;
    private String environment;
}
