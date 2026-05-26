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
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_TITLE_LENGTH = 150;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostStatusMachine statusMachine;
    private final PostSchedulerService postSchedulerService;
    private final PublishingProducer publishingProducer;
    private final NotificationService notificationService;
    private final PostMediaSyncService postMediaSyncService;
    private final PostMapper postMapper;
    private final RecurringPostService recurringPostService;

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
        post.setTitle(buildTitleFromContent(post.getContent()));

        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, List.of());

        PostStatus targetStatus = request.getStatus() == null || request.getStatus().isBlank() ? PostStatus.DRAFT : PostStatus.valueOf(request.getStatus());
        if (targetStatus != PostStatus.DRAFT) {
            applyUserTransition(post, targetStatus, request);
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
        post.setTitle(buildTitleFromContent(post.getContent()));

        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, List.of());

        prepareForImmediatePublish(post, request);

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
        applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, removeMediaPublicIds);

        PostStatus targetStatus = request.getStatus() == null || request.getStatus().isBlank() ? post.getStatus() : PostStatus.valueOf(request.getStatus());
        if (targetStatus != post.getStatus()) {
            applyUserTransition(post, targetStatus, request);
        } else if (targetStatus == PostStatus.SCHEDULED) {
            applyScheduledFields(post, request);
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
        applyCommonFields(post, request);
        postMediaSyncService.syncMedia(post, ownerId, mediaFiles, removeMediaPublicIds);

        prepareForImmediatePublish(post, request);

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
        prepareForImmediatePublish(post, request);

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

        if (oldStatus == PostStatus.SCHEDULED || oldStatus == PostStatus.FAILED || oldStatus == PostStatus.CANCELLED) {
            statusMachine.transition(post, PostStatus.DRAFT);
            applyDraftFields(post);
            Post saved = postRepository.save(post);
            handleSchedulingChange(saved, oldStatus);
            return;
        }

        throw new PostValidationException("Cannot move status " + oldStatus + " to draft");
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#ownerId")
    public void retryFailedNow(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        if (post.getStatus() != PostStatus.FAILED) {
            throw new PostValidationException("Retry is available only for FAILED posts");
        }
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
        duplicate.setTitle(buildTitleFromContent(source.getContent()));
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

        if (oldStatus == PostStatus.SCHEDULED) {
            applyScheduledFields(post, request);
        } else if (oldStatus == PostStatus.DRAFT) {
            applyUserTransition(post, PostStatus.SCHEDULED, request);
        } else if (oldStatus == PostStatus.FAILED || oldStatus == PostStatus.CANCELLED) {
            statusMachine.transition(post, PostStatus.DRAFT);
            applyUserTransition(post, PostStatus.SCHEDULED, request);
        } else {
            throw new PostValidationException("Cannot reschedule post with status " + oldStatus);
        }

        Post saved = postRepository.save(post);
        handleSchedulingChange(saved, oldStatus);
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
        post.setRecurring(request.isRecurring());
        if (request.isRecurring()) {
            List<String> days = request.getRecurringDays();
            post.setRecurringDays(days == null || days.isEmpty() ? null : String.join(",", days));
            post.setRecurringTime(request.getRecurringTime());
            post.setRecurringEndDate(request.getRecurringEndDate() != null
                    ? request.getRecurringEndDate().atZone(ZoneId.systemDefault()).toInstant()
                    : null);
        } else {
            post.setRecurringDays(null);
            post.setRecurringTime(null);
            post.setRecurringEndDate(null);
        }

        post.setSilentMode(request.isSilentMode());
        post.setInlineButtons(request.getInlineButtons());
        post.setPollQuestion(request.getPollQuestion());
        post.setPollOptions(request.getPollOptions());
        post.setPollMultipleAnswers(request.isPollMultipleAnswers());
        post.setPollIsQuiz(request.isPollIsQuiz());
        post.setPollCorrectOptionId(request.getPollCorrectOptionId());
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
        return title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title;
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
        if (post.isRecurring()) {
            String days = post.getRecurringDays();
            String time = post.getRecurringTime();
            if (days == null || days.isBlank() || time == null || time.isBlank()) {
                throw new PostValidationException("Recurring days and time are required");
            }
            Instant nextAt = recurringPostService.calculateFirstOccurrence(days, time);
            if (nextAt == null) {
                throw new PostValidationException("Could not calculate next recurring date");
            }
            post.setScheduledAt(nextAt);
        } else {
            if (request.getScheduledAt() == null) {
                throw new PostValidationException("Scheduled date is required for SCHEDULED posts");
            }
            post.setScheduledAt(request.getScheduledAt().atZone(ZoneId.systemDefault()).toInstant());
        }
        if (post.getPlatforms() == null || post.getPlatforms().isBlank()) {
            throw new PostValidationException("Select at least one platform for SCHEDULED posts");
        }
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

        notificationService.sendPostUpdate(userId, new PostNotification(postId, title, "PUBLISHING", "Publishing...", "info", Instant.now()));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishingProducer.sendPublishRequest(postId, false);
            }
        });
    }
}
