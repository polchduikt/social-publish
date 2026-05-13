package com.socialpublish.integrations.discord.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class DiscordSettingsRequest {
    private UUID id;

    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    private String label;

    private Boolean enabled = true;
}
