package com.socialpublish.integrations.linkedin.repository;

import com.socialpublish.integrations.linkedin.entity.LinkedInSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LinkedInSettingsRepository extends JpaRepository<LinkedInSettingsEntity, UUID> {
    Optional<LinkedInSettingsEntity> findByUserId(UUID userId);
}
