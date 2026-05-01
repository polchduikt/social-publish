package com.socialpublish.dashboard.dto;

import com.socialpublish.posts.dto.PostView;

import java.util.List;

public record DashboardView(
        DashboardStatsView stats,
        List<ActivityBarView> activityBars,
        List<PostView> recentPosts,
        List<PostView> allPosts
) {}
