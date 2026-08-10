package com.schemebridge.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationTemplateEngine {

    public String render(String templateText, Map<String, String> variables) {
        if (templateText == null || templateText.trim().isEmpty()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return templateText;
        }

        String rendered = templateText;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String replacement = entry.getValue() != null ? entry.getValue() : "";
            rendered = rendered.replace(placeholder, replacement);
        }
        return rendered;
    }
}
