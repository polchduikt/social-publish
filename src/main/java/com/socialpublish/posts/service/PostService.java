package com.socialpublish.posts.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostNotFoundException;
import com.socialpublish.posts.exception.PostValidationException;
import com.socialpublish.posts.exception.UnauthorizedPostAccessException;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.scheduling.service.PostSchedulerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostStatusMachine statusMachine;
    private final PostSchedulerService postSchedulerService;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            PostStatusMachine statusMachine,
            PostSchedulerService postSchedulerService
    ) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.statusMachine = statusMachine;
        this.postSchedulerService = postSchedulerService;
    }

    @Transactional(readOnly = true)
    public PostView getPostView(UUID ownerId, UUID postId) {
        return PostView.from(requireOwnedPost(ownerId, postId));
    }

    @Transactional(readOnly = true)
    public PostUpsertRequest getEditRequest(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        PostUpsertRequest request = new PostUpsertRequest();
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());
        request.setStatus(post.getStatus());
        request.setScheduledAt(
                post.getScheduledAt() == null
                        ? null
                        : post.getScheduledAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );
        request.setFailedReason(post.getFailedReason());
        return request;
    }

    @Transactional
    public PostView createPost(UUID ownerId, PostUpsertRequest request) {
        User owner = userRepository.findById(ownerId).orElseThrow(UnauthorizedPostAccessException::new);
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(PostStatus.DRAFT);
        applyCommonFields(post, request);

        PostStatus targetStatus = request.getStatus() == null ? PostStatus.DRAFT : request.getStatus();
        if (targetStatus != PostStatus.DRAFT) {
            applyUserTransition(post, targetStatus, request);
        }

        Post saved = postRepository.save(post);
        handleScheduling(saved);
        return PostView.from(saved);
    }

    @Transactional
    public PostView updatePost(UUID ownerId, UUID postId, PostUpsertRequest request) {
        Post post = requireOwnedPost(ownerId, postId);
        PostStatus oldStatus = post.getStatus();
        applyCommonFields(post, request);

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
    public void deletePost(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
        if (post.getStatus() == PostStatus.SCHEDULED) {
            postSchedulerService.cancelScheduledPost(postId);
        }
        postRepository.delete(post);
    }

    private Post requireOwnedPost(UUID ownerId, UUID postId) {
        return postRepository.findByIdAndOwnerId(postId, ownerId).orElseThrow(PostNotFoundException::new);
    }

    private void applyCommonFields(Post post, PostUpsertRequest request) {
        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        String content = request.getContent() == null ? "" : request.getContent().trim();
        post.setTitle(title);
        post.setContent(content);
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
}
