package com.socialpublish.integrations.notion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class NotionSettingsRequest {
    private UUID id;

    @NotBlank(message = "API Token is required")
    private String apiToken;

    @NotBlank(message = "Database ID is required")
    private String databaseId;

    private String label;

    private Boolean enabled = true;
}
