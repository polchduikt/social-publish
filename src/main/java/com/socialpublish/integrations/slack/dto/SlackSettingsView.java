package com.socialpublish.integrations.slack.dto;

public record SlackSettingsView(
        boolean configured,
        boolean enabled,
        String webhookUrl
) {
}
