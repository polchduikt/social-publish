package com.socialpublish.integrations.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseIntegrationSettingsRepository<E> extends CrudRepository<E, UUID> {
    List<E> findAllByUserId(UUID userId);
    Optional<E> findByUserId(UUID userId);
    void deleteAllByUserId(UUID userId);
}
