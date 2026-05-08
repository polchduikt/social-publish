package com.socialpublish.dashboard.dto;

public record DashboardActivityDayView(
        String day,
        long published,
        long scheduled,
        long failed,
        long total,
        int publishedHeight,
        int scheduledHeight,
        int failedHeight,
        int engagementHeight
) {
}
