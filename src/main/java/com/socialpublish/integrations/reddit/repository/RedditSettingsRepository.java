package com.socialpublish.integrations.reddit.repository;

import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RedditSettingsRepository extends JpaRepository<RedditSettingsEntity, UUID> {
    Optional<RedditSettingsEntity> findByUserId(UUID userId);
}
