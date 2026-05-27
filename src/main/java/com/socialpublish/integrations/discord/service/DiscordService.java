package com.socialpublish.integrations.discord.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.discord.dto.DiscordSettingsRequest;
import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
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
import com.socialpublish.integrations.discord.dto.DiscordSettingsListRequest;
import com.socialpublish.integrations.mapper.IntegrationSettingsMapper;

@Service
public class DiscordService extends BaseIntegrationService<DiscordSettingsEntity, DiscordSettingsRepository> {

    private final DiscordClientService discordClient;
    private final IntegrationSettingsMapper integrationSettingsMapper;

    public DiscordService(DiscordSettingsRepository settingsRepository, UserRepository userRepository,
                          DiscordClientService discordClient, IntegrationSettingsMapper integrationSettingsMapper) {
        super(settingsRepository, userRepository);
        this.discordClient = discordClient;
        this.integrationSettingsMapper = integrationSettingsMapper;
    }

    @Transactional(readOnly = true)
    public DiscordSettingsListRequest getSettingsRequest(UUID userId) {
        List<DiscordSettingsEntity> entities = settingsRepository.findAllByUserId(userId);
        DiscordSettingsListRequest request = new DiscordSettingsListRequest();
        request.setAccounts(integrationSettingsMapper.toDiscordRequests(entities));
        return request;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "integrations", key = "#userId"),
            @CacheEvict(value = "account-labels", key = "#userId")
    })
    public void saveSettings(UUID userId, DiscordSettingsListRequest requestList) {
        List<DiscordSettingsEntity> existing = settingsRepository.findAllByUserId(userId);
        Map<UUID, DiscordSettingsEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(DiscordSettingsEntity::getId, Function.identity()));

        List<DiscordSettingsEntity> toSave = new ArrayList<>();

        if (requestList.getAccounts() != null) {
            for (DiscordSettingsRequest req : requestList.getAccounts()) {
                DiscordSettingsEntity entity;
                if (req.getId() != null && existingMap.containsKey(req.getId())) {
                    entity = existingMap.get(req.getId());
                    existingMap.remove(req.getId());
                } else {
                    entity = new DiscordSettingsEntity();
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
        DiscordSettingsEntity settings = settingsRepository.findById(targetAccountId)
                .orElseThrow(() -> new RuntimeException("Discord account not found"));
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
