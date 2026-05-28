package com.socialpublish.publishing.service;

import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import com.socialpublish.publishing.config.PublishingProperties;
import com.socialpublish.publishing.entity.Platform;
import com.socialpublish.publishing.event.PostScheduledEvent;
import com.socialpublish.integrations.exception.IntegrationException;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import com.socialpublish.posts.service.RecurringPostService;
import com.socialpublish.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
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
    private final RecurringPostService recurringPostService;
    private final EmailService emailService;
    private final CacheManager cacheManager;
    private final PublishingProperties publishingProperties;
    private final PublishingTransactionHelper transactionHelper;

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

        eventPublisher.publishEvent(new PostScheduledEvent(postId, true));
        log.info("Post {} transitioned to PUBLISHING and event published (scheduled=true)", postId);
    }

    @Transactional
    public void handleMissedPost(UUID postId) {
        Post post = postRepository.findWithMediaAndOwnerById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found, skipping missed post handler", postId);
            return;
        }
        Hibernate.initialize(post.getOwner());
        Hibernate.initialize(post.getMedia());

        UUID userId = post.getOwner().getId();
        Duration delay = Duration.between(post.getScheduledAt(), Instant.now());
        long delayMinutes = delay.toMinutes();
        long delayHours = delay.toHours();
        long remainingMinutes = delayMinutes % 60;

        int silentThreshold = publishingProperties.getGracePeriodSilentMinutes();
        int maxThreshold = publishingProperties.getGracePeriodMaxMinutes();

        if (delayMinutes <= silentThreshold) {
            log.info("Post {} is {}min late (within silent grace period), auto-publishing", postId, delayMinutes);
            startScheduledPublish(postId);

        } else if (delayMinutes <= maxThreshold) {
            log.info("Post {} is {}min late (within warning grace period), auto-publishing with notification", postId, delayMinutes);
            startScheduledPublish(postId);

            String delayText = delayHours > 0
                    ? delayHours + "h " + remainingMinutes + "min"
                    : delayMinutes + "min";

            notificationService.sendPostUpdate(userId,
                    new PostNotification(postId, post.getTitle(), "PUBLISHING",
                            "Published with " + delayText + " delay (server recovery)",
                            "warning", Instant.now()));

        } else {
            String overdueText = delayHours + "h " + remainingMinutes + "min";
            log.warn("Post {} is {} overdue (exceeds max grace period), marking as FAILED", postId, overdueText);

            statusMachine.transition(post, PostStatus.FAILED);
            post.setFailedReason("Missed schedule window (" + overdueText + " overdue)");
            postRepository.save(post);

            notificationService.sendPostUpdate(userId,
                    new PostNotification(postId, post.getTitle(), "FAILED",
                            "Missed schedule window (" + overdueText + " overdue). Reschedule or publish manually.",
                            "error", Instant.now()));

            sendResultEmail(post, false, "Missed schedule window (" + overdueText + " overdue)");
            evictDashboardCache(userId);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostScheduled(PostScheduledEvent event) {
        publishingProducer.sendPublishRequest(event.postId(), event.scheduled());
        log.info("Post {} sent to RabbitMQ after commit (scheduled={})", event.postId(), event.scheduled());
    }

    public void attemptPublish(UUID postId, int attempt, boolean scheduled) {
        Post post = transactionHelper.preparePublishing(postId);
        if (post == null) {
            return;
        }

        UUID userId = post.getOwner().getId();

        notificationService.sendPostUpdate(userId,
                new PostNotification(postId, post.getTitle(), "PUBLISHING", "Publishing...", "info", Instant.now()));

        try {
            doPublish(post);
            Post updatedPost = transactionHelper.markPublished(postId);
            if (updatedPost != null) {
                notificationService.sendPostUpdate(userId,
                        new PostNotification(postId, updatedPost.getTitle(), "PUBLISHED", "Published", "success", Instant.now()));
                if (scheduled) {
                    sendResultEmail(updatedPost, true, null);
                }
            }
        } catch (Exception ex) {
            log.error("Publishing failed for post {}: {}", postId, ex.getMessage());
            PublishingTransactionHelper.FailureResult result = transactionHelper.handleFailure(postId, ex.getMessage(), attempt);
            if (result != null) {
                Post updatedPost = result.post();
                if (result.isRetry()) {
                    publishingProducer.sendRetryRequest(postId, attempt + 1, scheduled);
                    notificationService.sendPostUpdate(userId,
                            new PostNotification(postId, updatedPost.getTitle(), "RETRYING",
                                    "Retrying... (" + attempt + "/" + updatedPost.getMaxRetries() + ")", "warning", Instant.now()));
                } else {
                    notificationService.sendPostUpdate(userId,
                            new PostNotification(postId, updatedPost.getTitle(), "FAILED", "Failed - " + ex.getMessage(), "error", Instant.now()));
                    if (scheduled) {
                        sendResultEmail(updatedPost, false, ex.getMessage());
                    }
                }
            }
        }

        evictDashboardCache(userId);
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
                .filter(Objects::nonNull)
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



    private void sendResultEmail(Post post, boolean success, String errorReason) {
        String email = post.getOwner().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Cannot send result email for post {}: User {} has no email", post.getId(), post.getOwner().getId());
            return;
        }

        if (!post.getOwner().isEmailNotificationsEnabled()) {
            log.info("Email notifications are disabled for user {}", post.getOwner().getId());
            return;
        }

        String subject = success ? "✅ Post published: " + post.getTitle() : "❌ Publication error: " + post.getTitle();
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("postTitle", post.getTitle());
        variables.put("success", success);
        variables.put("scheduledTime", post.getScheduledAt());
        
        List<String> platformNames = new ArrayList<>();
        if (post.getPlatforms() != null && !post.getPlatforms().isBlank()) {
            for (String p : post.getPlatforms().split(",")) {
                String name = p.split(":")[0];
                platformNames.add(name);
            }
        }
        variables.put("platforms", platformNames);
        variables.put("errorReason", errorReason);

        emailService.sendHtmlMessage(email, subject, "post-result", variables);
    }

    private void evictDashboardCache(UUID userId) {
        if (userId != null) {
            try {
                Cache cache = cacheManager.getCache("dashboard");
                if (cache != null) {
                    cache.evict(userId);
                    log.debug("Evicted dashboard cache for user {}", userId);
                }
            } catch (Exception ex) {
                log.error("Failed to evict dashboard cache for user {}: {}", userId, ex.getMessage());
            }
        }
    }
}
