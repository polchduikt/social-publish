package com.socialpublish.integrations.notion.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.notion.dto.NotionSettingsRequest;
import com.socialpublish.integrations.notion.entity.NotionSettingsEntity;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.socialpublish.integrations.service.BaseIntegrationService;
import com.socialpublish.integrations.notion.dto.NotionSettingsListRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotionService extends BaseIntegrationService<NotionSettingsEntity, NotionSettingsRepository> {

    private final NotionClientService notionClient;

    public NotionService(NotionSettingsRepository settingsRepository, UserRepository userRepository, NotionClientService notionClient) {
        super(settingsRepository, userRepository);
        this.notionClient = notionClient;
    }
    @Transactional(readOnly = true)
    public NotionSettingsListRequest getSettingsRequest(UUID userId) {
        List<NotionSettingsEntity> entities = settingsRepository.findAllByUserId(userId);
        NotionSettingsListRequest request = new NotionSettingsListRequest();
        request.setAccounts(entities.stream().map(entity -> {
            NotionSettingsRequest req = new NotionSettingsRequest();
            req.setId(entity.getId());
            req.setApiToken(entity.getApiToken());
            req.setDatabaseId(entity.getDatabaseId());
            req.setLabel(entity.getLabel());
            req.setEnabled(entity.isEnabled());
            return req;
        }).collect(Collectors.toList()));
        return request;
    }

    @Transactional
    public void saveSettings(UUID userId, NotionSettingsListRequest requestList) {
        List<NotionSettingsEntity> existing = settingsRepository.findAllByUserId(userId);
        Map<UUID, NotionSettingsEntity> existingMap = existing.stream()
                .collect(Collectors.toMap(NotionSettingsEntity::getId, Function.identity()));

        List<NotionSettingsEntity> toSave = new ArrayList<>();

        if (requestList.getAccounts() != null) {
            for (NotionSettingsRequest req : requestList.getAccounts()) {
                NotionSettingsEntity entity;
                if (req.getId() != null && existingMap.containsKey(req.getId())) {
                    entity = existingMap.get(req.getId());
                    existingMap.remove(req.getId());
                } else {
                    entity = new NotionSettingsEntity();
                    entity.setUser(userRepository.getReferenceById(userId));
                }
                
                String rawDbId = req.getDatabaseId().trim();
                if (!rawDbId.contains("...")) {
                    entity.setDatabaseId(extractDatabaseId(rawDbId));
                }
                
                String rawToken = req.getApiToken();
                if (rawToken != null && !rawToken.isBlank() && !rawToken.contains("...")) {
                    entity.setApiToken(rawToken.trim());
                }
                entity.setLabel(req.getLabel() != null ? req.getLabel().trim() : "");
                entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : false);
                toSave.add(entity);
            }
        }

        settingsRepository.deleteAll(existingMap.values());
        settingsRepository.saveAll(toSave);
    }

    public void testEntry(UUID targetAccountId, String testMessage) {
        NotionSettingsEntity settings = settingsRepository.findById(targetAccountId)
                .orElseThrow(() -> new RuntimeException("Notion account not found"));
        if (!settings.isEnabled()) {
            throw new RuntimeException("Notion is disabled");
        }
        notionClient.createDatabaseEntry(settings.getApiToken(), settings.getDatabaseId(), testMessage, null);
    }

    @Override
    @Transactional
    public void disconnect(UUID userId) {
        super.disconnect(userId);
    }

    private String extractDatabaseId(String dbId) {
        if (dbId.contains("notion.so")) {
            if (dbId.contains("?")) {
                dbId = dbId.substring(0, dbId.indexOf("?"));
            }
            if (dbId.contains("/")) {
                dbId = dbId.substring(dbId.lastIndexOf("/") + 1);
            }
        }
        return dbId;
    }
}
