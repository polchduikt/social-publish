package com.socialpublish.auth.service;

import com.socialpublish.auth.dto.ChangePasswordRequest;
import com.socialpublish.auth.dto.SetPasswordRequest;
import com.socialpublish.auth.dto.UpdateProfileRequest;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.exception.SettingsOperationException;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.auth.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);

        String normalizedName = request.getFullName().trim();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new SettingsOperationException("Email already in use");
        }

        user.setFullName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setEmailNotificationsEnabled(request.isEmailNotificationsEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void updateAiAssistantEnabled(UUID userId, boolean enabled) {
        User user = requireUser(userId);
        user.setAiAssistantEnabled(enabled);
        userRepository.save(user);
    }

    @Transactional
    public void setPassword(UUID userId, SetPasswordRequest request) {
        User user = requireUser(userId);

        if (user.isPasswordLoginEnabled() && user.getPassword() != null) {
            throw new SettingsOperationException("Password already set. Use Change Password instead.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordLoginEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);

        if (!user.isPasswordLoginEnabled() || user.getPassword() == null) {
            throw new SettingsOperationException("Set a password first before changing it");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new SettingsOperationException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void unlinkGoogle(UUID userId) {
        User user = requireUser(userId);

        if (!user.isGoogleLinked()) {
            throw new SettingsOperationException("Google account is not linked");
        }

        if (!user.isPasswordLoginEnabled() || user.getPassword() == null) {
            throw new SettingsOperationException("Set a password before unlinking Google. Otherwise you won't be able to log in.");
        }

        user.setGoogleEmail(null);
        user.setGoogleSub(null);
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        eventPublisher.publishEvent(new UserDeletedEvent(userId));
        userRepository.deleteById(userId);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SettingsOperationException("User not found"));
    }
}
