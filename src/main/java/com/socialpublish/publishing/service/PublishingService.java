package com.socialpublish.publishing.service;

import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import com.socialpublish.publishing.entity.Platform;
import com.socialpublish.publishing.event.PostScheduledEvent;
import com.socialpublish.integrations.exception.IntegrationException;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
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
    private final TelegramSettingsRepository telegramRepository;
    private final DiscordSettingsRepository discordRepository;
    private final SlackSettingsRepository slackRepository;
    private final NotionSettingsRepository notionRepository;
    private final LinkedInSettingsRepository linkedinRepository;
    private final RedditSettingsRepository redditRepository;

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

    private record PlatformTarget(Platform platform, UUID targetId) {}

    private void doPublish(Post post) {
        List<PlatformTarget> targets = parsePlatforms(post.getPlatforms());
        if (targets.isEmpty()) {
            throw new RuntimeException("No platforms selected for publishing");
        }

        boolean anyPublished = false;
        StringBuilder errors = new StringBuilder();

        for (PlatformTarget target : targets) {
            UUID effectiveTargetId = target.targetId();
            
            for (PlatformPublisher publisher : publishers) {
                if (target.platform() == publisher.getPlatform()) {
                    try {
                        if (effectiveTargetId == null) {
                            effectiveTargetId = findDefaultTargetId(post.getOwner().getId(), target.platform());
                            if (effectiveTargetId == null) {
                                throw new IntegrationException(target.platform() + " not configured or no enabled accounts found");
                            }
                        }
                        
                        publisher.publish(post, effectiveTargetId);
                        anyPublished = true;
                    } catch (Exception ex) {
                        log.warn("{} publishing failed for post {}: {}", publisher.getPlatform(), post.getId(), ex.getMessage());
                        if (!errors.isEmpty()) errors.append("; ");
                        errors.append(publisher.getPlatform()).append(": ").append(ex.getMessage());
                    }
                }
            }
        }

        if (!anyPublished) {
            throw new RuntimeException(errors.toString());
        }
    }

    private List<PlatformTarget> parsePlatforms(String platformsStr) {
        if (platformsStr == null || platformsStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(platformsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        if (s.contains(":")) {
                            String[] parts = s.split(":");
                            return new PlatformTarget(Platform.valueOf(parts[0]), UUID.fromString(parts[1]));
                        } else {
                            return new PlatformTarget(Platform.valueOf(s), null);
                        }
                    } catch (Exception e) {
                        log.warn("Unknown platform or invalid target format: {}", s);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private UUID findDefaultTargetId(UUID userId, Platform platform) {
        return switch (platform) {
            case TELEGRAM -> telegramRepository.findAllByUserId(userId).stream()
                    .filter(s -> s.isEnabled()).map(s -> s.getId()).findFirst().orElse(null);
            case DISCORD -> discordRepository.findAllByUserId(userId).stream()
                    .filter(s -> s.isEnabled()).map(s -> s.getId()).findFirst().orElse(null);
            case SLACK -> slackRepository.findAllByUserId(userId).stream()
                    .filter(s -> s.isEnabled()).map(s -> s.getId()).findFirst().orElse(null);
            case NOTION -> notionRepository.findAllByUserId(userId).stream()
                    .filter(s -> s.isEnabled()).map(s -> s.getId()).findFirst().orElse(null);
            case LINKEDIN -> linkedinRepository.findByUserId(userId).map(s -> s.getId()).orElse(null);
            case REDDIT -> redditRepository.findByUserId(userId).map(s -> s.getId()).orElse(null);
        };
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
