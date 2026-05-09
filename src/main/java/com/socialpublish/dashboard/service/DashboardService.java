package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardActivityDayView;
import com.socialpublish.dashboard.dto.DashboardNextPublishView;
import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.dto.DashboardStatusSliceView;
import com.socialpublish.dashboard.dto.DashboardSuccessTimelinePointView;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public DashboardView buildDashboard(UUID ownerId) {
        List<Post> allPosts = postRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        List<PostView> allPostViews = allPosts.stream().map(PostView::from).toList();
        List<PostView> recentPosts = allPostViews.stream().limit(RECENT_ACTIVITY_LIMIT).toList();

        DashboardStatsView stats = buildStats(allPosts);
        List<DashboardActivityDayView> activityDays = buildActivityDays(allPosts);
        List<DashboardStatusSliceView> statusSlices = buildStatusSlices(stats);
        SuccessTimelineData successTimeline = buildSuccessTimeline(allPosts);
        DashboardNextPublishView nextPublish = buildNextPublish(allPostViews);

        return new DashboardView(
                stats,
                activityDays,
                statusSlices,
                buildDonutGradient(statusSlices),
                successTimeline.points(),
                successTimeline.linePath(),
                successTimeline.areaPath(),
                successTimeline.showArea(),
                successTimeline.axisMax(),
                successTimeline.axisMid(),
                successTimeline.axisMin(),
                nextPublish,
                recentPosts,
                allPostViews
        );
    }

    private DashboardNextPublishView buildNextPublish(List<PostView> posts) {
        LocalDateTime now = LocalDateTime.now();

        PostView next = posts.stream()
                .filter(post -> post.scheduledAt() != null)
                .filter(post -> post.status() == PostStatus.SCHEDULED
                        || post.status() == PostStatus.PUBLISHING
                        || post.status() == PostStatus.RETRYING)
                .sorted((left, right) -> left.scheduledAt().compareTo(right.scheduledAt()))
                .findFirst()
                .orElse(null);

        if (next == null) {
            return null;
        }

        Duration delta = Duration.between(now, next.scheduledAt());
        boolean overdue = delta.isNegative();
        Duration absolute = delta.abs();

        long days = absolute.toDays();
        long hours = absolute.toHoursPart();
        long minutes = absolute.toMinutesPart();

        StringBuilder countdown = new StringBuilder();
        if (days > 0) {
            countdown.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            countdown.append(hours).append("h ");
        }
        countdown.append(minutes).append("m");

        List<String> platforms = next.platformList();

        return new DashboardNextPublishView(
                next.id(),
                next.title(),
                next.excerpt(),
                next.scheduledAt(),
                countdown.toString().trim(),
                overdue,
                platforms
        );
    }

    private DashboardStatsView buildStats(List<Post> posts) {
        Map<PostStatus, Long> counts = new EnumMap<>(PostStatus.class);
        for (PostStatus status : PostStatus.values()) {
            counts.put(status, 0L);
        }

        for (Post post : posts) {
            counts.merge(post.getStatus(), 1L, Long::sum);
        }

        long total = posts.size();
        long published = counts.get(PostStatus.PUBLISHED);
        long failed = counts.get(PostStatus.FAILED);
        long attempts = published + failed;
        double successRate = attempts == 0 ? 0.0 : (published * 100.0) / attempts;

        return new DashboardStatsView(
                total,
                counts.get(PostStatus.DRAFT),
                counts.get(PostStatus.SCHEDULED),
                counts.get(PostStatus.PUBLISHING),
                published,
                counts.get(PostStatus.RETRYING),
                failed,
                counts.get(PostStatus.CANCELLED),
                successRate
        );
    }

    private List<DashboardActivityDayView> buildActivityDays(List<Post> posts) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalDate startDate = today.minusDays(6);

        Map<LocalDate, DailyActivityAccumulator> perDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
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
        if (maxTotal < 1) {
            maxTotal = 1L;
        }

        long maxEngagement = perDay.values().stream()
                .mapToLong(DailyActivityAccumulator::engagementScore)
                .max()
                .orElse(1L);
        if (maxEngagement < 1) {
            maxEngagement = 1L;
        }

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

    private List<DashboardStatusSliceView> buildStatusSlices(DashboardStatsView stats) {
        long total = Math.max(1, stats.totalPosts());
        return List.of(
                new DashboardStatusSliceView("dash.draft", stats.draftPosts(), toPercent(stats.draftPosts(), total), "draft"),
                new DashboardStatusSliceView("dash.scheduled", stats.scheduledPosts(), toPercent(stats.scheduledPosts(), total), "scheduled"),
                new DashboardStatusSliceView("dash.published", stats.publishedPosts(), toPercent(stats.publishedPosts(), total), "published"),
                new DashboardStatusSliceView("dash.failed", stats.failedPosts(), toPercent(stats.failedPosts(), total), "failed")
        );
    }

    private String buildDonutGradient(List<DashboardStatusSliceView> slices) {
        long totalCount = slices.stream().mapToLong(DashboardStatusSliceView::count).sum();
        if (totalCount == 0) {
            return "conic-gradient(#2a3d61 0deg 360deg)";
        }

        Map<String, String> colors = Map.of(
                "draft", "#93a7c9",
                "scheduled", "#4f83ff",
                "published", "#2ac978",
                "failed", "#f87171"
        );

        double start = 0.0;
        List<String> parts = new ArrayList<>();
        for (DashboardStatusSliceView slice : slices) {
            double fraction = (double) slice.count() / totalCount;
            double end = start + (fraction * 360.0);
            String color = colors.getOrDefault(slice.cssClass(), "#4f83ff");
            parts.add(color + " " + round(start) + "deg " + round(end) + "deg");
            start = end;
        }
        return "conic-gradient(" + String.join(", ", parts) + ")";
    }

    private SuccessTimelineData buildSuccessTimeline(List<Post> posts) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalDate startDate = today.minusDays(13);

        Map<LocalDate, DailySuccessCounter> byDate = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            LocalDate d = startDate.plusDays(i);
            byDate.put(d, new DailySuccessCounter(d));
        }

        for (Post post : posts) {
            LocalDate updatedDate = post.getUpdatedAt().atZone(zoneId).toLocalDate();
            DailySuccessCounter counter = byDate.get(updatedDate);
            if (counter == null) {
                continue;
            }
            if (post.getStatus() == PostStatus.PUBLISHED) {
                counter.published++;
            } else if (post.getStatus() == PostStatus.FAILED) {
                counter.failed++;
            }
        }

        List<DailySuccessCounter> allDays = new ArrayList<>(byDate.values());

        boolean anyActivity = allDays.stream().anyMatch(d -> d.published + d.failed > 0);
        if (!anyActivity) {
            return new SuccessTimelineData(List.of(), "M 0,90 L 100,90", "", false, 100, 50, 0);
        }

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US);
        DateTimeFormatter shortLabelFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.US);

        List<DashboardSuccessTimelinePointView> points = new ArrayList<>();
        for (int i = 0; i < allDays.size(); i++) {
            DailySuccessCounter raw = allDays.get(i);
            long attempts = raw.published + raw.failed;
            boolean hasActivity = attempts > 0;
            int rate = hasActivity ? (int) Math.round((raw.published * 100.0) / attempts) : 0;
            boolean showLabel = (i % 6 == 0) || (i == allDays.size() - 1 && (i % 6 > 3));
            double xOffset = (i * 100.0) / (allDays.size() - 1);
            points.add(new DashboardSuccessTimelinePointView(
                    raw.date.format(labelFormatter),
                    raw.date.format(shortLabelFormatter),
                    rate,
                    toAxisPercent(rate),
                    showLabel,
                    hasActivity,
                    xOffset
            ));
        }

        String linePath = buildSmoothLinePath(points);
        String areaPath = buildSmoothAreaPath(points);

        return new SuccessTimelineData(points, linePath, areaPath, true, 100, 50, 0);
    }

    private String buildSmoothLinePath(List<DashboardSuccessTimelinePointView> points) {
        if (points.isEmpty()) {
            return "M 0,90 L 100,90";
        }

        double[][] coords = new double[points.size()][2];
        for (int i = 0; i < points.size(); i++) {
            coords[i][0] = (i * 100.0) / (points.size() - 1);
            coords[i][1] = 100 - points.get(i).rateY();
        }

        if (points.size() == 1) {
            return "M " + round(coords[0][0]) + "," + round(coords[0][1]);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("M ").append(round(coords[0][0])).append(",").append(round(coords[0][1]));

        for (int i = 0; i < coords.length - 1; i++) {
            double[] p1 = coords[i];
            double[] p2 = coords[i + 1];

            double cp1x = p1[0] + (p2[0] - p1[0]) / 2.0;
            double cp1y = p1[1];
            double cp2x = cp1x;
            double cp2y = p2[1];

            sb.append(" C ")
              .append(round(cp1x)).append(",").append(round(cp1y)).append(" ")
              .append(round(cp2x)).append(",").append(round(cp2y)).append(" ")
              .append(round(p2[0])).append(",").append(round(p2[1]));
        }

        return sb.toString();
    }

    private String buildSmoothAreaPath(List<DashboardSuccessTimelinePointView> points) {
        String linePath = buildSmoothLinePath(points);
        if (points.isEmpty()) {
            return "";
        }
        return linePath + " L 100,90 L 0,90 Z";
    }

    private int toAxisPercent(int rate) {
        int clamped = Math.max(0, Math.min(100, rate));
        return 10 + (int) Math.round(clamped * 0.8);
    }

    private int toPercent(long value, long max) {
        if (max <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.round((value * 100.0) / max);
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

    private String round(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static class DailyActivityAccumulator {
        private long published;
        private long scheduled;
        private long failed;

        private long total() {
            return published + scheduled + failed;
        }

        private long engagementScore() {
            long raw = (published * 10) + (scheduled * 4) - (failed * 6);
            return Math.max(raw, 0);
        }
    }

    private static class DailySuccessCounter {
        private final LocalDate date;
        private long published;
        private long failed;

        private DailySuccessCounter(LocalDate date) {
            this.date = date;
        }

        private LocalDate date() {
            return date;
        }
    }

    private record SuccessTimelineData(
            List<DashboardSuccessTimelinePointView> points,
            String linePath,
            String areaPath,
            boolean showArea,
            int axisMax,
            int axisMid,
            int axisMin
    ) {
    }
}
