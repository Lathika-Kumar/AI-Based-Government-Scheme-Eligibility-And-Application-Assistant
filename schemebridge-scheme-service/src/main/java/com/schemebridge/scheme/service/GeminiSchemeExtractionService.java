package com.schemebridge.scheme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.dto.response.SchemeDraftResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiSchemeExtractionService {

    private final String apiKey;
    private final String modelName;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiSchemeExtractionService(
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:${GEMINI_MODEL:gemini-1.5-flash}}") String modelName,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.modelName = modelName != null && !modelName.isBlank() ? modelName.trim() : "gemini-1.5-flash";
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Extracts structured scheme draft from government circular text using Google Gemini API.
     * Enforces the Zero-Fabrication Rule: If Gemini is unavailable, missing, or fails,
     * returns extractionStatus = "MANUAL_REVIEW" with empty fields (never invented data).
     *
     * @param extractedText Text extracted from the government circular PDF
     * @param filename      Original filename of the circular
     * @param documentId    MongoDB GridFS file ID
     * @param adminUsername Username/ID of the admin who uploaded the file
     * @return Structured SchemeDraftResponse DTO
     */
    public SchemeDraftResponse extractSchemeFromText(String extractedText, String filename, String documentId, String adminUsername) {
        Instant now = Instant.now();
        String snippet = extractedText != null ? extractedText.substring(0, Math.min(extractedText.length(), 600)) : "";

        SchemeDraftResponse.SchemeDraftResponseBuilder builder = SchemeDraftResponse.builder()
                .sourceDocumentId(documentId)
                .sourceFileName(filename)
                .uploadedBy(adminUsername)
                .uploadedAt(now)
                .extractedAt(now)
                .modelUsed(modelName)
                .rawSnippet(snippet);

        if (apiKey.isEmpty()) {
            log.warn("GEMINI_API_KEY is not configured. Falling back to MANUAL_REVIEW without fabrication.");
            return builder
                    .extractionStatus("MANUAL_REVIEW")
                    .build();
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("Extracted circular text is empty. Falling back to MANUAL_REVIEW.");
            return builder
                    .extractionStatus("MANUAL_REVIEW")
                    .build();
        }

        try {
            String prompt = buildPrompt(extractedText);
            String requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "responseMimeType", "application/json"
                    )
            );

            log.info("Sending circular extraction request to Gemini API (model: {}) for file: {}", modelName, filename);
            String responseJson = restClient.post()
                    .uri(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            SchemeDraftResponse extractedDraft = parseGeminiResponse(responseJson, builder);
            extractedDraft.setExtractionStatus("SUCCESS");
            log.info("Successfully extracted structured scheme draft via Gemini for file: {}", filename);
            return extractedDraft;

        } catch (Exception e) {
            log.error("Gemini API extraction failed for file: {}. Reason: {}. Falling back to MANUAL_REVIEW.", filename, e.getMessage());
            return builder
                    .extractionStatus("MANUAL_REVIEW")
                    .build();
        }
    }

    private String buildPrompt(String circularText) {
        return "You are an AI assistant for SchemeBridge, an Indian government welfare scheme platform. " +
                "Analyze the following government circular / official notification text and extract structured scheme metadata. " +
                "Output strictly a JSON object with the following schema:\n" +
                "{\n" +
                "  \"name\": string or null,\n" +
                "  \"schemeCode\": string or null,\n" +
                "  \"description\": string or null,\n" +
                "  \"shortDescription\": string or null,\n" +
                "  \"ministry\": string or null,\n" +
                "  \"department\": string or null,\n" +
                "  \"category\": string or null,\n" +
                "  \"schemeLevel\": \"CENTRAL\" | \"STATE\" | \"UT\" | null,\n" +
                "  \"stateOrUt\": string or null,\n" +
                "  \"beneficiaryType\": string or null,\n" +
                "  \"minAge\": number or null,\n" +
                "  \"maxAge\": number or null,\n" +
                "  \"maxIncome\": number or null,\n" +
                "  \"occupations\": string[] or [],\n" +
                "  \"castes\": string[] or [],\n" +
                "  \"genders\": string[] or [],\n" +
                "  \"states\": string[] or [],\n" +
                "  \"benefits\": string[] or [],\n" +
                "  \"requiredDocuments\": string[] or [],\n" +
                "  \"steps\": string[] or [],\n" +
                "  \"officialLink\": string or null,\n" +
                "  \"deadline\": string or null\n" +
                "}\n\n" +
                "CRITICAL ZERO-FABRICATION RULE: Do NOT invent or hallucinate dates, rules, criteria, or values. " +
                "If a field cannot be determined from the circular text, set it to null or empty list.\n\n" +
                "GOVERNMENT CIRCULAR TEXT:\n" + circularText;
    }

    private SchemeDraftResponse parseGeminiResponse(String responseJson, SchemeDraftResponse.SchemeDraftResponseBuilder builder) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                return builder.extractionStatus("MANUAL_REVIEW").build();
            }

            String rawJson = textNode.asText().trim();
            if (rawJson.startsWith("```json")) {
                rawJson = rawJson.substring(7);
            }
            if (rawJson.startsWith("```")) {
                rawJson = rawJson.substring(3);
            }
            if (rawJson.endsWith("```")) {
                rawJson = rawJson.substring(0, rawJson.length() - 3);
            }
            rawJson = rawJson.trim();

            JsonNode data = objectMapper.readTree(rawJson);

            return builder
                    .name(getText(data, "name"))
                    .schemeCode(getText(data, "schemeCode"))
                    .description(getText(data, "description"))
                    .shortDescription(getText(data, "shortDescription"))
                    .ministry(getText(data, "ministry"))
                    .department(getText(data, "department"))
                    .category(getText(data, "category"))
                    .schemeLevel(getText(data, "schemeLevel"))
                    .stateOrUt(getText(data, "stateOrUt"))
                    .beneficiaryType(getText(data, "beneficiaryType"))
                    .minAge(getInt(data, "minAge"))
                    .maxAge(getInt(data, "maxAge"))
                    .maxIncome(getDouble(data, "maxIncome"))
                    .occupations(getList(data, "occupations"))
                    .castes(getList(data, "castes"))
                    .genders(getList(data, "genders"))
                    .states(getList(data, "states"))
                    .benefits(getList(data, "benefits"))
                    .requiredDocuments(getList(data, "requiredDocuments"))
                    .steps(getList(data, "steps"))
                    .officialLink(getText(data, "officialLink"))
                    .deadline(getText(data, "deadline"))
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse Gemini response JSON: {}", e.getMessage());
            return builder.extractionStatus("MANUAL_REVIEW").build();
        }
    }

    private String getText(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return (f != null && !f.isNull() && !f.asText().isBlank()) ? f.asText().trim() : null;
    }

    private Integer getInt(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return (f != null && !f.isNull() && f.isNumber()) ? f.asInt() : null;
    }

    private Double getDouble(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return (f != null && !f.isNull() && f.isNumber()) ? f.asDouble() : null;
    }

    private List<String> getList(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        JsonNode f = node.get(field);
        if (f != null && f.isArray()) {
            for (JsonNode item : f) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    list.add(item.asText().trim());
                }
            }
        }
        return list;
    }
}
