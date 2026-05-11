package com.socialpublish.integrations.slack.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.slack.dto.SlackSettingsRequest;
import com.socialpublish.integrations.slack.entity.SlackSettingsEntity;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import com.socialpublish.integrations.service.BaseIntegrationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlackService extends BaseIntegrationService<SlackSettingsEntity, SlackSettingsRepository> {

    private final SlackClientService slackClient;

    public SlackService(SlackSettingsRepository settingsRepository, UserRepository userRepository, SlackClientService slackClient) {
        super(settingsRepository, userRepository);
        this.slackClient = slackClient;
    }

    @Transactional
    public void saveSettings(UUID userId, SlackSettingsRequest request) {
        SlackSettingsEntity settings = findOrCreate(userId, SlackSettingsEntity::new);

        settings.setWebhookUrl(request.getWebhookUrl().trim());
        settings.setEnabled(request.isEnabled());
        settingsRepository.save(settings);
    }

    public void testMessage(UUID userId, String testMessage) {
        SlackSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Slack is not configured"));
        if (!settings.isEnabled()) {
            throw new RuntimeException("Slack is disabled");
        }
        slackClient.sendMessage(settings.getWebhookUrl(), testMessage);
    }

    @Override
    @Transactional
    public void disconnect(UUID userId) {
        super.disconnect(userId);
    }
}
