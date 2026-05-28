package com.socialpublish.integrations.slack.repository;

import com.socialpublish.integrations.repository.BaseIntegrationSettingsRepository;
import com.socialpublish.integrations.slack.entity.SlackSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SlackSettingsRepository extends JpaRepository<SlackSettingsEntity, UUID>,
        BaseIntegrationSettingsRepository<SlackSettingsEntity> {
}


