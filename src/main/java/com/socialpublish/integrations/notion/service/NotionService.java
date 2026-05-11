package com.socialpublish.integrations.notion.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.notion.dto.NotionSettingsRequest;
import com.socialpublish.integrations.notion.entity.NotionSettingsEntity;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import com.socialpublish.integrations.service.BaseIntegrationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotionService extends BaseIntegrationService<NotionSettingsEntity, NotionSettingsRepository> {

    private final NotionClientService notionClient;

    public NotionService(NotionSettingsRepository settingsRepository, UserRepository userRepository, NotionClientService notionClient) {
        super(settingsRepository, userRepository);
        this.notionClient = notionClient;
    }

    @Transactional
    public void saveSettings(UUID userId, NotionSettingsRequest request) {
        NotionSettingsEntity settings = findOrCreate(userId, NotionSettingsEntity::new);

        String dbId = extractDatabaseId(request.getDatabaseId().trim());

        settings.setApiToken(request.getApiToken().trim());
        settings.setDatabaseId(dbId);
        settings.setEnabled(request.isEnabled());
        settingsRepository.save(settings);
    }

    public void testEntry(UUID userId, String testMessage) {
        NotionSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Notion is not configured"));
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
