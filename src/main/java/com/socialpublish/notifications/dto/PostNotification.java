package com.socialpublish.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record PostNotification(
        UUID postId,
        String title,
        String status,
        String message,
        String type,
        Instant timestamp
) {
}
