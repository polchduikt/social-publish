package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.mapper.PostMapper;
import com.socialpublish.posts.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostMapper postMapper;
    @Mock private DashboardStatsBuilder statsBuilder;
    @Mock private DashboardActivityBuilder activityBuilder;
    @Mock private DashboardTimelineBuilder timelineBuilder;
    @Mock private DashboardStatusBuilder statusBuilder;
    @Mock private DashboardNextPublishBuilder nextPublishBuilder;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void buildDashboard_Success() {
        UUID ownerId = UUID.randomUUID();
        when(postRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId)).thenReturn(List.of(new Post()));
        when(postMapper.toViews(any())).thenReturn(List.of());
        when(statsBuilder.build(any())).thenReturn(new DashboardStatsView(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0));
        when(activityBuilder.build(any())).thenReturn(List.of());
        when(statusBuilder.buildSlices(any())).thenReturn(List.of());
        when(timelineBuilder.build(any())).thenReturn(new DashboardTimelineBuilder.SuccessTimelineData(List.of(), "", "", false, 0, 0, 0));
        when(nextPublishBuilder.build(any())).thenReturn(null);

        DashboardView result = dashboardService.buildDashboard(ownerId);

        assertNotNull(result);
        assertNotNull(result.stats());
    }
}
