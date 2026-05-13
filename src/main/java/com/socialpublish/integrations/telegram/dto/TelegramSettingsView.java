package com.socialpublish.integrations.telegram.dto;

import java.util.UUID;
import java.util.List;

public record TelegramSettingsView(
        List<TelegramAccountView> accounts,
        boolean configured,
        boolean enabled
) {
    public record TelegramAccountView(
            UUID id,
            String botToken,
            String chatId,
            String label,
            boolean enabled
    ) {}
}
