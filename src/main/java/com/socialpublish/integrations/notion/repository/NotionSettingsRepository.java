package com.socialpublish.integrations.notion.repository;

import com.socialpublish.integrations.notion.entity.NotionSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.socialpublish.integrations.repository.BaseIntegrationSettingsRepository;

public interface NotionSettingsRepository extends JpaRepository<NotionSettingsEntity, UUID>,
        BaseIntegrationSettingsRepository<NotionSettingsEntity> {
}
