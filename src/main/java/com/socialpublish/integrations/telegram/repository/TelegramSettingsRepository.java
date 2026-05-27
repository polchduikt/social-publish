package com.socialpublish.integrations.telegram.repository;

import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.socialpublish.integrations.repository.BaseIntegrationSettingsRepository;

public interface TelegramSettingsRepository extends JpaRepository<TelegramSettingsEntity, UUID>,
        BaseIntegrationSettingsRepository<TelegramSettingsEntity> {

    boolean existsByUserId(UUID userId);
}
