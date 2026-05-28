package com.socialpublish.integrations.reddit.repository;

import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.socialpublish.integrations.repository.BaseIntegrationSettingsRepository;

public interface RedditSettingsRepository extends JpaRepository<RedditSettingsEntity, UUID>,
        BaseIntegrationSettingsRepository<RedditSettingsEntity> {
}
