package com.socialpublish.notifications.dto;

public record NotificationItemResponse(
        String id,
        String postId,
        String title,
        String message,
        String type,
        String status,
        boolean read,
        String timestamp
) {
}
