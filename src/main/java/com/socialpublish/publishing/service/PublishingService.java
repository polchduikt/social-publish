package com.socialpublish.publishing.service;

import com.socialpublish.integrations.telegram.service.TelegramPublisherService;
import com.socialpublish.integrations.discord.service.DiscordPublisherService;
import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishingService {

    private static final Logger log = LoggerFactory.getLogger(PublishingService.class);

    private final PostRepository postRepository;
    private final PostStatusMachine statusMachine;
    private final PublishingProducer publishingProducer;
    private final TelegramPublisherService telegramPublisherService;
    private final DiscordPublisherService discordPublisherService;
    private final NotificationService notificationService;

    @Transactional
    public void startScheduledPublish(UUID postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found, skipping scheduled publish", postId);
            return;
        }

        if (post.getStatus() != PostStatus.SCHEDULED) {
            log.info("Post {} is {}, not SCHEDULED — skipping scheduled publish", postId, post.getStatus());
            return;
        }

        statusMachine.transition(post, PostStatus.PUBLISHING);
        post.setRetryCount(0);
        postRepository.save(post);

        notificationService.sendPostUpdate(post.getOwner().getId(),
                PostNotification.publishing(postId, post.getTitle()));

        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishingProducer.sendPublishRequest(postId);
                        log.info("Post {} transitioned to PUBLISHING and sent to RabbitMQ", postId);
                    }
                }
        );
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

        UUID userId = post.getOwner().getId();

        if (post.getStatus() == PostStatus.RETRYING) {
            statusMachine.transition(post, PostStatus.PUBLISHING);
            notificationService.sendPostUpdate(userId,
                    PostNotification.publishing(postId, post.getTitle()));
        }

        try {
            doPublish(post);
            markPublished(post);
            notificationService.sendPostUpdate(userId,
                    PostNotification.published(postId, post.getTitle()));
        } catch (Exception ex) {
            log.error("Publishing failed for post {}: {}", postId, ex.getMessage());
            handleFailure(post, ex.getMessage(), attempt);
        }
    }

    private void doPublish(Post post) {
        String platforms = post.getPlatforms();
        if (platforms == null || platforms.isBlank()) {
            throw new RuntimeException("No platforms selected for publishing");
        }

        boolean anyPublished = false;
        StringBuilder errors = new StringBuilder();

        if (platforms.contains("TELEGRAM")) {
            try {
                telegramPublisherService.publish(post);
                anyPublished = true;
            } catch (Exception ex) {
                log.warn("Telegram publishing failed for post {}: {}", post.getId(), ex.getMessage());
                errors.append("Telegram: ").append(ex.getMessage());
            }
        }

        if (platforms.contains("DISCORD")) {
            try {
                discordPublisherService.publish(post);
                anyPublished = true;
            } catch (Exception ex) {
                log.warn("Discord publishing failed for post {}: {}", post.getId(), ex.getMessage());
                if (!errors.isEmpty()) errors.append("; ");
                errors.append("Discord: ").append(ex.getMessage());
            }
        }

        if (!anyPublished) {
            throw new RuntimeException(errors.toString());
        }
    }

    private void markPublished(Post post) {
        statusMachine.transition(post, PostStatus.PUBLISHED);
        post.setPublishedAt(Instant.now());
        post.setFailedReason(null);
        postRepository.save(post);
        log.info("Post {} published successfully", post.getId());
    }

    private void handleFailure(Post post, String reason, int attempt) {
        UUID userId = post.getOwner().getId();

        if (attempt < post.getMaxRetries()) {
            statusMachine.transition(post, PostStatus.RETRYING);
            post.setRetryCount(attempt);
            post.setFailedReason(reason);
            postRepository.save(post);
            publishingProducer.sendRetryRequest(post.getId(), attempt + 1);

            notificationService.sendPostUpdate(userId,
                    PostNotification.retrying(post.getId(), post.getTitle(), attempt, post.getMaxRetries()));
        } else {
            statusMachine.transition(post, PostStatus.FAILED);
            post.setRetryCount(attempt);
            post.setFailedReason(reason);
            postRepository.save(post);

            notificationService.sendPostUpdate(userId,
                    PostNotification.failed(post.getId(), post.getTitle(), reason));
        }
    }
}
