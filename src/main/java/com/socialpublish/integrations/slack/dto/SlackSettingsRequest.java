package com.socialpublish.integrations.slack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SlackSettingsRequest {
    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    private boolean enabled = true;
}


