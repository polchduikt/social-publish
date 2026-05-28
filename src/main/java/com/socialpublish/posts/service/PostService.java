package com.socialpublish.posts.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostNotFoundException;
import com.socialpublish.posts.exception.PostValidationException;
import com.socialpublish.posts.exception.UnauthorizedPostAccessException;
import com.socialpublish.posts.mapper.PostMapper;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.publishing.service.PublishingProducer;
import com.socialpublish.scheduling.service.PostSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheEvict;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostSchedulerService postSchedulerService;
    private final PublishingProducer publishingProducer;
    private final NotificationService notificationService;
    private final PostMediaSyncService postMediaSyncService;
    private final PostMapper postMapper;
    private final PostLifecyclePolicy postLifecyclePolicy;
    private final PostTitleGenerator postTitleGenerator;

    @Transactional(readOnly = true)
    public PostView getPostView(UUID ownerId, UUID postId) {
        return postMapper.toView(requireOwnedPost(ownerId, postId));
    }

    @Transactional(readOnly = true)
    public List<PostView> getQueuePosts(UUID ownerId, PostStatus statusFilter) {
        if (statusFilter != null) {
            return postRepository.findByOwnerIdAndStatusOrderByUpdatedAtDesc(ownerId, statusFilter)
                    .stream().map(postMapper::toView).toList();
        }
        return postRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId)
                .stream().map(postMapper::toView).toList();
    }

    @Transactional(readOnly = true)
    public PostUpsertRequest getEditRequest(UUID ownerId, UUID postId) {
        return postMapper.toUpsertRequest(requireOwnedPost(ownerId, postId));
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView createPost(UUID ownerId, PostUpsertRequest request) {
        return createPost(ownerId, request, List.of());
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView createPost(UUID ownerId, PostUpsertRequest request, List<MultipartFile> mediaFiles) {
        User owner = userRepository.findById(ownerId).orElseThrow(UnauthorizedPostAccessException::new);
        Post post = postMapper.toEntity(request);
        post.setOwner(owner);
        post.setStatus(PostStatus.DRAFT);
        post.setTitle(postTitleGenerator.fromContent(post.getContent()));

        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, List.of());

        PostStatus targetStatus = postLifecyclePolicy.resolveRequestedStatus(request, PostStatus.DRAFT);
        if (targetStatus != PostStatus.DRAFT) {
            postLifecyclePolicy.applyUserTransition(post, targetStatus, request);
        }

        Post saved = postRepository.save(post);
        handleScheduling(saved);
        return postMapper.toView(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView createPostAndPublishNow(UUID ownerId, PostUpsertRequest request) {
        return createPostAndPublishNow(ownerId, request, List.of());
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView createPostAndPublishNow(UUID ownerId, PostUpsertRequest request, List<MultipartFile> mediaFiles) {
        User owner = userRepository.findById(ownerId).orElseThrow(UnauthorizedPostAccessException::new);
        Post post = postMapper.toEntity(request);
        post.setOwner(owner);
        post.setStatus(PostStatus.DRAFT);
        post.setTitle(postTitleGenerator.fromContent(post.getContent()));

        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, List.of());

        postLifecyclePolicy.prepareForImmediatePublish(post, request);

        Post saved = postRepository.save(post);
        dispatchImmediatePublishing(saved);
        return postMapper.toView(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView updatePost(UUID ownerId, UUID postId, PostUpsertRequest request) {
        return updatePost(ownerId, postId, request, List.of(), List.of());
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView updatePost(
            UUID ownerId,
            UUID postId,
            PostUpsertRequest request,
            List<MultipartFile> mediaFiles,
            List<String> removeMediaPublicIds
    ) {
        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();
        postLifecyclePolicy.applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, removeMediaPublicIds);

        PostStatus targetStatus = postLifecyclePolicy.resolveRequestedStatus(request, post.getStatus());
        if (targetStatus != post.getStatus()) {
            postLifecyclePolicy.applyUserTransition(post, targetStatus, request);
        } else if (targetStatus == PostStatus.SCHEDULED) {
            postLifecyclePolicy.applyScheduledFields(post, request);
        }

        Post saved = postRepository.save(post);
        handleSchedulingChange(saved, oldStatus);
        return postMapper.toView(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView updatePostAndPublishNow(UUID ownerId, UUID postId, PostUpsertRequest request) {
        return updatePostAndPublishNow(ownerId, postId, request, List.of(), List.of());
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView updatePostAndPublishNow(
            UUID ownerId,
            UUID postId,
            PostUpsertRequest request,
            List<MultipartFile> mediaFiles,
            List<String> removeMediaPublicIds
    ) {
        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();
        postLifecyclePolicy.applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, removeMediaPublicIds);

        postLifecyclePolicy.prepareForImmediatePublish(post, request);

        Post saved = postRepository.save(post);
        if (oldStatus == PostStatus.SCHEDULED) {
            postSchedulerService.cancelScheduledPost(saved.getId());
        }
        dispatchImmediatePublishing(saved);
        return postMapper.toView(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public void deletePost(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        if (post.getStatus() == PostStatus.SCHEDULED) {
            postSchedulerService.cancelScheduledPost(postId);
        }
        postMediaSyncService.deleteByPublicIds(
                post.getMedia().stream().map(PostMedia::getPublicId).toList()
        );
        postRepository.delete(post);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public void publishNow(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();

        PostUpsertRequest request = new PostUpsertRequest();
        request.setScheduledAt(LocalDateTime.now());
        postLifecyclePolicy.prepareForImmediatePublish(post, request);

        Post saved = postRepository.save(post);
        if (oldStatus == PostStatus.SCHEDULED) {
            postSchedulerService.cancelScheduledPost(saved.getId());
        }
        dispatchImmediatePublishing(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public void moveToDraft(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();

        if (oldStatus == PostStatus.DRAFT) {
            return;
        }

        postLifecyclePolicy.moveToDraft(post);
        Post saved = postRepository.save(post);
        handleSchedulingChange(saved, oldStatus);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public void retryFailedNow(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        postLifecyclePolicy.requireRetryable(post);
        publishNow(ownerId, postId);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public PostView duplicatePost(UUID ownerId, UUID postId) {
        Post source = requireOwnedPost(ownerId, postId);
        User owner = source.getOwner();

        Post duplicate = new Post();
        duplicate.setOwner(owner);
        duplicate.setStatus(PostStatus.DRAFT);
        duplicate.setContent(source.getContent());
        duplicate.setTitle(postTitleGenerator.fromContent(source.getContent()));
        duplicate.setPlatforms(source.getPlatforms());
        duplicate.setFailedReason(null);
        duplicate.setScheduledAt(null);
        duplicate.setPublishedAt(null);
        duplicate.setRetryCount(0);

        postMediaSyncService.copyMedia(source, duplicate, ownerId);

        Post saved = postRepository.save(duplicate);
        return postMapper.toView(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public void reschedulePost(UUID ownerId, UUID postId, LocalDateTime scheduledAt) {
        if (scheduledAt == null) {
            throw new PostValidationException("Scheduled date is required");
        }

        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();

        PostUpsertRequest request = new PostUpsertRequest();
        request.setScheduledAt(scheduledAt);

        postLifecyclePolicy.reschedule(post, request);

        Post saved = postRepository.save(post);
        handleSchedulingChange(saved, oldStatus);
    }

    private Post requireOwnedPost(UUID ownerId, UUID postId) {
        return postRepository.findByIdAndOwnerId(postId, ownerId).orElseThrow(PostNotFoundException::new);
    }

    private void handleScheduling(Post post) {
        if (post.getStatus() == PostStatus.SCHEDULED) {
            postSchedulerService.schedulePost(post);
        }
    }

    private void handleSchedulingChange(Post post, PostStatus oldStatus) {
        if (oldStatus == PostStatus.SCHEDULED && post.getStatus() != PostStatus.SCHEDULED) {
            postSchedulerService.cancelScheduledPost(post.getId());
        }
        if (post.getStatus() == PostStatus.SCHEDULED) {
            postSchedulerService.schedulePost(post);
        }
    }

    private void dispatchImmediatePublishing(Post post) {
        UUID postId = post.getId();
        UUID userId = post.getOwner().getId();
        String title = post.getTitle();
        notificationService.sendPostUpdate(userId, new PostNotification(postId, title, "PUBLISHING", "Publishing...", "info", Instant.now()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishingProducer.sendPublishRequest(postId, false);
            }
        });
    }
}
