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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
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
        applyCommonFields(post, request);
        applyStatus(post, request);
        return PostView.from(postRepository.save(post));
    }

    @Transactional
    public PostView updatePost(UUID ownerId, UUID postId, PostUpsertRequest request) {
        Post post = requireOwnedPost(ownerId, postId);
        applyCommonFields(post, request);
        applyStatus(post, request);
        return PostView.from(postRepository.save(post));
    }

    @Transactional
    public void deletePost(UUID ownerId, UUID postId) {
        Post post = requireOwnedPost(ownerId, postId);
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

    private void applyStatus(Post post, PostUpsertRequest request) {
        PostStatus status = request.getStatus() == null ? PostStatus.DRAFT : request.getStatus();
        post.setStatus(status);

        switch (status) {
            case DRAFT -> updateDraft(post);
            case SCHEDULED -> schedule(post, request);
            case PUBLISHED -> publish(post);
            case FAILED -> fail(post, request);
        }
    }

    private void updateDraft(Post post) {
        post.setScheduledAt(null);
        post.setPublishedAt(null);
        post.setFailedReason(null);
    }

    private void schedule(Post post, PostUpsertRequest request) {
        if (request.getScheduledAt() == null) {
            throw new PostValidationException("Scheduled date is required for SCHEDULED posts");
        }
        post.setScheduledAt(request.getScheduledAt().atZone(ZoneId.systemDefault()).toInstant());
        post.setPublishedAt(null);
        post.setFailedReason(null);
    }

    private void publish(Post post) {
        post.setScheduledAt(null);
        post.setFailedReason(null);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }
    }

    private void fail(Post post, PostUpsertRequest request) {
        post.setScheduledAt(null);
        post.setPublishedAt(null);
        String reason = request.getFailedReason();
        post.setFailedReason(reason == null || reason.isBlank() ? "Publishing failed" : reason.trim());
    }
}
