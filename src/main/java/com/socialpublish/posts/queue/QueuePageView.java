package com.socialpublish.posts.queue;

import com.socialpublish.posts.dto.PostView;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public record QueuePageView(
        List<PostView> posts,
        QueueStatsView stats,
        long totalFiltered,
        boolean hasMore,
        int nextSize,
        Instant nextScheduledAt
) {

    public long nextPostInMinutes() {
        if (nextScheduledAt == null) {
            return -1;
        }
        Duration duration = Duration.between(Instant.now(), nextScheduledAt);
        if (duration.isNegative()) {
            return 0;
        }
        long minutes = duration.toMinutes();
        return minutes == 0 ? 1 : minutes;
    }

    public LocalDateTime nextScheduledLocalDateTime() {
        if (nextScheduledAt == null) {
            return null;
        }
        return nextScheduledAt.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
