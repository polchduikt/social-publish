package com.socialpublish.integrations.discord.repository;

import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import com.socialpublish.integrations.repository.BaseIntegrationSettingsRepository;

@Repository
public interface DiscordSettingsRepository extends JpaRepository<DiscordSettingsEntity, UUID>,
        BaseIntegrationSettingsRepository<DiscordSettingsEntity> {
}
