package com.socialpublish.publishing.service;

import com.socialpublish.AbstractIntegrationTest;
import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.publishing.entity.Platform;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.mail.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PublishingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PublishingService publishingService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private PublishingProducer publishingProducer;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean(name = "telegramPublisherService")
    private PlatformPublisher platformPublisher;

    private User owner;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        when(platformPublisher.getPlatform()).thenReturn(Platform.TELEGRAM);

        owner = new User();
        owner.setEmail("publisher-owner@example.com");
        owner.setFullName("Publisher Owner");
        owner.setProvider(AuthProvider.LOCAL);
        owner.setRole(Role.USER);
        owner.setEmailNotificationsEnabled(true);
        owner = userRepository.save(owner);
    }

    @Test
    void shouldStartScheduledPublishAndTransitionToPublishing() {
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(PostStatus.SCHEDULED);
        post.setContent("Scheduled publish content");
        post.setTitle("Scheduled title");
        post.setPlatforms("TELEGRAM:123e4567-e89b-12d3-a456-426614174000");
        post = postRepository.save(post);

        publishingService.startScheduledPublish(post.getId());

        Post dbPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(dbPost.getStatus()).isEqualTo(PostStatus.PUBLISHING);
        assertThat(dbPost.getRetryCount()).isZero();

        verify(notificationService).sendPostUpdate(eq(owner.getId()), any());
    }

    @Test
    void shouldAttemptPublishSuccessfullyAndTransitionToPublished() {
        UUID targetId = UUID.randomUUID();
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(PostStatus.PUBLISHING);
        post.setContent("Content to publish");
        post.setTitle("Publish Title");
        post.setPlatforms("TELEGRAM:" + targetId);
        post = postRepository.save(post);

        publishingService.attemptPublish(post.getId(), 0, true);
        Post dbPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(dbPost.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(dbPost.getPublishedAt()).isNotNull();
        assertThat(dbPost.getFailedReason()).isNull();

        verify(platformPublisher).publish(any(), eq(targetId));

        verify(emailService).sendHtmlMessage(eq("publisher-owner@example.com"), anyString(), eq("post-result"), any());
    }

    @Test
    void shouldAttemptPublishFailedAndTransitionToRetryingWhenUnderMaxRetries() {
        UUID targetId = UUID.randomUUID();
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(PostStatus.PUBLISHING);
        post.setContent("Content to publish");
        post.setTitle("Publish Title");
        post.setPlatforms("TELEGRAM:" + targetId);
        post.setMaxRetries(3);
        post = postRepository.save(post);

        doThrow(new RuntimeException("API Call timeout")).when(platformPublisher).publish(any(), any());

        publishingService.attemptPublish(post.getId(), 0, true);

        Post dbPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(dbPost.getStatus()).isEqualTo(PostStatus.RETRYING);
        assertThat(dbPost.getFailedReason()).contains("API Call timeout");

        verify(publishingProducer).sendRetryRequest(post.getId(), 1, true);
    }

    @Test
    void shouldAttemptPublishFailedAndTransitionToFailedWhenMaxRetriesExceeded() {
        UUID targetId = UUID.randomUUID();
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(PostStatus.PUBLISHING);
        post.setContent("Content to publish");
        post.setTitle("Publish Title");
        post.setPlatforms("TELEGRAM:" + targetId);
        post.setMaxRetries(3);
        post = postRepository.save(post);

        doThrow(new RuntimeException("Bad Request")).when(platformPublisher).publish(any(), any());

        publishingService.attemptPublish(post.getId(), 3, true);

        Post dbPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(dbPost.getStatus()).isEqualTo(PostStatus.FAILED);
        assertThat(dbPost.getFailedReason()).contains("Bad Request");

        verify(emailService).sendHtmlMessage(eq("publisher-owner@example.com"), contains("Publication error"), eq("post-result"), any());
    }
}
