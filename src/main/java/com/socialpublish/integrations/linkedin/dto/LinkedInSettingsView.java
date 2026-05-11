package com.socialpublish.integrations.linkedin.dto;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

@Builder
public record LinkedInSettingsView(
    UUID id,
    String accessToken,
    String authorUrn,
    Instant expiresAt,
    boolean enabled,
    boolean configured
) {
}
