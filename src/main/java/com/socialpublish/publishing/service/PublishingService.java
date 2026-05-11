package com.socialpublish.publishing.service;

import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import com.socialpublish.publishing.entity.Platform;
import com.socialpublish.publishing.event.PostScheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishingService {

    private final PostRepository postRepository;
    private final PostStatusMachine statusMachine;
    private final PublishingProducer publishingProducer;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final List<PlatformPublisher> publishers;

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
                new PostNotification(postId, post.getTitle(), "PUBLISHING", "Publishing...", "info", Instant.now()));

        eventPublisher.publishEvent(new PostScheduledEvent(postId));
        log.info("Post {} transitioned to PUBLISHING and event published", postId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostScheduled(PostScheduledEvent event) {
        publishingProducer.sendPublishRequest(event.postId());
        log.info("Post {} sent to RabbitMQ after commit", event.postId());
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
                    new PostNotification(postId, post.getTitle(), "PUBLISHING", "Publishing...", "info", Instant.now()));
        }

        try {
            doPublish(post);
            markPublished(post);
            notificationService.sendPostUpdate(userId,
                    new PostNotification(postId, post.getTitle(), "PUBLISHED", "Published", "success", Instant.now()));
        } catch (Exception ex) {
            log.error("Publishing failed for post {}: {}", postId, ex.getMessage());
            handleFailure(post, ex.getMessage(), attempt);
        }
    }

    private void doPublish(Post post) {
        Set<Platform> requestedPlatforms = parsePlatforms(post.getPlatforms());
        if (requestedPlatforms.isEmpty()) {
            throw new RuntimeException("No platforms selected for publishing");
        }

        boolean anyPublished = false;
        StringBuilder errors = new StringBuilder();

        for (PlatformPublisher publisher : publishers) {
            if (requestedPlatforms.contains(publisher.getPlatform())) {
                try {
                    publisher.publish(post);
                    anyPublished = true;
                } catch (Exception ex) {
                    log.warn("{} publishing failed for post {}: {}", publisher.getPlatform(), post.getId(), ex.getMessage());
                    if (!errors.isEmpty()) errors.append("; ");
                    errors.append(publisher.getPlatform()).append(": ").append(ex.getMessage());
                }
            }
        }

        if (!anyPublished) {
            throw new RuntimeException(errors.toString());
        }
    }

    private Set<Platform> parsePlatforms(String platformsStr) {
        if (platformsStr == null || platformsStr.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(platformsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Platform.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown platform: {}", s);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
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
                    new PostNotification(post.getId(), post.getTitle(), "RETRYING", "Retrying... (" + attempt + "/" + post.getMaxRetries() + ")", "warning", Instant.now()));
        } else {
            statusMachine.transition(post, PostStatus.FAILED);
            post.setRetryCount(attempt);
            post.setFailedReason(reason);
            postRepository.save(post);

            notificationService.sendPostUpdate(userId,
                    new PostNotification(post.getId(), post.getTitle(), "FAILED", "Failed - " + reason, "error", Instant.now()));
        }
    }
}
