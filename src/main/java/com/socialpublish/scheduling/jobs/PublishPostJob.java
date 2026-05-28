package com.socialpublish.scheduling.jobs;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.publishing.service.PublishingService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class PublishPostJob implements Job {

    @Autowired
    private PublishingService publishingService;

    @Autowired
    private PostRepository postRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String postIdStr = context.getJobDetail().getJobDataMap().getString("postId");
        UUID postId = UUID.fromString(postIdStr);

        log.info("Quartz job triggered for post {}", postId);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found, skipping", postId);
            return;
        }

        if (post.getStatus() != PostStatus.SCHEDULED) {
            log.info("Post {} is {}, not SCHEDULED — skipping", postId, post.getStatus());
            return;
        }

        Duration delay = Duration.between(post.getScheduledAt(), Instant.now());

        if (delay.toMinutes() > 1) {
            log.info("Post {} is {}min late, routing to missed post handler", postId, delay.toMinutes());
            publishingService.handleMissedPost(postId);
        } else {
            publishingService.startScheduledPublish(postId);
        }
    }
}
