package com.socialpublish.dashboard.service;

import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DashboardStatsBuilder {

    public DashboardStatsView build(List<Post> posts) {
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
                counts.get(PostStatus.PUBLISHING),
                published,
                counts.get(PostStatus.RETRYING),
                failed,
                counts.get(PostStatus.CANCELLED),
                successRate
        );
    }
}
