package com.socialpublish.dashboard.dto;

public record DashboardStatsView(
        long totalPosts,
        long draftPosts,
        long scheduledPosts,
        long publishingPosts,
        long publishedPosts,
        long retryingPosts,
        long failedPosts,
        long cancelledPosts,
        double successRate
) {}
