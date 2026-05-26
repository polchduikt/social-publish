package com.socialpublish.integrations.telegram.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsRequest;
import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import com.socialpublish.integrations.service.BaseIntegrationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsListRequest;

@Service
public class TelegramService extends BaseIntegrationService<TelegramSettingsEntity, TelegramSettingsRepository> {

    private static final int MIN_MASK_LENGTH = 8;
    private static final int MASK_VISIBLE_CHARS = 4;
    private final TelegramClientService telegramClient;

    public TelegramService(TelegramSettingsRepository settingsRepository, UserRepository userRepository, TelegramClientService telegramClient) {
        super(settingsRepository, userRepository);
        this.telegramClient = telegramClient;
    }

    @Transactional(readOnly = true)
    public TelegramSettingsListRequest getSettingsRequest(UUID userId) {
        List<TelegramSettingsEntity> entities = settingsRepository.findAllByUserId(userId);
        TelegramSettingsListRequest request = new TelegramSettingsListRequest();
        request.setAccounts(entities.stream().map(entity -> {
            TelegramSettingsRequest req = new TelegramSettingsRequest();
            req.setId(entity.getId());
            req.setBotToken(entity.getBotToken());
            req.setChatId(entity.getChatId());
            req.setLabel(entity.getLabel());
            req.setEnabled(entity.isEnabled());
            return req;
        }).collect(Collectors.toList()));
        return request;
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= MIN_MASK_LENGTH) return token == null ? "" : token;
        return token.substring(0, MASK_VISIBLE_CHARS) + "..." + token.substring(token.length() - MASK_VISIBLE_CHARS);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "integrations", key = "#userId"),
            @CacheEvict(value = "account-labels", key = "#userId")
    })
    public void saveSettings(UUID userId, TelegramSettingsListRequest requestList) {
        List<TelegramSettingsEntity> existing = settingsRepository.findAllByUserId(userId);
        Map<UUID, TelegramSettingsEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(TelegramSettingsEntity::getId, Function.identity()));

        List<TelegramSettingsEntity> toSave = new ArrayList<>();

        if (requestList.getAccounts() != null) {
            for (TelegramSettingsRequest req : requestList.getAccounts()) {
                TelegramSettingsEntity entity;
                if (req.getId() != null && existingMap.containsKey(req.getId())) {
                    entity = existingMap.get(req.getId());
                    existingMap.remove(req.getId());
                } else {
                    entity = new TelegramSettingsEntity();
                    entity.setUser(userRepository.getReferenceById(userId));
                }
                String rawToken = req.getBotToken();
                if (rawToken != null && !rawToken.isBlank() && !rawToken.contains("...")) {
                    entity.setBotToken(rawToken.trim());
                }
                String rawChatId = req.getChatId().trim();
                if (!rawChatId.contains("...")) {
                    if (!rawChatId.startsWith("-") && !rawChatId.startsWith("@") && rawChatId.matches("\\d+")) {
                        rawChatId = "-" + rawChatId;
                    }
                    entity.setChatId(rawChatId);
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
        TelegramSettingsEntity settings = settingsRepository.findById(targetAccountId)
                .orElseThrow(() -> new RuntimeException("Telegram account not found"));
        if (!settings.isEnabled()) {
            throw new RuntimeException("Telegram is disabled");
        }
        telegramClient.sendMessage(settings.getBotToken(), settings.getChatId(), testMessage, false, null);
    }

    @Override
    @Transactional
    public void disconnect(UUID userId) {
        super.disconnect(userId);
    }
}
