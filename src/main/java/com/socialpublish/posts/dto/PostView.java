package com.socialpublish.posts.dto;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public record PostView(
        UUID id,
        String title,
        String content,
        PostStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime publishedAt,
        String failedReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    private static final int EXCERPT_MAX_LENGTH = 88;
    private static final int EXCERPT_TRIM_LENGTH = 85;

    public static PostView from(Post post) {
        ZoneId zoneId = ZoneId.systemDefault();
        return new PostView(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                post.getScheduledAt() == null ? null : post.getScheduledAt().atZone(zoneId).toLocalDateTime(),
                post.getPublishedAt() == null ? null : post.getPublishedAt().atZone(zoneId).toLocalDateTime(),
                post.getFailedReason(),
                post.getCreatedAt().atZone(zoneId).toLocalDateTime(),
                post.getUpdatedAt().atZone(zoneId).toLocalDateTime()
        );
    }

    public String excerpt() {
        if (content == null) {
            return "";
        }
        if (content.length() <= EXCERPT_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, EXCERPT_TRIM_LENGTH) + "...";
    }
}
