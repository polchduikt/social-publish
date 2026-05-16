package com.socialpublish.posts.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostTemplateDto(
        UUID id,
        String templateName,
        String content,
        String platforms,
        List<String> formattedPlatforms,
        Instant updatedAt
) {}
