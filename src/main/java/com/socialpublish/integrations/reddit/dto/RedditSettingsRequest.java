package com.socialpublish.integrations.reddit.dto;

import lombok.Data;

@Data
public class RedditSettingsRequest {
    private String defaultSubreddit;
    private boolean enabled;
}
