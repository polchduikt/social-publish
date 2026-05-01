package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.ActivityBarView;
import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private final PostRepository postRepository;

    public DashboardService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public DashboardView buildDashboard(UUID ownerId) {
        List<Post> allPosts = postRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        List<PostView> allPostViews = allPosts.stream().map(PostView::from).toList();
        List<PostView> recentPosts = allPostViews.stream().limit(5).toList();

        DashboardStatsView stats = buildStats(allPosts);

        return new DashboardView(stats, buildActivityBars(allPosts), recentPosts, allPostViews);
    }

    private DashboardStatsView buildStats(List<Post> posts) {
        Map<PostStatus, Long> counts = new EnumMap<>(PostStatus.class);
        for (PostStatus status : PostStatus.values()) {
            counts.put(status, 0L);
        }

        for (Post post : posts) {
            counts.merge(post.getStatus(), 1L, Long::sum);
        }

        long total = posts.size();
        long published = counts.get(PostStatus.PUBLISHED);
        long failed = counts.get(PostStatus.FAILED);
        long attempts = published + failed;
        double successRate = attempts == 0 ? 0.0 : (published * 100.0) / attempts;

        return new DashboardStatsView(
                total,
                counts.get(PostStatus.DRAFT),
                counts.get(PostStatus.SCHEDULED),
                published,
                failed,
                successRate
        );
    }

    private List<ActivityBarView> buildActivityBars(List<Post> posts) {
        Map<DayOfWeek, Long> counts = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            counts.put(day, 0L);
        }

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate startDate = LocalDate.now(zoneId).minusDays(6);

        for (Post post : posts) {
            LocalDate postDate = post.getCreatedAt().atZone(zoneId).toLocalDate();
            if (!postDate.isBefore(startDate)) {
                DayOfWeek day = postDate.getDayOfWeek();
                counts.merge(day, 1L, Long::sum);
            }
        }

        long max = counts.values().stream().max(Comparator.naturalOrder()).orElse(1L);
        if (max == 0) {
            max = 1;
        }

        List<ActivityBarView> bars = new ArrayList<>();
        bars.add(mapBar("Mon", counts.get(DayOfWeek.MONDAY), max));
        bars.add(mapBar("Tue", counts.get(DayOfWeek.TUESDAY), max));
        bars.add(mapBar("Wed", counts.get(DayOfWeek.WEDNESDAY), max));
        bars.add(mapBar("Thu", counts.get(DayOfWeek.THURSDAY), max));
        bars.add(mapBar("Fri", counts.get(DayOfWeek.FRIDAY), max));
        bars.add(mapBar("Sat", counts.get(DayOfWeek.SATURDAY), max));
        bars.add(mapBar("Sun", counts.get(DayOfWeek.SUNDAY), max));
        return bars;
    }

    private ActivityBarView mapBar(String day, long value, long max) {
        int heightPercent = (int) Math.max(12, Math.round((value * 100.0) / max));
        return new ActivityBarView(day, value, heightPercent);
    }
}
