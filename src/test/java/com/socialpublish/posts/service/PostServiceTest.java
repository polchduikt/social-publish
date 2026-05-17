package com.socialpublish.posts.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostNotFoundException;
import com.socialpublish.posts.exception.PostValidationException;
import com.socialpublish.posts.mapper.PostMapper;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.publishing.service.PublishingProducer;
import com.socialpublish.scheduling.service.PostSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Spy
    private PostStatusMachine statusMachine;
    @Mock private PostSchedulerService postSchedulerService;
    @Mock private PublishingProducer publishingProducer;
    @Mock private NotificationService notificationService;
    @Mock private PostMediaSyncService postMediaSyncService;
    @Mock private PostMapper postMapper;
    @Mock private RecurringPostService recurringPostService;

    @InjectMocks
    private PostService postService;

    private UUID ownerId;
    private UUID postId;
    private Post post;
    private User owner;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        postId = UUID.randomUUID();
        
        owner = new User();
        owner.setId(ownerId);
        
        post = new Post();
        post.setId(postId);
        post.setOwner(owner);
    }

    @Test
    void deletePost_Draft_DeletesPostAndMedia() {
        post.setStatus(PostStatus.DRAFT);
        when(postRepository.findByIdAndOwnerId(postId, ownerId)).thenReturn(Optional.of(post));

        postService.deletePost(ownerId, postId);

        verify(postSchedulerService, never()).cancelScheduledPost(any());
        verify(postMediaSyncService).deleteByPublicIds(anyList());
        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_Scheduled_CancelsScheduleAndDeletes() {
        post.setStatus(PostStatus.SCHEDULED);
        when(postRepository.findByIdAndOwnerId(postId, ownerId)).thenReturn(Optional.of(post));

        postService.deletePost(ownerId, postId);

        verify(postSchedulerService).cancelScheduledPost(postId);
        verify(postMediaSyncService).deleteByPublicIds(anyList());
        verify(postRepository).delete(post);
    }

    @Test
    void moveToDraft_FromScheduled_UpdatesStatusAndCancelsSchedule() {
        post.setStatus(PostStatus.SCHEDULED);
        when(postRepository.findByIdAndOwnerId(postId, ownerId)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        postService.moveToDraft(ownerId, postId);

        verify(statusMachine).transition(post, PostStatus.DRAFT);
        verify(postSchedulerService).cancelScheduledPost(postId);
        assertNull(post.getScheduledAt());
    }

    @Test
    void moveToDraft_FromDraft_DoesNothing() {
        post.setStatus(PostStatus.DRAFT);
        when(postRepository.findByIdAndOwnerId(postId, ownerId)).thenReturn(Optional.of(post));

        postService.moveToDraft(ownerId, postId);

        verify(statusMachine, never()).transition(any(), any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void moveToDraft_FromPublishing_ThrowsValidationException() {
        post.setStatus(PostStatus.PUBLISHING);
        when(postRepository.findByIdAndOwnerId(postId, ownerId)).thenReturn(Optional.of(post));

        assertThrows(PostValidationException.class, () -> postService.moveToDraft(ownerId, postId));
    }
    
    @Test
    void getPostView_PostNotFound_ThrowsException() {
        when(postRepository.findByIdAndOwnerId(postId, ownerId)).thenReturn(Optional.empty());
        
        assertThrows(PostNotFoundException.class, () -> postService.getPostView(ownerId, postId));
    }
}
