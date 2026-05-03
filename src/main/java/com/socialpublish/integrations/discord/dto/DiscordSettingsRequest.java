package com.socialpublish.integrations.discord.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DiscordSettingsRequest {
    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    private boolean enabled = true;
}
