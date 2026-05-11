package com.socialpublish.integrations.reddit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RedditSubmitResponse(
    @JsonProperty("success") Boolean success,
    @JsonProperty("jquery") Object jquery
) {
}
