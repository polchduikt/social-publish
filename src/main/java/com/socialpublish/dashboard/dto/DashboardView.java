package com.socialpublish.dashboard.dto;

import com.socialpublish.posts.dto.PostView;

import java.util.List;

public record DashboardView(
        DashboardStatsView stats,
        List<DashboardActivityDayView> activityDays,
        List<DashboardStatusSliceView> statusSlices,
        String statusDonutGradient,
        List<DashboardSuccessTimelinePointView> successTimeline,
        String successTimelineLinePath,
        String successTimelineAreaPath,
        boolean successTimelineShowArea,
        int successTimelineAxisMax,
        int successTimelineAxisMid,
        int successTimelineAxisMin,
        DashboardNextPublishView nextPublish,
        List<PostView> recentPosts,
        List<PostView> allPosts
) {}
