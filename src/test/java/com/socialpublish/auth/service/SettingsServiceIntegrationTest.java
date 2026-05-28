package com.socialpublish.auth.service;

import com.socialpublish.AbstractIntegrationTest;
import com.socialpublish.auth.dto.ChangePasswordRequest;
import com.socialpublish.auth.dto.SetPasswordRequest;
import com.socialpublish.auth.dto.UpdateProfileRequest;
import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.exception.SettingsOperationException;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.posts.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("user@example.com");
        testUser.setPassword(passwordEncoder.encode("correctpassword"));
        testUser.setFullName("Test User");
        testUser.setProvider(AuthProvider.LOCAL);
        testUser.setRole(Role.USER);
        testUser.setPasswordLoginEnabled(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldUpdateProfileSuccessfully() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setEmail("newemail@example.com");
        request.setEmailNotificationsEnabled(true);

        settingsService.updateProfile(testUser.getId(), request);

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getEmail()).isEqualTo("newemail@example.com");
        assertThat(updated.isEmailNotificationsEnabled()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenUpdatingProfileWithExistingEmail() {
        User anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setFullName("Another");
        anotherUser.setProvider(AuthProvider.LOCAL);
        anotherUser.setRole(Role.USER);
        userRepository.save(anotherUser);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setEmail("another@example.com");

        assertThatThrownBy(() -> settingsService.updateProfile(testUser.getId(), request))
                .isInstanceOf(SettingsOperationException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void shouldSetPasswordSuccessfullyForOAuth2User() {
        User oauthUser = new User();
        oauthUser.setEmail("oauth@example.com");
        oauthUser.setFullName("OAuth User");
        oauthUser.setProvider(AuthProvider.GOOGLE);
        oauthUser.setRole(Role.USER);
        oauthUser.setGoogleEmail("oauth@example.com");
        oauthUser.setGoogleSub("12345");
        oauthUser.setPassword(null);
        oauthUser.setPasswordLoginEnabled(false);
        oauthUser = userRepository.save(oauthUser);

        SetPasswordRequest request = new SetPasswordRequest();
        request.setNewPassword("newlocalpassword");
        request.setConfirmPassword("newlocalpassword");

        settingsService.setPassword(oauthUser.getId(), request);

        User updated = userRepository.findById(oauthUser.getId()).orElseThrow();
        assertThat(updated.isPasswordLoginEnabled()).isTrue();
        assertThat(passwordEncoder.matches("newlocalpassword", updated.getPassword())).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenSettingPasswordOnUserWithPasswordAlreadySet() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setNewPassword("anotherpassword");
        request.setConfirmPassword("anotherpassword");

        assertThatThrownBy(() -> settingsService.setPassword(testUser.getId(), request))
                .isInstanceOf(SettingsOperationException.class)
                .hasMessageContaining("Password already set");
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("correctpassword");
        request.setNewPassword("brandnewpassword");
        request.setConfirmPassword("brandnewpassword");

        settingsService.changePassword(testUser.getId(), request);

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brandnewpassword", updated.getPassword())).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenChangingPasswordWithIncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpassword");
        request.setNewPassword("brandnewpassword");
        request.setConfirmPassword("brandnewpassword");

        assertThatThrownBy(() -> settingsService.changePassword(testUser.getId(), request))
                .isInstanceOf(SettingsOperationException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void shouldUnlinkGoogleSuccessfully() {
        testUser.setGoogleEmail("google@example.com");
        testUser.setGoogleSub("sub123");
        userRepository.save(testUser);

        settingsService.unlinkGoogle(testUser.getId());

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.isGoogleLinked()).isFalse();
        assertThat(updated.getGoogleEmail()).isNull();
        assertThat(updated.getGoogleSub()).isNull();
    }

    @Test
    void shouldThrowExceptionWhenUnlinkingGoogleFromUserWithoutPassword() {
        User oauthUser = new User();
        oauthUser.setEmail("oauth@example.com");
        oauthUser.setFullName("OAuth User");
        oauthUser.setProvider(AuthProvider.GOOGLE);
        oauthUser.setRole(Role.USER);
        oauthUser.setGoogleEmail("oauth@example.com");
        oauthUser.setGoogleSub("12345");
        oauthUser.setPassword(null);
        oauthUser.setPasswordLoginEnabled(false);
        oauthUser = userRepository.save(oauthUser);

        final UUID oauthUserId = oauthUser.getId();

        assertThatThrownBy(() -> settingsService.unlinkGoogle(oauthUserId))
                .isInstanceOf(SettingsOperationException.class)
                .hasMessageContaining("Set a password before unlinking Google");
    }

    @Test
    void shouldDeleteAccountSuccessfully() {
        settingsService.deleteAccount(testUser.getId());

        Optional<User> found = userRepository.findById(testUser.getId());
        assertThat(found).isEmpty();
    }
}
