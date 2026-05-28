package com.socialpublish.integrations.service;

import com.socialpublish.auth.event.UserDeletedEvent;
import com.socialpublish.integrations.repository.BaseIntegrationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationCleanupListener {

    private final List<BaseIntegrationSettingsRepository<?>> integrationRepositories;

    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("Received UserDeletedEvent for user: {}. Cleaning up integration settings.", event.getUserId());
        for (BaseIntegrationSettingsRepository<?> repo : integrationRepositories) {
            try {
                repo.deleteAllByUserId(event.getUserId());
            } catch (Exception e) {
                log.error("Failed to clean up integration repository {} for user {}", repo.getClass().getSimpleName(), event.getUserId(), e);
                throw e;
            }
        }
        log.info("Completed cleaning up integration settings for user: {}", event.getUserId());
    }
}
