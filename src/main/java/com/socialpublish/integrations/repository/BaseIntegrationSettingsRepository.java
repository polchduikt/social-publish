package com.socialpublish.integrations.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseIntegrationSettingsRepository<E> extends CrudRepository<E, UUID> {
    Optional<E> findByUserId(UUID userId);
}
