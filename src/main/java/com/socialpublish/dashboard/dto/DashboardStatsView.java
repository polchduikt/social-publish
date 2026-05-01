package com.socialpublish.dashboard.dto;

public record DashboardStatsView(
        long totalPosts,
        long draftPosts,
        long scheduledPosts,
        long publishedPosts,
        long failedPosts,
        double successRate
) {}
