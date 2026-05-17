package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardActivityDayView;
import com.socialpublish.dashboard.dto.DashboardNextPublishView;
import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.dto.DashboardStatusSliceView;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.mapper.PostMapper;
import com.socialpublish.posts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final DashboardStatsBuilder statsBuilder;
    private final DashboardActivityBuilder activityBuilder;
    private final DashboardTimelineBuilder timelineBuilder;
    private final DashboardStatusBuilder statusBuilder;
    private final DashboardNextPublishBuilder nextPublishBuilder;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "#ownerId")
    public DashboardView buildDashboard(UUID ownerId) {
        List<Post> allPosts = postRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        List<PostView> allPostViews = postMapper.toViews(allPosts);
        List<PostView> recentPosts = allPostViews.stream().limit(RECENT_ACTIVITY_LIMIT).toList();

        DashboardStatsView stats = statsBuilder.build(allPosts);
        List<DashboardActivityDayView> activityDays = activityBuilder.build(allPosts);
        List<DashboardStatusSliceView> statusSlices = statusBuilder.buildSlices(stats);
        DashboardTimelineBuilder.SuccessTimelineData successTimeline = timelineBuilder.build(allPosts);
        DashboardNextPublishView nextPublish = nextPublishBuilder.build(allPostViews);

        return new DashboardView(
                stats,
                activityDays,
                statusSlices,
                statusBuilder.buildDonutGradient(statusSlices),
                successTimeline.points(),
                successTimeline.linePath(),
                successTimeline.areaPath(),
                successTimeline.showArea(),
                successTimeline.axisMax(),
                successTimeline.axisMid(),
                successTimeline.axisMin(),
                nextPublish,
                recentPosts,
                allPostViews
        );
    }
}
