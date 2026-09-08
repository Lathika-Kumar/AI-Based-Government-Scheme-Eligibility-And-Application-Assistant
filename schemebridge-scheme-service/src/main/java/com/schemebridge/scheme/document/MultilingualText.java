package com.schemebridge.scheme.document;

import lombok.*;
import java.util.Map;
import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultilingualText {
    private String english;
    private String tamil;
    
    @Builder.Default
    private Map<String, String> translations = new HashMap<>();

    public String getTranslation(String langCode) {
        if ("en".equalsIgnoreCase(langCode)) return english;
        if ("ta".equalsIgnoreCase(langCode)) return tamil;
        return translations != null ? translations.get(langCode) : null;
    }
}
