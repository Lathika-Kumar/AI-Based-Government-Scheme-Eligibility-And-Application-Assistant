package com.schemebridge.coreservice.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MySchemeJsonParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode parseFile(File file) {
        try {
            JsonNode root = mapper.readTree(file);
            if (root != null && root.has("data") && root.get("data").isObject()) {
                return root.get("data");
            }
        } catch (Exception e) {
            log.error("Failed to parse JSON file {}: {}", file.getName(), e.getMessage());
        }
        return null;
    }

    public String cleanHtml(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String cleaned = text.replaceAll("<[^>]+>", " ");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    /**
     * Recursively extracts plain text from a SlateJS AST node, array, object, or text value.
     * Never throws exceptions on unexpected node structures.
     */
    public String extractSlateText(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode child : node) {
                String txt = extractSlateText(child);
                if (!txt.trim().isEmpty()) {
                    parts.add(txt.trim());
                }
            }
            return String.join("\n", parts);
        }
        if (node.isObject()) {
            if (node.has("text")) {
                return node.get("text").asText();
            }
            if (node.has("children")) {
                return extractSlateText(node.get("children"));
            }
            if (node.has("process")) {
                return extractSlateText(node.get("process"));
            }
            if (node.has("process_md") && !node.get("process_md").isNull()) {
                return node.get("process_md").asText();
            }
        }
        return "";
    }

    /**
     * Recursively extracts individual bullet points / list lines from a SlateJS AST node.
     */
    public List<String> extractSlateTextLines(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node == null || node.isNull()) return result;

        String fullText = extractSlateText(node);
        if (fullText != null && !fullText.trim().isEmpty()) {
            for (String line : fullText.split("\n")) {
                String cleaned = cleanHtml(line);
                if (!cleaned.trim().isEmpty()) {
                    result.add(cleaned.trim());
                }
            }
        }
        return result;
    }
}
