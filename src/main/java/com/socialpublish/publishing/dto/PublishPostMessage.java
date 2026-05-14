package com.socialpublish.publishing.dto;

import java.util.UUID;

public record PublishPostMessage(
        UUID postId,
        int attempt,
        boolean scheduled
) {}
