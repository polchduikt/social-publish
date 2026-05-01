package com.socialpublish.integrations.telegram.dto;

import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;

public record TelegramSettingsView(
        String botToken,
        String chatId,
        boolean enabled,
        boolean configured
) {

    public static TelegramSettingsView from(TelegramSettingsEntity settings) {
        String maskedToken = maskToken(settings.getBotToken());
        return new TelegramSettingsView(maskedToken, settings.getChatId(), settings.isEnabled(), true);
    }

    public static TelegramSettingsView empty() {
        return new TelegramSettingsView("", "", false, false);
    }

    private static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 4);
    }
}
