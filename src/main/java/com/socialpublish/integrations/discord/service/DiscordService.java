package com.socialpublish.integrations.discord.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.discord.dto.DiscordSettingsRequest;
import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import com.socialpublish.integrations.service.BaseIntegrationService;

@Service
public class DiscordService extends BaseIntegrationService<DiscordSettingsEntity, DiscordSettingsRepository> {

    private final DiscordClientService discordClient;

    public DiscordService(DiscordSettingsRepository settingsRepository, UserRepository userRepository, DiscordClientService discordClient) {
        super(settingsRepository, userRepository);
        this.discordClient = discordClient;
    }

    @Transactional
    public void saveSettings(UUID userId, DiscordSettingsRequest request) {
        DiscordSettingsEntity settings = findOrCreate(userId, DiscordSettingsEntity::new);

        settings.setWebhookUrl(request.getWebhookUrl().trim());
        settings.setEnabled(request.isEnabled());
        settingsRepository.save(settings);
    }

    public void testMessage(UUID userId, String testMessage) {
        DiscordSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Discord is not configured"));
        if (!settings.isEnabled()) {
            throw new RuntimeException("Discord is disabled");
        }
        discordClient.sendMessage(settings.getWebhookUrl(), testMessage);
    }

    @Override
    @Transactional
    public void disconnect(UUID userId) {
        super.disconnect(userId);
    }
}
