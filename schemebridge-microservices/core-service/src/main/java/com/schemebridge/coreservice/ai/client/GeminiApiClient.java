package com.schemebridge.coreservice.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * GeminiApiClient — Non-blocking backend client for Google Gemini REST API.
 * Never logs or exposes API keys or PII.
 */
@Component
@Slf4j
public class GeminiApiClient {

    @Value("${integration.ai.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${integration.ai.model:${GEMINI_MODEL:gemini-1.5-flash}}")
    private String model;

    @Value("${integration.ai.endpoint-url:https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent}")
    private String endpointUrl;


    private final RestTemplate restTemplate;

    public GeminiApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateContent(String promptText) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("placeholder") || apiKey.contains("demo")) {
            log.info("[GeminiApiClient] GEMINI_API_KEY not configured or placeholder used. Falling back cleanly.");
            return null;
        }

        try {
            String url = endpointUrl.replace("{model}", model) + "?key=" + apiKey.trim();

            Map<String, Object> textPart = Map.of("text", promptText);
            Map<String, Object> contentMap = Map.of("parts", List.of(textPart));
            Map<String, Object> payload = Map.of(
                    "contents", List.of(contentMap),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "maxOutputTokens", 1000,
                            "responseMimeType", "application/json"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            Map<?, ?> response = restTemplate.postForObject(url, requestEntity, Map.class);
            if (response != null && response.containsKey("candidates")) {
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                    if (content != null && content.containsKey("parts")) {
                        List<?> parts = (List<?>) content.get("parts");
                        if (!parts.isEmpty()) {
                            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                            return (String) firstPart.get("text");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[GeminiApiClient] Gemini API invocation note: {}", e.getMessage());
        }
        return null;
    }
}
