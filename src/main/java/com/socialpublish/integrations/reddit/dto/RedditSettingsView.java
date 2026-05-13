package com.socialpublish.integrations.reddit.dto;

public record RedditSettingsView(boolean configured, boolean enabled, String defaultSubreddit, String label) {
}
