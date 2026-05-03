package com.socialpublish.integrations.discord.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscordSettingsRequest {
    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    private boolean enabled = true;
}
