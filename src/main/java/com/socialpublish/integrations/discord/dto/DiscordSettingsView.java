package com.socialpublish.integrations.discord.dto;

import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;

public record DiscordSettingsView(
        boolean configured,
        boolean enabled,
        String webhookUrl
) {
    public static DiscordSettingsView from(DiscordSettingsEntity entity) {
        return new DiscordSettingsView(
                true,
                entity.isEnabled(),
                maskUrl(entity.getWebhookUrl())
        );
    }

    public static DiscordSettingsView empty() {
        return new DiscordSettingsView(false, false, "");
    }

    private static String maskUrl(String url) {
        if (url == null || url.length() < 20) {
            return "***";
        }
        return url.substring(0, 40) + "..." + url.substring(url.length() - 6);
    }
}
