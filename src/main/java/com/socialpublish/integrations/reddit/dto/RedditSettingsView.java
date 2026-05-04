package com.socialpublish.integrations.reddit.dto;

import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;

public record RedditSettingsView(boolean configured, boolean enabled, String defaultSubreddit) {
    public static RedditSettingsView from(RedditSettingsEntity entity) {
        boolean isConfigured = entity.getRefreshToken() != null && !entity.getRefreshToken().isBlank();
        return new RedditSettingsView(isConfigured, entity.isEnabled(), entity.getDefaultSubreddit() != null ? entity.getDefaultSubreddit() : "");
    }

    public static RedditSettingsView empty() {
        return new RedditSettingsView(false, false, "");
    }
}
