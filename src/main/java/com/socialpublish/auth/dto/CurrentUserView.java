package com.socialpublish.auth.dto;

import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import java.util.UUID;

public record CurrentUserView(
        UUID id,
        String email,
        String googleEmail,
        String fullName,
        AuthProvider provider,
        Role role,
        boolean passwordLoginEnabled,
        boolean googleLinked,
        boolean emailNotificationsEnabled,
        boolean aiAssistantEnabled
) {
}
