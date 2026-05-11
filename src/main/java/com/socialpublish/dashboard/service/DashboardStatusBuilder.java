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
        if (totalCount == 0) return "conic-gradient(#2a3d61 0deg 360deg)";

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

    private int toPercent(long value, long max) {
        if (max <= 0 || value <= 0) return 0;
        return (int) Math.round((value * 100.0) / max);
    }

    private String round(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
