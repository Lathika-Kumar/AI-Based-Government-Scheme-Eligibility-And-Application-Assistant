package com.schemebridge.coreservice.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private Instant buildTime;
    private String environment;
}
