package com.socialpublish.publishing.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.mail.service.EmailService;
import com.socialpublish.publishing.config.PublishingProperties;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import com.socialpublish.posts.service.RecurringPostService;
import com.socialpublish.publishing.entity.Platform;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishingServiceTest {

    @Mock private PostRepository postRepository;
    @Spy
    private PostStatusMachine statusMachine;
    @Mock private PublishingProducer publishingProducer;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TelegramSettingsRepository telegramRepository;
    @Mock private DiscordSettingsRepository discordRepository;
    @Mock private SlackSettingsRepository slackRepository;
    @Mock private NotionSettingsRepository notionRepository;
    @Mock private LinkedInSettingsRepository linkedinRepository;
    @Mock private RedditSettingsRepository redditRepository;
    @Mock private RecurringPostService recurringPostService;
    @Mock private EmailService emailService;
    @Mock private CacheManager cacheManager;
    @Mock private PublishingProperties publishingProperties;
    @Mock private PlatformPublisher telegramPublisher;
    @Mock private PublishingTransactionHelper transactionHelper;

    private PublishingService publishingService;

    @BeforeEach
    void setUp() {
        publishingService = new PublishingService(
                postRepository, statusMachine, publishingProducer, notificationService,
                eventPublisher, List.of(telegramPublisher), telegramRepository, discordRepository,
                slackRepository, notionRepository, linkedinRepository, redditRepository,
                recurringPostService, emailService, cacheManager, publishingProperties,
                transactionHelper
        );
    }

    @Test
    void startScheduledPublish_WithValidPost_TransitionsToPublishingAndSendsEvent() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.SCHEDULED);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        post.setOwner(owner);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        publishingService.startScheduledPublish(postId);

        verify(statusMachine).transition(post, PostStatus.PUBLISHING);
        assertEquals(0, post.getRetryCount());
        verify(postRepository).save(post);
        verify(eventPublisher).publishEvent(any(com.socialpublish.publishing.event.PostScheduledEvent.class));
        verify(notificationService).sendPostUpdate(eq(owner.getId()), any());
    }

    @Test
    void startScheduledPublish_WithNotScheduledPost_DoesNothing() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.DRAFT);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        publishingService.startScheduledPublish(postId);

        verify(statusMachine, never()).transition(any(), any());
        verify(postRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void attemptPublish_Success_TransitionsToPublished() {
        when(telegramPublisher.getPlatform()).thenReturn(Platform.TELEGRAM);
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.PUBLISHING);
        post.setPlatforms("TELEGRAM:123e4567-e89b-12d3-a456-426614174000");
        User owner = new User();
        owner.setId(UUID.randomUUID());
        post.setOwner(owner);

        when(transactionHelper.preparePublishing(postId)).thenReturn(post);
        when(transactionHelper.markPublished(postId)).thenReturn(post);

        publishingService.attemptPublish(postId, 0, false);

        verify(telegramPublisher).publish(eq(post), eq(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")));
        verify(transactionHelper).markPublished(postId);
        verify(notificationService).sendPostUpdate(eq(owner.getId()), argThat(n -> "PUBLISHED".equals(n.status())));
    }

    @Test
    void attemptPublish_Failure_RetriesIfUnderMaxRetries() {
        when(telegramPublisher.getPlatform()).thenReturn(Platform.TELEGRAM);
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.PUBLISHING);
        post.setPlatforms("TELEGRAM:123e4567-e89b-12d3-a456-426614174000");
        post.setMaxRetries(3);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        post.setOwner(owner);

        when(transactionHelper.preparePublishing(postId)).thenReturn(post);
        when(transactionHelper.handleFailure(postId, "API Error", 0)).thenReturn(
                new PublishingTransactionHelper.FailureResult(post, true)
        );
        doThrow(new RuntimeException("API Error")).when(telegramPublisher).publish(any(), any());

        publishingService.attemptPublish(postId, 0, false);

        verify(transactionHelper).handleFailure(postId, "API Error", 0);
        verify(publishingProducer).sendRetryRequest(postId, 1, false);
        verify(notificationService).sendPostUpdate(eq(owner.getId()), argThat(n -> "RETRYING".equals(n.status())));
    }
}
