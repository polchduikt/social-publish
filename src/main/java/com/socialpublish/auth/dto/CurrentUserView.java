package com.socialpublish.auth.dto;

import java.util.UUID;

public record CurrentUserView(
        UUID id,
        String email,
        String googleEmail,
        String fullName,
        AuthProviderType provider,
        UserRoleType role,
        boolean passwordLoginEnabled,
        boolean googleLinked,
        boolean emailNotificationsEnabled,
        boolean aiAssistantEnabled
) {
}
