package com.socialpublish.aiassistant.dto;

public record AiAssistantChatResponse(
        String reply,
        String generatedText,
        boolean needsPlacementChoice
) {
}

