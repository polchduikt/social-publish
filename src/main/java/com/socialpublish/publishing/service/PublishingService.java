package com.socialpublish.publishing.service;

import com.socialpublish.integrations.telegram.service.TelegramPublisherService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PublishingService {

    private static final Logger log = LoggerFactory.getLogger(PublishingService.class);

    private final PostRepository postRepository;
    private final PostStatusMachine statusMachine;
    private final PublishingProducer publishingProducer;
    private final TelegramPublisherService telegramPublisherService;

    public PublishingService(
            PostRepository postRepository,
            PostStatusMachine statusMachine,
            PublishingProducer publishingProducer,
            TelegramPublisherService telegramPublisherService
    ) {
        this.postRepository = postRepository;
        this.statusMachine = statusMachine;
        this.publishingProducer = publishingProducer;
        this.telegramPublisherService = telegramPublisherService;
    }

    @Transactional
    public void attemptPublish(UUID postId, int attempt) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found, skipping publish", postId);
            return;
        }

        if (post.getStatus() != PostStatus.PUBLISHING && post.getStatus() != PostStatus.RETRYING) {
            log.info("Post {} is {}, skipping publish", postId, post.getStatus());
            return;
        }

        if (post.getStatus() == PostStatus.RETRYING) {
            statusMachine.transition(post, PostStatus.PUBLISHING);
        }

        try {
            doPublish(post);
            markPublished(post);
        } catch (Exception ex) {
            log.error("Publishing failed for post {}: {}", postId, ex.getMessage());
            handleFailure(post, ex.getMessage(), attempt);
        }
    }

    private void doPublish(Post post) {
        telegramPublisherService.publish(post);
    }

    private void markPublished(Post post) {
        statusMachine.transition(post, PostStatus.PUBLISHED);
        post.setPublishedAt(Instant.now());
        post.setFailedReason(null);
        postRepository.save(post);
        log.info("Post {} published successfully", post.getId());
    }

    private void handleFailure(Post post, String reason, int attempt) {
        if (attempt < post.getMaxRetries()) {
            statusMachine.transition(post, PostStatus.RETRYING);
            post.setRetryCount(attempt);
            post.setFailedReason(reason);
            postRepository.save(post);
            publishingProducer.sendRetryRequest(post.getId(), attempt + 1);
            log.info("Post {} queued for retry (attempt {}/{})", post.getId(), attempt + 1, post.getMaxRetries());
        } else {
            statusMachine.transition(post, PostStatus.FAILED);
            post.setRetryCount(attempt);
            post.setFailedReason(reason);
            postRepository.save(post);
            log.warn("Post {} failed permanently after {} attempts", post.getId(), attempt);
        }
    }
}
