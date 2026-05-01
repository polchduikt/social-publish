package com.socialpublish.integrations.telegram.repository;

import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TelegramSettingsRepository extends JpaRepository<TelegramSettingsEntity, UUID> {

    Optional<TelegramSettingsEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
