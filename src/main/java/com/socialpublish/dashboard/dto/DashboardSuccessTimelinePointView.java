package com.socialpublish.dashboard.dto;

public record DashboardSuccessTimelinePointView(
        String label,
        String shortLabel,
        int successRate,
        int rateY,
        boolean showLabel,
        boolean hasActivity,
        double xOffset
) {
}
