package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preferences {
    private String preferredLanguage;
    private String notificationPreference;
    private List<String> preferredSchemeCategories;
    private Boolean voiceAssistantEnabled;
    private Boolean darkMode;
}
