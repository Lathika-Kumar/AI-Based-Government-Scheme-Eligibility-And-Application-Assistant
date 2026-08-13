package com.schemebridge.coreservice.application.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceInfoResponse {
    private String service;
    private String version;
    private int port;
    private String database;
    private String status;
    private Instant buildTime;
    private String environment;
}
