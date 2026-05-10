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

    public String nextPostCountdown() {
        if (nextScheduledAt == null) {
            return "-";
        }
        Duration duration = Duration.between(Instant.now(), nextScheduledAt);
        Duration absolute = duration.abs();

        long days = absolute.toDays();
        long hours = absolute.toHoursPart();
        long minutes = absolute.toMinutesPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        sb.append(minutes).append("m");
        return sb.toString().trim();
    }

    public LocalDateTime nextScheduledLocalDateTime() {
        if (nextScheduledAt == null) {
            return null;
        }
        return nextScheduledAt.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
