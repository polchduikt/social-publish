package com.socialpublish.notifications.dto;

import com.socialpublish.posts.entity.PostStatus;

import java.time.Instant;
import java.util.UUID;

public record PostNotification(
        UUID postId,
        String title,
        PostStatus status,
        String message,
        String type,
        Instant timestamp
) {

    public static PostNotification publishing(UUID postId, String title) {
        return new PostNotification(postId, title, PostStatus.PUBLISHING,
                "Publishing...", "info", Instant.now());
    }

    public static PostNotification published(UUID postId, String title) {
        return new PostNotification(postId, title, PostStatus.PUBLISHED,
                "Published", "success", Instant.now());
    }

    public static PostNotification failed(UUID postId, String title, String reason) {
        return new PostNotification(postId, title, PostStatus.FAILED,
                "Failed - " + reason, "error", Instant.now());
    }

    public static PostNotification retrying(UUID postId, String title, int attempt, int maxRetries) {
        return new PostNotification(postId, title, PostStatus.RETRYING,
                "Retrying... (" + attempt + "/" + maxRetries + ")", "warning", Instant.now());
    }
}
