package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardSuccessTimelinePointView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DashboardTimelineBuilder {

    private static final int TIMELINE_DAYS = 14;
    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final int LABEL_INTERVAL_DAYS = 6;
    private static final int LABEL_FIT_THRESHOLD = 3;
    private static final int AXIS_MIN_PERCENT = 10;
    private static final double AXIS_SCALE_FACTOR = 0.8;
    private static final int SVG_BASELINE_Y = 90;
    private static final int AXIS_LABEL_MAX = 100;
    private static final int AXIS_LABEL_MID = 50;
    private static final int AXIS_LABEL_MIN = 0;
    private static final String DEFAULT_LINE_PATH = "M 0," + SVG_BASELINE_Y + " L 100," + SVG_BASELINE_Y;

    private record Point2D(double x, double y) {}

    public SuccessTimelineData build(List<Post> posts) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalDate startDate = today.minusDays(TIMELINE_DAYS - 1);

        Map<LocalDate, DailySuccessCounter> byDate = new LinkedHashMap<>();
        for (int i = 0; i < TIMELINE_DAYS; i++) {
            LocalDate d = startDate.plusDays(i);
            byDate.put(d, new DailySuccessCounter(d));
        }

        for (Post post : posts) {
            LocalDate updatedDate = post.getUpdatedAt().atZone(zoneId).toLocalDate();
            DailySuccessCounter counter = byDate.get(updatedDate);
            if (counter == null) continue;

            if (post.getStatus() == PostStatus.PUBLISHED) {
                counter.published++;
            } else if (post.getStatus() == PostStatus.FAILED) {
                counter.failed++;
            }
        }

        List<DailySuccessCounter> allDays = new ArrayList<>(byDate.values());
        boolean anyActivity = allDays.stream().anyMatch(d -> d.published + d.failed > 0);
        if (!anyActivity) {
            return new SuccessTimelineData(
                    List.of(),
                    DEFAULT_LINE_PATH,
                    "",
                    false,
                    AXIS_LABEL_MAX,
                    AXIS_LABEL_MID,
                    AXIS_LABEL_MIN
            );
        }

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US);
        DateTimeFormatter shortLabelFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.US);

        List<DashboardSuccessTimelinePointView> points = new ArrayList<>();
        for (int i = 0; i < allDays.size(); i++) {
            DailySuccessCounter raw = allDays.get(i);
            long attempts = raw.published + raw.failed;
            int rate = attempts > 0 ? (int) Math.round((raw.published * PERCENTAGE_MULTIPLIER) / attempts) : 0;
            boolean showLabel = (i % LABEL_INTERVAL_DAYS == 0) || (i == allDays.size() - 1 && (i % LABEL_INTERVAL_DAYS > LABEL_FIT_THRESHOLD));
            double xOffset = (i * PERCENTAGE_MULTIPLIER) / (allDays.size() - 1);
            points.add(new DashboardSuccessTimelinePointView(
                    raw.date.format(labelFormatter),
                    raw.date.format(shortLabelFormatter),
                    rate,
                    toAxisPercent(rate),
                    showLabel,
                    attempts > 0,
                    xOffset
            ));
        }

        String linePath = buildSmoothLinePath(points);
        String areaPath = linePath + " L 100," + SVG_BASELINE_Y + " L 0," + SVG_BASELINE_Y + " Z";

        return new SuccessTimelineData(
                points,
                linePath,
                areaPath,
                true,
                AXIS_LABEL_MAX,
                AXIS_LABEL_MID,
                AXIS_LABEL_MIN
        );
    }

    private String buildSmoothLinePath(List<DashboardSuccessTimelinePointView> points) {
        if (points.isEmpty()) return DEFAULT_LINE_PATH;

        List<Point2D> coords = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            double x = (i * PERCENTAGE_MULTIPLIER) / (points.size() - 1);
            double y = PERCENTAGE_MULTIPLIER - points.get(i).rateY();
            coords.add(new Point2D(x, y));
        }

        if (points.size() == 1) {
            Point2D single = coords.get(0);
            return "M " + round(single.x) + "," + round(single.y);
        }

        StringBuilder sb = new StringBuilder();
        Point2D first = coords.get(0);
        sb.append("M ").append(round(first.x)).append(",").append(round(first.y));

        for (int i = 0; i < coords.size() - 1; i++) {
            Point2D p1 = coords.get(i);
            Point2D p2 = coords.get(i + 1);
            double cp1x = p1.x + (p2.x - p1.x) / 2.0;
            sb.append(" C ").append(round(cp1x)).append(",").append(round(p1.y)).append(" ")
              .append(round(cp1x)).append(",").append(round(p2.y)).append(" ")
              .append(round(p2.x)).append(",").append(round(p2.y));
        }
        return sb.toString();
    }

    private int toAxisPercent(int rate) {
        int clamped = Math.max(0, Math.min((int) PERCENTAGE_MULTIPLIER, rate));
        return AXIS_MIN_PERCENT + (int) Math.round(clamped * AXIS_SCALE_FACTOR);
    }

    private String round(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static class DailySuccessCounter {
        private final LocalDate date;
        private long published;
        private long failed;
        private DailySuccessCounter(LocalDate date) { this.date = date; }
    }

    public record SuccessTimelineData(
            List<DashboardSuccessTimelinePointView> points,
            String linePath,
            String areaPath,
            boolean showArea,
            int axisMax,
            int axisMid,
            int axisMin
    ) {}
}
