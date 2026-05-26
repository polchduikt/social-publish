package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.dto.DashboardStatusSliceView;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DashboardStatusBuilder {

    private static final double FULL_CIRCLE_DEGREES = 360.0;
    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final String DEFAULT_GRADIENT = "conic-gradient(#2a3d61 0deg 360deg)";
    private static final String COLOR_DRAFT = "#93a7c9";
    private static final String COLOR_SCHEDULED = "#4f83ff";
    private static final String COLOR_PUBLISHED = "#2ac978";
    private static final String COLOR_FAILED = "#f87171";

    public List<DashboardStatusSliceView> buildSlices(DashboardStatsView stats) {
        long total = Math.max(1, stats.totalPosts());
        return List.of(
                new DashboardStatusSliceView("dash.draft", stats.draftPosts(), toPercent(stats.draftPosts(), total), "draft"),
                new DashboardStatusSliceView("dash.scheduled", stats.scheduledPosts(), toPercent(stats.scheduledPosts(), total), "scheduled"),
                new DashboardStatusSliceView("dash.published", stats.publishedPosts(), toPercent(stats.publishedPosts(), total), "published"),
                new DashboardStatusSliceView("dash.failed", stats.failedPosts(), toPercent(stats.failedPosts(), total), "failed")
        );
    }

    public String buildDonutGradient(List<DashboardStatusSliceView> slices) {
        long totalCount = slices.stream().mapToLong(DashboardStatusSliceView::count).sum();
        if (totalCount == 0) return DEFAULT_GRADIENT;

        Map<String, String> colors = Map.of(
                "draft", COLOR_DRAFT,
                "scheduled", COLOR_SCHEDULED,
                "published", COLOR_PUBLISHED,
                "failed", COLOR_FAILED
        );

        double start = 0.0;
        List<String> parts = new ArrayList<>();
        for (DashboardStatusSliceView slice : slices) {
            double fraction = (double) slice.count() / totalCount;
            double end = start + (fraction * FULL_CIRCLE_DEGREES);
            String color = colors.getOrDefault(slice.cssClass(), COLOR_SCHEDULED);
            parts.add(color + " " + round(start) + "deg " + round(end) + "deg");
            start = end;
        }
        return "conic-gradient(" + String.join(", ", parts) + ")";
    }

    private int toPercent(long value, long max) {
        if (max <= 0 || value <= 0) return 0;
        return (int) Math.round((value * PERCENTAGE_MULTIPLIER) / max);
    }

    private String round(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
