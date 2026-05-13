package com.socialpublish.integrations.telegram.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class TelegramSettingsRequest {

    private UUID id;

    @NotBlank(message = "Bot token is required")
    private String botToken;

    @NotBlank(message = "Chat ID is required")
    private String chatId;

    private String label;

    private Boolean enabled = true;
}
