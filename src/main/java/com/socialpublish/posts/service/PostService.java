package com.socialpublish.posts.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.media.dto.MediaUploadResult;
import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.media.service.CloudinaryMediaService;
import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.service.NotificationService;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostNotFoundException;
import com.socialpublish.posts.exception.PostValidationException;
import com.socialpublish.posts.exception.UnauthorizedPostAccessException;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.publishing.service.PublishingProducer;
import com.socialpublish.scheduling.service.PostSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostStatusMachine statusMachine;
    private final PostSchedulerService postSchedulerService;
    private final PublishingProducer publishingProducer;
    private final NotificationService notificationService;
    private final PostMediaSyncService postMediaSyncService;

    @Transactional(readOnly = true)
    public PostView getPostView(UUID ownerId, UUID postId) {
        return PostView.fromWithMedia(requireOwnedPost(ownerId, postId));
    }

    @Transactional(readOnly = true)
    public List<PostView> getQueuePosts(UUID ownerId, PostStatus statusFilter) {
        if (statusFilter != null) {
            return postRepository.findByOwnerIdAndStatusOrderByUpdatedAtDesc(ownerId, statusFilter)
                    .stream().map(PostView::from).toList();
        }
        return postRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId)
                .stream().map(PostView::from).toList();
    }

    @Transactional(readOnly = true)
    public PostUpsertRequest getEditRequest(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        PostUpsertRequest request = new PostUpsertRequest();
        request.setContent(post.getContent());
        request.setStatus(post.getStatus());
        request.setScheduledAt(
                post.getScheduledAt() == null
                        ? null
                        : post.getScheduledAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );
        request.setFailedReason(post.getFailedReason());
        if (post.getPlatforms() != null && !post.getPlatforms().isBlank()) {
            request.setPlatforms(Arrays.asList(post.getPlatforms().split(",")));
        }
        return request;
    }

    @Transactional
    public PostView createPost(UUID ownerId, PostUpsertRequest request) {
        return createPost(ownerId, request, List.of());
    }

    @Transactional
    public PostView createPost(UUID ownerId, PostUpsertRequest request, List<MultipartFile> mediaFiles) {
        User owner = userRepository.findById(ownerId).orElseThrow(UnauthorizedPostAccessException::new);
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(PostStatus.DRAFT);
        applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, List.of());

        PostStatus targetStatus = request.getStatus() == null ? PostStatus.DRAFT : request.getStatus();
        if (targetStatus != PostStatus.DRAFT) {
            applyUserTransition(post, targetStatus, request);
        }

        Post saved = postRepository.save(post);
        handleScheduling(saved);
        return PostView.from(saved);
    }

    @Transactional
    public PostView createPostAndPublishNow(UUID ownerId, PostUpsertRequest request) {
        return createPostAndPublishNow(ownerId, request, List.of());
    }

    @Transactional
    public PostView createPostAndPublishNow(UUID ownerId, PostUpsertRequest request, List<MultipartFile> mediaFiles) {
        User owner = userRepository.findById(ownerId).orElseThrow(UnauthorizedPostAccessException::new);
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(PostStatus.DRAFT);
        applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, List.of());

        prepareForImmediatePublish(post, request);

        Post saved = postRepository.save(post);
        dispatchImmediatePublishing(saved);
        return PostView.from(saved);
    }

    @Transactional
    public PostView updatePost(UUID ownerId, UUID postId, PostUpsertRequest request) {
        return updatePost(ownerId, postId, request, List.of(), List.of());
    }

    @Transactional
    public PostView updatePost(
            UUID ownerId,
            UUID postId,
            PostUpsertRequest request,
            List<MultipartFile> mediaFiles,
            List<String> removeMediaPublicIds
    ) {
        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();
        applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, removeMediaPublicIds);

        PostStatus targetStatus = request.getStatus() == null ? post.getStatus() : request.getStatus();
        if (targetStatus != post.getStatus()) {
            applyUserTransition(post, targetStatus, request);
        } else if (targetStatus == PostStatus.SCHEDULED) {
            applyScheduledFields(post, request);
        }

        Post saved = postRepository.save(post);
        handleSchedulingChange(saved, oldStatus);
        return PostView.from(saved);
    }

    @Transactional
    public PostView updatePostAndPublishNow(UUID ownerId, UUID postId, PostUpsertRequest request) {
        return updatePostAndPublishNow(ownerId, postId, request, List.of(), List.of());
    }

    @Transactional
    public PostView updatePostAndPublishNow(
            UUID ownerId,
            UUID postId,
            PostUpsertRequest request,
            List<MultipartFile> mediaFiles,
            List<String> removeMediaPublicIds
    ) {
        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();
        applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, removeMediaPublicIds);

        prepareForImmediatePublish(post, request);

        Post saved = postRepository.save(post);
        if (oldStatus == PostStatus.SCHEDULED) {
            postSchedulerService.cancelScheduledPost(saved.getId());
        }
        dispatchImmediatePublishing(saved);
        return PostView.from(saved);
    }

    @Transactional
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

    private Post requireOwnedPost(UUID ownerId, UUID postId) {
        return postRepository.findByIdAndOwnerId(postId, ownerId).orElseThrow(PostNotFoundException::new);
    }

    private void applyCommonFields(Post post, PostUpsertRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        post.setTitle(buildTitleFromContent(content));
        post.setContent(content);
        List<String> platforms = request.getPlatforms();
        post.setPlatforms(platforms == null || platforms.isEmpty() ? "" : String.join(",", platforms));
    }

    private String buildTitleFromContent(String content) {
        if (content == null || content.isBlank()) {
            return "Untitled post";
        }

        String firstLine = content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("Untitled post");

        String plain = firstLine
                .replaceAll("\\[(.*?)]\\((.*?)\\)", "$1")
                .replaceAll("[*_~`>|\\[\\]()]", "")
                .trim();

        String title = plain.isBlank() ? "Untitled post" : plain;
        return title.length() > 150 ? title.substring(0, 150) : title;
    }

    private void applyUserTransition(Post post, PostStatus target, PostUpsertRequest request) {
        if (!PostStatus.userSettable().contains(target)) {
            throw new PostValidationException("Cannot manually set status: " + target);
        }
        statusMachine.transition(post, target);
        switch (target) {
            case DRAFT -> applyDraftFields(post);
            case SCHEDULED -> applyScheduledFields(post, request);
            case CANCELLED -> applyCancelledFields(post);
            default -> { }
        }
    }

    private void applyDraftFields(Post post) {
        post.setScheduledAt(null);
        post.setPublishedAt(null);
        post.setFailedReason(null);
        post.setRetryCount(0);
    }

    private void applyScheduledFields(Post post, PostUpsertRequest request) {
        if (request.getScheduledAt() == null) {
            throw new PostValidationException("Scheduled date is required for SCHEDULED posts");
        }
        if (post.getPlatforms() == null || post.getPlatforms().isBlank()) {
            throw new PostValidationException("Select at least one platform for SCHEDULED posts");
        }
        post.setScheduledAt(request.getScheduledAt().atZone(ZoneId.systemDefault()).toInstant());
        post.setPublishedAt(null);
        post.setFailedReason(null);
        post.setRetryCount(0);
    }

    private void applyCancelledFields(Post post) {
        post.setScheduledAt(null);
        post.setPublishedAt(null);
        post.setFailedReason(null);
        post.setRetryCount(0);
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

    private void prepareForImmediatePublish(Post post, PostUpsertRequest request) {
        if (post.getPlatforms() == null || post.getPlatforms().isBlank()) {
            throw new PostValidationException("Select at least one platform for publishing");
        }

        request.setScheduledAt(LocalDateTime.now());

        PostStatus current = post.getStatus();
        if (current == PostStatus.FAILED || current == PostStatus.CANCELLED) {
            statusMachine.transition(post, PostStatus.DRAFT);
        }

        if (post.getStatus() == PostStatus.SCHEDULED) {
            applyScheduledFields(post, request);
        } else if (post.getStatus() == PostStatus.DRAFT) {
            applyUserTransition(post, PostStatus.SCHEDULED, request);
        } else {
            throw new PostValidationException("Cannot publish now from status " + post.getStatus());
        }

        statusMachine.transition(post, PostStatus.PUBLISHING);
        post.setRetryCount(0);
        post.setFailedReason(null);
        post.setPublishedAt(null);
    }

    private void dispatchImmediatePublishing(Post post) {
        UUID postId = post.getId();
        UUID userId = post.getOwner().getId();
        String title = post.getTitle();

        notificationService.sendPostUpdate(userId, PostNotification.publishing(postId, title));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishingProducer.sendPublishRequest(postId);
            }
        });
    }
}
