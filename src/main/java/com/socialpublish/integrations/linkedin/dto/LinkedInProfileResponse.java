package com.socialpublish.integrations.linkedin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LinkedInProfileResponse(
    @JsonProperty("sub") String sub
) {
}
