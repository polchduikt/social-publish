package com.socialpublish.integrations.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.entity.BaseIntegrationSettings;
import com.socialpublish.integrations.repository.BaseIntegrationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor
public abstract class BaseIntegrationService<E extends BaseIntegrationSettings, R extends BaseIntegrationSettingsRepository<E>> {

    protected final R settingsRepository;
    protected final UserRepository userRepository;

    @Transactional
    public E findOrCreate(UUID userId, Supplier<E> entitySupplier) {
        return settingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    E settings = entitySupplier.get();
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                    settings.setUser(user);
                    return settings;
                });
    }

    @Transactional
    public void disconnect(UUID userId) {
        settingsRepository.findByUserId(userId).ifPresent(settingsRepository::delete);
    }
}
