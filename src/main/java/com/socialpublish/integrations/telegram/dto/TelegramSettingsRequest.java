package com.socialpublish.integrations.telegram.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TelegramSettingsRequest {

    @NotBlank(message = "Bot token is required")
    private String botToken;

    @NotBlank(message = "Chat ID is required")
    private String chatId;

    private boolean enabled = true;
}
