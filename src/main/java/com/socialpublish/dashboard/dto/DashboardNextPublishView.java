package com.socialpublish.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DashboardNextPublishView(
        UUID postId,
        String title,
        String excerpt,
        LocalDateTime scheduledAt,
        String countdownValue,
        boolean overdue,
        List<String> platforms
) {
}
