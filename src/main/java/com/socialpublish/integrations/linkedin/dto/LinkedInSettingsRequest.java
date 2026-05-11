package com.socialpublish.integrations.linkedin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkedInSettingsRequest {
    @NotBlank(message = "Access Token is required")
    private String accessToken;

    @NotBlank(message = "Author URN is required (e.g., urn:li:person:ABC or urn:li:organization:123)")
    private String authorUrn;

    private boolean enabled = true;
}
