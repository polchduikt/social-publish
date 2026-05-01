package com.socialpublish.auth.service;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<CurrentUserView> resolveCurrentUser(Authentication authentication) {
        return resolvePersistedUser(authentication).map(CurrentUserView::from);
    }

    public Optional<CurrentUserView> resolveForView(Authentication authentication) {
        return resolveCurrentUser(authentication).or(() -> resolveOAuth2View(authentication));
    }

    private Optional<User> resolvePersistedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }

        if (principal instanceof OAuth2User oAuth2User) {
            return extractEmail(oAuth2User)
                    .flatMap(userRepository::findByEmailIgnoreCase);
        }

        String authenticationName = authentication.getName();
        if (authenticationName == null || authenticationName.isBlank() || "anonymousUser".equals(authenticationName)) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(authenticationName.trim().toLowerCase());
    }

    private Optional<CurrentUserView> resolveOAuth2View(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oAuth2User) {
            return extractEmail(oAuth2User)
                    .map(email -> CurrentUserView.ofOAuth2(
                            email,
                            readOAuthDisplayName(oAuth2User, email)
                    ));
        }

        return Optional.empty();
    }

    private Optional<String> extractEmail(OAuth2User oAuth2User) {
        Object value = oAuth2User.getAttributes().get("email");
        if (value instanceof String email && !email.isBlank()) {
            return Optional.of(email.trim().toLowerCase());
        }
        return Optional.empty();
    }

    private String readOAuthDisplayName(OAuth2User oAuth2User, String fallbackEmail) {
        Object nameAttribute = oAuth2User.getAttributes().get("name");
        if (nameAttribute instanceof String name && !name.isBlank()) {
            return name.trim();
        }
        return fallbackEmail;
    }
}
