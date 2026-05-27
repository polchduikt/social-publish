package com.socialpublish.integrations.slack.dto;

import java.util.List;
import java.util.UUID;

public record SlackSettingsView(
        List<SlackAccountView> accounts,
        boolean configured,
        boolean enabled
) {
    public record SlackAccountView(
            UUID id,
            String maskedWebhookUrl,
            String label,
            boolean enabled
    ) {}
}
