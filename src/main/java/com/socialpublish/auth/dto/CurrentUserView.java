package com.socialpublish.auth.dto;

import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import java.util.UUID;

public record CurrentUserView(
        UUID id,
        String email,
        String fullName,
        AuthProvider provider,
        Role role,
        boolean passwordLoginEnabled,
        boolean emailNotificationsEnabled
) {
}
