package com.socialpublish.integrations.discord.dto;

import java.util.List;
import java.util.UUID;

public record DiscordSettingsView(
        List<DiscordAccountView> accounts,
        boolean configured,
        boolean enabled
) {
    public record DiscordAccountView(
            UUID id,
            String webhookUrl,
            String label,
            boolean enabled
    ) {}
}
