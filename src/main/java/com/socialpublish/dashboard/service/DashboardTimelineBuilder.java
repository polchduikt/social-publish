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

    public SuccessTimelineData build(List<Post> posts) {
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
            return new SuccessTimelineData(List.of(), "M 0,90 L 100,90", "", false, 100, 50, 0);
        }

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US);
        DateTimeFormatter shortLabelFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.US);

        List<DashboardSuccessTimelinePointView> points = new ArrayList<>();
        for (int i = 0; i < allDays.size(); i++) {
            DailySuccessCounter raw = allDays.get(i);
            long attempts = raw.published + raw.failed;
            int rate = attempts > 0 ? (int) Math.round((raw.published * 100.0) / attempts) : 0;
            boolean showLabel = (i % 6 == 0) || (i == allDays.size() - 1 && (i % 6 > 3));
            double xOffset = (i * 100.0) / (allDays.size() - 1);
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
        String areaPath = linePath + " L 100,90 L 0,90 Z";

        return new SuccessTimelineData(points, linePath, areaPath, true, 100, 50, 0);
    }

    private String buildSmoothLinePath(List<DashboardSuccessTimelinePointView> points) {
        if (points.isEmpty()) return "M 0,90 L 100,90";

        double[][] coords = new double[points.size()][2];
        for (int i = 0; i < points.size(); i++) {
            coords[i][0] = (i * 100.0) / (points.size() - 1);
            coords[i][1] = 100 - points.get(i).rateY();
        }

        if (points.size() == 1) return "M " + round(coords[0][0]) + "," + round(coords[0][1]);

        StringBuilder sb = new StringBuilder();
        sb.append("M ").append(round(coords[0][0])).append(",").append(round(coords[0][1]));

        for (int i = 0; i < coords.length - 1; i++) {
            double[] p1 = coords[i];
            double[] p2 = coords[i + 1];
            double cp1x = p1[0] + (p2[0] - p1[0]) / 2.0;
            sb.append(" C ").append(round(cp1x)).append(",").append(round(p1[1])).append(" ")
              .append(round(cp1x)).append(",").append(round(p2[1])).append(" ")
              .append(round(p2[0])).append(",").append(round(p2[1]));
        }
        return sb.toString();
    }

    private int toAxisPercent(int rate) {
        int clamped = Math.max(0, Math.min(100, rate));
        return 10 + (int) Math.round(clamped * 0.8);
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
