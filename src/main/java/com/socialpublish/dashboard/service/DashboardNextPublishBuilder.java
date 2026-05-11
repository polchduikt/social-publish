package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardNextPublishView;
import com.socialpublish.posts.dto.PostView;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DashboardNextPublishBuilder {

    public DashboardNextPublishView build(List<PostView> posts) {
        LocalDateTime now = LocalDateTime.now();

        PostView next = posts.stream()
                .filter(post -> post.scheduledAt() != null)
                .filter(post -> "SCHEDULED".equals(post.status())
                        || "PUBLISHING".equals(post.status())
                        || "RETRYING".equals(post.status()))
                .sorted((left, right) -> left.scheduledAt().compareTo(right.scheduledAt()))
                .findFirst()
                .orElse(null);

        if (next == null) return null;

        Duration delta = Duration.between(now, next.scheduledAt());
        boolean overdue = delta.isNegative();
        Duration absolute = delta.abs();

        long days = absolute.toDays();
        long hours = absolute.toHoursPart();
        long minutes = absolute.toMinutesPart();

        StringBuilder countdown = new StringBuilder();
        if (days > 0) countdown.append(days).append("d ");
        if (hours > 0 || days > 0) countdown.append(hours).append("h ");
        countdown.append(minutes).append("m");

        return new DashboardNextPublishView(
                next.id(),
                next.title(),
                next.excerpt(),
                next.scheduledAt(),
                countdown.toString().trim(),
                overdue,
                next.platformList()
        );
    }
}
