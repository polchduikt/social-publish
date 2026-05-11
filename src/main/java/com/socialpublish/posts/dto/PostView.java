package com.socialpublish.posts.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostView(
        UUID id,
        String title,
        String content,
        String excerpt,
        String status,
        LocalDateTime scheduledAt,
        LocalDateTime publishedAt,
        String failedReason,
        int retryCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String platforms,
        List<String> platformList,
        List<PostMediaView> media
) {
}
