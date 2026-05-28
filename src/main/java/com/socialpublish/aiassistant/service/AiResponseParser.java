package com.socialpublish.aiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedResponse parseResponse(String rawResponse) {
        String cleaned = stripCodeFences(rawResponse == null ? "" : rawResponse.trim());
        if (cleaned.isBlank()) {
            return new ParsedResponse("", "", false);
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            String reply = textOrEmpty(root.get("reply"));
            String generatedText = textOrEmpty(root.get("generatedText"));
            boolean needsPlacementChoice = root.path("needsPlacementChoice").asBoolean(false);
            return new ParsedResponse(reply, generatedText, needsPlacementChoice);
        } catch (Exception e) {
            log.warn("Failed to parse JSON response from AI provider. Raw response: {}, Cleaned response: {}", rawResponse, cleaned, e);
            return new ParsedResponse(cleaned, "", false);
        }
    }

    private String stripCodeFences(String value) {
        if (value.startsWith("```") && value.endsWith("```")) {
            String noStart = value.replaceFirst("^```[a-zA-Z]*\\s*", "");
            return noStart.replaceFirst("\\s*```$", "");
        }
        return value;
    }

    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    public record ParsedResponse(String reply, String generatedText, boolean needsPlacementChoice) {
    }
}
