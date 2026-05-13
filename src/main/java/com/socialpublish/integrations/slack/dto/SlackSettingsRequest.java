package com.socialpublish.integrations.slack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class SlackSettingsRequest {
    private UUID id;

    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    private String label;

    private Boolean enabled = true;
}


