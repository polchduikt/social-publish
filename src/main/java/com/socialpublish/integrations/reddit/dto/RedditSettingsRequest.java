package com.socialpublish.integrations.reddit.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedditSettingsRequest {
    @Size(max = 64, message = "Subreddit must be at most 64 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9_\\-]*$",
            message = "Subreddit can contain only letters, numbers, underscore and hyphen"
    )
    private String defaultSubreddit;

    @Size(max = 120, message = "Label must be at most 120 characters")
    private String label;

    private boolean enabled;
}
