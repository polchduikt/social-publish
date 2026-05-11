package com.socialpublish.integrations.linkedin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LinkedInTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") Integer expiresIn
) {
}
