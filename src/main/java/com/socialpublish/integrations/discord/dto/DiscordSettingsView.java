package com.socialpublish.integrations.discord.dto;

public record DiscordSettingsView(
        boolean configured,
        boolean enabled,
        String webhookUrl
) {
}
