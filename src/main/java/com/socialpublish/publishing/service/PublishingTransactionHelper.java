package com.socialpublish.publishing.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import com.socialpublish.posts.service.RecurringPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishingTransactionHelper {

    private final PostRepository postRepository;
    private final PostStatusMachine statusMachine;
    private final RecurringPostService recurringPostService;

    @Transactional
    public Post preparePublishing(UUID postId) {
        Post post = postRepository.findWithMediaAndOwnerById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found, skipping publish", postId);
            return null;
        }
        Hibernate.initialize(post.getOwner());
        Hibernate.initialize(post.getMedia());

        if (post.getStatus() != PostStatus.PUBLISHING && post.getStatus() != PostStatus.RETRYING) {
            log.info("Post {} is {}, skipping publish", postId, post.getStatus());
            return null;
        }

        if (post.getStatus() == PostStatus.RETRYING) {
            statusMachine.transition(post, PostStatus.PUBLISHING);
            post = postRepository.save(post);
        }

        return post;
    }

    @Transactional
    public Post markPublished(UUID postId) {
        Post post = postRepository.findWithMediaAndOwnerById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found when marking published", postId);
            return null;
        }
        Hibernate.initialize(post.getOwner());
        Hibernate.initialize(post.getMedia());
        statusMachine.transition(post, PostStatus.PUBLISHED);
        post.setPublishedAt(Instant.now());
        post.setFailedReason(null);
        post = postRepository.save(post);
        log.info("Post {} marked as PUBLISHED in database", postId);

        if (post.isRecurring()) {
            try {
                recurringPostService.createNextOccurrence(post).ifPresent(nextPost ->
                        log.info("Next recurring post {} scheduled for {}", nextPost.getId(), nextPost.getScheduledAt())
                );
            } catch (Exception ex) {
                log.error("Failed to create next recurring occurrence for post {}: {}", post.getId(), ex.getMessage());
            }
        }
        return post;
    }

    @Transactional
    public FailureResult handleFailure(UUID postId, String reason, int attempt) {
        Post post = postRepository.findWithMediaAndOwnerById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found when handling failure", postId);
            return null;
        }
        Hibernate.initialize(post.getOwner());
        Hibernate.initialize(post.getMedia());

        boolean isRetry = attempt < post.getMaxRetries();
        if (isRetry) {
            statusMachine.transition(post, PostStatus.RETRYING);
            post.setRetryCount(attempt);
            post.setFailedReason(reason);
            post = postRepository.save(post);
        } else {
            statusMachine.transition(post, PostStatus.FAILED);
            post.setRetryCount(attempt);
            post.setFailedReason(reason);
            post = postRepository.save(post);
        }

        return new FailureResult(post, isRetry);
    }

    public record FailureResult(Post post, boolean isRetry) {}
}
