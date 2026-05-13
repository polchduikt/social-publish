package com.socialpublish.integrations.notion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class NotionSettingsRequest {
    private UUID id;

    @NotBlank(message = "API Token is required")
    private String apiToken;

    @NotBlank(message = "Database ID is required")
    private String databaseId;

    private String label;

    private Boolean enabled = true;
}
