package com.socialpublish.auth.service;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.mapper.CurrentUserViewMapper;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.auth.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;
    private final CurrentUserViewMapper currentUserViewMapper;

    public AuthenticatedUserService(
            UserRepository userRepository,
            CurrentUserViewMapper currentUserViewMapper
    ) {
        this.userRepository = userRepository;
        this.currentUserViewMapper = currentUserViewMapper;
    }

    public Optional<CurrentUserView> resolveCurrentUser(Authentication authentication) {
        return resolvePersistedUser(authentication).map(currentUserViewMapper::toView);
    }

    public Optional<CurrentUserView> resolveForView(Authentication authentication) {
        return resolveCurrentUser(authentication).or(() -> resolveOAuth2View(authentication));
    }

    private Optional<User> resolvePersistedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserDetails appUserDetails) {
            return Optional.of(appUserDetails.getUser());
        }

        if (principal instanceof OAuth2User oAuth2User) {
            return resolveByOAuth2Attributes(oAuth2User);
        }

        String authenticationName = authentication.getName();
        if (authenticationName == null || authenticationName.isBlank() || "anonymousUser".equals(authenticationName)) {
            return Optional.empty();
        }

        return userRepository.findByEmailIgnoreCase(authenticationName.trim().toLowerCase());
    }

    private Optional<User> resolveByOAuth2Attributes(OAuth2User oAuth2User) {
        String sub = extractString(oAuth2User, "sub");
        if (sub != null) {
            Optional<User> bySub = userRepository.findByGoogleSub(sub);
            if (bySub.isPresent()) {
                return bySub;
            }
        }
        return extractEmail(oAuth2User)
                .flatMap(email -> userRepository.findByGoogleEmailIgnoreCase(email)
                        .or(() -> userRepository.findByEmailIgnoreCase(email)));
    }

    private Optional<CurrentUserView> resolveOAuth2View(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oAuth2User) {
            return extractEmail(oAuth2User)
                    .map(email -> currentUserViewMapper.toOAuth2View(
                            email,
                            readOAuthDisplayName(oAuth2User, email)
                    ));
        }

        return Optional.empty();
    }

    private Optional<String> extractEmail(OAuth2User oAuth2User) {
        String email = extractString(oAuth2User, "email");
        if (email != null && !email.isBlank()) {
            return Optional.of(email.trim().toLowerCase());
        }
        return Optional.empty();
    }

    private String extractString(OAuth2User oAuth2User, String key) {
        Object value = oAuth2User.getAttributes().get(key);
        return value instanceof String text ? text : null;
    }

    private String readOAuthDisplayName(OAuth2User oAuth2User, String fallbackEmail) {
        String name = extractString(oAuth2User, "name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return fallbackEmail;
    }
}
