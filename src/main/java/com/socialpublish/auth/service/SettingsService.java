package com.socialpublish.auth.service;

import com.socialpublish.auth.dto.ChangePasswordRequest;
import com.socialpublish.auth.dto.UpdateProfileRequest;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.exception.SettingsOperationException;
import com.socialpublish.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);

        if (!user.isPasswordLoginEnabled()) {
            throw new SettingsOperationException("Password change is not available for OAuth accounts");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new SettingsOperationException("Passwords do not match");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new SettingsOperationException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        userRepository.deleteById(userId);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SettingsOperationException("User not found"));
    }
}
