package com.socialpublish.integrations.slack.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.slack.dto.SlackSettingsRequest;
import com.socialpublish.integrations.slack.entity.SlackSettingsEntity;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.socialpublish.integrations.service.BaseIntegrationService;
import com.socialpublish.integrations.slack.dto.SlackSettingsListRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlackService extends BaseIntegrationService<SlackSettingsEntity, SlackSettingsRepository> {

    private final SlackClientService slackClient;

    public SlackService(SlackSettingsRepository settingsRepository, UserRepository userRepository, SlackClientService slackClient) {
        super(settingsRepository, userRepository);
        this.slackClient = slackClient;
    }
    @Transactional(readOnly = true)
    public SlackSettingsListRequest getSettingsRequest(UUID userId) {
        List<SlackSettingsEntity> entities = settingsRepository.findAllByUserId(userId);
        SlackSettingsListRequest request = new SlackSettingsListRequest();
        request.setAccounts(entities.stream().map(entity -> {
            SlackSettingsRequest req = new SlackSettingsRequest();
            req.setId(entity.getId());
            req.setWebhookUrl(entity.getWebhookUrl());
            req.setLabel(entity.getLabel());
            req.setEnabled(entity.isEnabled());
            return req;
        }).collect(Collectors.toList()));
        return request;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "integrations", key = "#userId"),
            @CacheEvict(value = "account-labels", key = "#userId")
    })
    public void saveSettings(UUID userId, SlackSettingsListRequest requestList) {
        List<SlackSettingsEntity> existing = settingsRepository.findAllByUserId(userId);
        Map<UUID, SlackSettingsEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(SlackSettingsEntity::getId, Function.identity()));

        List<SlackSettingsEntity> toSave = new ArrayList<>();

        if (requestList.getAccounts() != null) {
            for (SlackSettingsRequest req : requestList.getAccounts()) {
                SlackSettingsEntity entity;
                if (req.getId() != null && existingMap.containsKey(req.getId())) {
                    entity = existingMap.get(req.getId());
                    existingMap.remove(req.getId());
                } else {
                    entity = new SlackSettingsEntity();
                    entity.setUser(userRepository.getReferenceById(userId));
                }
                String rawUrl = req.getWebhookUrl();
                if (rawUrl != null && !rawUrl.isBlank() && !rawUrl.contains("...")) {
                    entity.setWebhookUrl(rawUrl.trim());
                }
                entity.setLabel(req.getLabel() != null ? req.getLabel().trim() : "");
                entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : false);
                toSave.add(entity);
            }
        }

        settingsRepository.deleteAll(existingMap.values());
        settingsRepository.saveAll(toSave);
    }

    public void testMessage(UUID targetAccountId, String testMessage) {
        SlackSettingsEntity settings = settingsRepository.findById(targetAccountId)
                .orElseThrow(() -> new RuntimeException("Slack account not found"));
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
