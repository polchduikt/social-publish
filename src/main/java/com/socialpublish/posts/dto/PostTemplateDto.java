package com.socialpublish.posts.dto;

import java.time.Instant;
import java.util.UUID;

public record PostTemplateDto(
        UUID id,
        String templateName,
        String content,
        String platforms,
        Instant updatedAt
) {}
