package com.socialpublish.integrations.telegram.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsRequest;
import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.socialpublish.integrations.service.BaseIntegrationService;

@Service
public class TelegramService extends BaseIntegrationService<TelegramSettingsEntity, TelegramSettingsRepository> {

    private final TelegramClientService telegramClient;

    public TelegramService(TelegramSettingsRepository settingsRepository, UserRepository userRepository, TelegramClientService telegramClient) {
        super(settingsRepository, userRepository);
        this.telegramClient = telegramClient;
    }

    @Transactional
    public void saveSettings(UUID userId, TelegramSettingsRequest request) {
        TelegramSettingsEntity settings = findOrCreate(userId, TelegramSettingsEntity::new);

        settings.setBotToken(request.getBotToken().trim());
        settings.setChatId(request.getChatId().trim());
        settings.setEnabled(request.isEnabled());
        settingsRepository.save(settings);
    }

    public void testMessage(UUID userId, String testMessage) {
        TelegramSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Telegram is not configured"));
        if (!settings.isEnabled()) {
            throw new RuntimeException("Telegram is disabled");
        }
        telegramClient.sendMessage(settings.getBotToken(), settings.getChatId(), testMessage);
    }

    @Override
    @Transactional
    public void disconnect(UUID userId) {
        super.disconnect(userId);
    }
}
