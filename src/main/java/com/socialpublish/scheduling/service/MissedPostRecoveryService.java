package com.socialpublish.scheduling.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.publishing.service.PublishingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissedPostRecoveryService {

    private final PostRepository postRepository;
    private final PublishingService publishingService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverMissedPosts() {
        List<Post> missedPosts = postRepository
                .findByStatusAndScheduledAtBefore(PostStatus.SCHEDULED, Instant.now());

        if (missedPosts.isEmpty()) {
            log.info("No missed scheduled posts found on startup");
            return;
        }

        log.warn("Found {} missed scheduled post(s), starting recovery...", missedPosts.size());

        int recovered = 0;
        int failed = 0;

        for (Post post : missedPosts) {
            try {
                publishingService.handleMissedPost(post.getId());
                recovered++;
            } catch (Exception e) {
                log.error("Failed to recover missed post {} ('{}'): {}",
                        post.getId(), post.getTitle(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("Missed post recovery completed: {} recovered, {} errors out of {} total",
                recovered, failed, missedPosts.size());
    }
}
