package com.socialpublish.auth.dto;

import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;

import java.util.UUID;

public record CurrentUserView(
        UUID id,
        String email,
        String fullName,
        AuthProvider provider,
        Role role,
        boolean passwordLoginEnabled
) {

    public static CurrentUserView from(User user) {
        return new CurrentUserView(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getProvider(),
                user.getRole(),
                user.isPasswordLoginEnabled()
        );
    }

    public static CurrentUserView ofOAuth2(String email, String fullName) {
        return new CurrentUserView(
                null,
                email,
                fullName,
                AuthProvider.GOOGLE,
                Role.USER,
                false
        );
    }
}
