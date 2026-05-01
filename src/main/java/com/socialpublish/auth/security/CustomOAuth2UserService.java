package com.socialpublish.auth.security;

import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.exception.OAuth2AccountConflictException;
import com.socialpublish.auth.exception.OAuth2EmailNotVerifiedException;
import com.socialpublish.auth.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        syncUser(oAuth2User.getAttributes());
        return oAuth2User;
    }

    private void syncUser(Map<String, Object> attributes) {
        String email = readString(attributes, "email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_request"),
                    "Google account did not provide email"
            );
        }
        Boolean emailVerified = readBoolean(attributes, "email_verified");
        if (Boolean.FALSE.equals(emailVerified)) {
            throw new OAuth2EmailNotVerifiedException("Google account email is not verified");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String name = readString(attributes, "name");
        String fullName = (name == null || name.isBlank()) ? normalizedEmail : name.trim();

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(existingUser -> {
            if (existingUser.getProvider() == AuthProvider.LOCAL) {
                throw new OAuth2AccountConflictException("This email is already registered with password login");
            }

            boolean changed = false;
            if (!fullName.equals(existingUser.getFullName())) {
                existingUser.setFullName(fullName);
                changed = true;
            }

            if (existingUser.isPasswordLoginEnabled()) {
                existingUser.setPasswordLoginEnabled(false);
                changed = true;
            }

            if (changed) {
                userRepository.save(existingUser);
            }
        }, () -> {
            User user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(fullName);
            user.setProvider(AuthProvider.GOOGLE);
            user.setRole(Role.USER);
            user.setPassword(null);
            user.setPasswordLoginEnabled(false);
            userRepository.save(user);
        });
    }

    private String readString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof String text ? text : null;
    }

    private Boolean readBoolean(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof Boolean bool ? bool : null;
    }
}
