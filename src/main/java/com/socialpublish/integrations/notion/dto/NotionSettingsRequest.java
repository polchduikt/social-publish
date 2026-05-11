package com.socialpublish.integrations.notion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotionSettingsRequest {
    @NotBlank(message = "API Token is required")
    private String apiToken;

    @NotBlank(message = "Database ID is required")
    private String databaseId;

    private boolean enabled = true;
}
