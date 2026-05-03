package com.socialpublish.integrations.service;

import com.socialpublish.integrations.discord.dto.DiscordSettingsView;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsView;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegrationStatusService {

    private final TelegramSettingsRepository telegramSettingsRepository;
    private final DiscordSettingsRepository discordSettingsRepository;

    public boolean isTelegramConnected(UUID userId) {
        return telegramSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getBotToken() != null
                        && !settings.getBotToken().isBlank())
                .orElse(false);
    }

    public boolean isDiscordConnected(UUID userId) {
        return discordSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getWebhookUrl() != null
                        && !settings.getWebhookUrl().isBlank())
                .orElse(false);
    }

    public TelegramSettingsView getTelegramView(UUID userId) {
        return telegramSettingsRepository.findByUserId(userId)
                .map(TelegramSettingsView::from)
                .orElse(TelegramSettingsView.empty());
    }

    public DiscordSettingsView getDiscordView(UUID userId) {
        return discordSettingsRepository.findByUserId(userId)
                .map(DiscordSettingsView::from)
                .orElse(DiscordSettingsView.empty());
    }
}
