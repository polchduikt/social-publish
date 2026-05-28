package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardActivityDayView;
import com.socialpublish.posts.entity.Post;
import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DashboardActivityBuilder {

    private static final int ACTIVITY_DAYS = 7;
    private static final int PUBLISHED_WEIGHT = 10;
    private static final int SCHEDULED_WEIGHT = 4;
    private static final int FAILED_PENALTY_WEIGHT = 6;
    private static final double PERCENTAGE_MULTIPLIER = 100.0;

    public List<DashboardActivityDayView> build(List<Post> posts) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalDate startDate = today.minusDays(ACTIVITY_DAYS - 1);

        Map<LocalDate, DailyActivityAccumulator> perDay = new LinkedHashMap<>();
        for (int i = 0; i < ACTIVITY_DAYS; i++) {
            perDay.put(startDate.plusDays(i), new DailyActivityAccumulator());
        }

        for (Post post : posts) {
            LocalDate postDate = post.getCreatedAt().atZone(zoneId).toLocalDate();
            DailyActivityAccumulator day = perDay.get(postDate);
            if (day == null) {
                continue;
            }
            switch (post.getStatus()) {
                case PUBLISHED -> day.published++;
                case FAILED -> day.failed++;
                case SCHEDULED, PUBLISHING, RETRYING -> day.scheduled++;
                default -> {
                }
            }
        }

        long maxTotal = perDay.values().stream()
                .mapToLong(DailyActivityAccumulator::total)
                .max()
                .orElse(1L);
        if (maxTotal < 1) maxTotal = 1L;

        long maxEngagement = perDay.values().stream()
                .mapToLong(DailyActivityAccumulator::engagementScore)
                .max()
                .orElse(1L);
        if (maxEngagement < 1) maxEngagement = 1L;

        List<DashboardActivityDayView> days = new ArrayList<>();
        for (Map.Entry<LocalDate, DailyActivityAccumulator> entry : perDay.entrySet()) {
            DailyActivityAccumulator acc = entry.getValue();
            long published = acc.published;
            long scheduled = acc.scheduled;
            long failed = acc.failed;
            long total = acc.total();
            long score = acc.engagementScore();

            days.add(new DashboardActivityDayView(
                    toDayKey(entry.getKey().getDayOfWeek()),
                    published,
                    scheduled,
                    failed,
                    total,
                    toPercent(published, maxTotal),
                    toPercent(scheduled, maxTotal),
                    toPercent(failed, maxTotal),
                    toPercent(score, maxEngagement)
            ));
        }
        return days;
    }

    private int toPercent(long value, long max) {
        if (max <= 0 || value <= 0) return 0;
        return (int) Math.round((value * PERCENTAGE_MULTIPLIER) / max);
    }

    private String toDayKey(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Mon";
            case TUESDAY -> "Tue";
            case WEDNESDAY -> "Wed";
            case THURSDAY -> "Thu";
            case FRIDAY -> "Fri";
            case SATURDAY -> "Sat";
            case SUNDAY -> "Sun";
        };
    }

    private static class DailyActivityAccumulator {
        private long published;
        private long scheduled;
        private long failed;

        private long total() {
            return published + scheduled + failed;
        }

        private long engagementScore() {
            long raw = (published * PUBLISHED_WEIGHT) + (scheduled * SCHEDULED_WEIGHT) - (failed * FAILED_PENALTY_WEIGHT);
            return Math.max(raw, 0);
        }
    }
}
