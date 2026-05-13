package com.socialpublish.integrations.notion.dto;

import java.util.List;
import java.util.UUID;

public record NotionSettingsView(
    List<NotionAccountView> accounts,
    boolean configured,
    boolean enabled
) {
    public record NotionAccountView(
        UUID id,
        String apiToken,
        String databaseId,
        String label,
        boolean enabled
    ) {}
}
