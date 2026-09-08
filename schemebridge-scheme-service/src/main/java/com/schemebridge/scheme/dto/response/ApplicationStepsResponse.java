package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStepsResponse {
    private String schemeCode;
    private String schemeTitle;
    private String applicationMode;
    private String officialApplicationUrl;
    private String helplineNumber;
    private List<String> applicationSteps;
    private List<String> requiredDocuments;
    private List<String> benefits;
}
