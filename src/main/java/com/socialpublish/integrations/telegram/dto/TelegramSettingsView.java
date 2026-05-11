package com.socialpublish.integrations.telegram.dto;

public record TelegramSettingsView(
        String botToken,
        String chatId,
        boolean enabled,
        boolean configured
) {
}
