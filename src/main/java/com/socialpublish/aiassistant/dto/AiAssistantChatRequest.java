package com.socialpublish.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAssistantChatRequest(
        @NotBlank
        @Size(max = 2500)
        String message,

        @Size(max = 5000)
        String currentPostMessage
) {
}

