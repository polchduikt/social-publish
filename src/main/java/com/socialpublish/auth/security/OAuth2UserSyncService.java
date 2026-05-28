package com.socialpublish.auth.security;

import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.exception.OAuth2EmailNotVerifiedException;
import com.socialpublish.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OAuth2UserSyncService {

    public static final String LINK_GOOGLE_USER_ID = "LINK_GOOGLE_USER_ID";

    private final UserRepository userRepository;

    public OAuth2UserSyncService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void syncUser(Map<String, Object> attributes) {
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
        String sub = readString(attributes, "sub");
        String name = readString(attributes, "name");
        String fullName = (name == null || name.isBlank()) ? normalizedEmail : name.trim();
        UUID linkUserId = consumeLinkingFlag();
        if (linkUserId != null) {
            linkGoogleToUserById(linkUserId, normalizedEmail, sub);
            return;
        }

        Optional<User> byGoogleSub = (sub != null) ? userRepository.findByGoogleSub(sub) : Optional.empty();
        if (byGoogleSub.isPresent()) {
            updateExistingUser(byGoogleSub.get(), normalizedEmail, fullName, sub);
            return;
        }

        Optional<User> byGoogleEmail = userRepository.findByGoogleEmailIgnoreCase(normalizedEmail);
        if (byGoogleEmail.isPresent()) {
            updateExistingUser(byGoogleEmail.get(), normalizedEmail, fullName, sub);
            return;
        }

        Optional<User> byPrimaryEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (byPrimaryEmail.isPresent()) {
            linkGoogleToExistingUser(byPrimaryEmail.get(), normalizedEmail, fullName, sub);
            return;
        }
        createNewUser(normalizedEmail, fullName, sub);
    }

    private void linkGoogleToUserById(UUID userId, String googleEmail, String sub) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_request"),
                        "User not found for linking"
                ));

        Optional<User> existingByGoogleEmail = userRepository.findByGoogleEmailIgnoreCase(googleEmail);
        if (existingByGoogleEmail.isPresent() && !existingByGoogleEmail.get().getId().equals(userId)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_conflict"),
                    "This Google account is already linked to another user"
            );
        }

        if (sub != null) {
            Optional<User> existingBySub = userRepository.findByGoogleSub(sub);
            if (existingBySub.isPresent() && !existingBySub.get().getId().equals(userId)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_conflict"),
                        "This Google account is already linked to another user"
                );
            }
        }
        user.setGoogleEmail(googleEmail);
        user.setGoogleSub(sub);
        userRepository.save(user);
    }

    private UUID consumeLinkingFlag() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;

            HttpServletRequest request = attrs.getRequest();
            HttpSession session = request.getSession(false);
            if (session == null) return null;

            Object value = session.getAttribute(LINK_GOOGLE_USER_ID);
            if (value instanceof UUID userId) {
                session.removeAttribute(LINK_GOOGLE_USER_ID);
                return userId;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateExistingUser(User user, String googleEmail, String fullName, String sub) {
        boolean changed = false;

        if (sub != null && !sub.equals(user.getGoogleSub())) {
            user.setGoogleSub(sub);
            changed = true;
        }

        if (!googleEmail.equals(user.getGoogleEmail())) {
            user.setGoogleEmail(googleEmail);
            changed = true;
        }

        if (!fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
    }

    private void linkGoogleToExistingUser(User user, String googleEmail, String fullName, String sub) {
        user.setGoogleEmail(googleEmail);
        user.setGoogleSub(sub);
        userRepository.save(user);
    }

    private void createNewUser(String email, String fullName, String sub) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setGoogleEmail(email);
        user.setGoogleSub(sub);
        user.setProvider(AuthProvider.GOOGLE);
        user.setRole(Role.USER);
        user.setPassword(null);
        user.setPasswordLoginEnabled(false);
        userRepository.save(user);
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
