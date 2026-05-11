package com.socialpublish.integrations.notion.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record NotionSettingsView(
    UUID id,
    String apiToken,
    String databaseId,
    boolean enabled,
    boolean configured
) {
}
