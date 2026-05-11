package com.socialpublish.integrations.reddit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RedditTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("scope") String scope
) {
}
