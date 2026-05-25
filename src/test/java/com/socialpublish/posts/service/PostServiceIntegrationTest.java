package com.socialpublish.posts.service;

import com.socialpublish.AbstractIntegrationTest;
import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.publishing.service.PublishingProducer;
import com.socialpublish.scheduling.service.PostSchedulerService;
import com.socialpublish.notifications.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class PostServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private PublishingProducer publishingProducer;

    @MockitoBean
    private PostSchedulerService postSchedulerService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private PostMediaSyncService postMediaSyncService;

    private User owner;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setEmail("owner@example.com");
        owner.setFullName("Post Owner");
        owner.setProvider(AuthProvider.LOCAL);
        owner.setRole(Role.USER);
        owner = userRepository.save(owner);
    }

    @Test
    void shouldCreateDraftPostSuccessfully() {
        PostUpsertRequest request = new PostUpsertRequest();
        request.setContent("This is a draft post content.\nSecond line.");
        request.setPlatforms(List.of("TELEGRAM", "DISCORD"));
        request.setStatus("DRAFT");

        PostView view = postService.createPost(owner.getId(), request);

        assertThat(view).isNotNull();
        assertThat(view.id()).isNotNull();
        assertThat(view.content()).isEqualTo("This is a draft post content.\nSecond line.");
        assertThat(view.title()).isEqualTo("This is a draft post content.");
        assertThat(view.status()).isEqualTo("DRAFT");

        Optional<Post> dbPost = postRepository.findById(view.id());
        assertThat(dbPost).isPresent();
        assertThat(dbPost.get().getPlatforms()).isEqualTo("TELEGRAM,DISCORD");
    }

    @Test
    void shouldCreateAndPublishPostNowSuccessfully() {
        PostUpsertRequest request = new PostUpsertRequest();
        request.setContent("Publish this immediately!");
        request.setPlatforms(List.of("TELEGRAM"));

        PostView view = postService.createPostAndPublishNow(owner.getId(), request);

        assertThat(view.status()).isEqualTo("PUBLISHING");
        
        Optional<Post> dbPost = postRepository.findById(view.id());
        assertThat(dbPost).isPresent();
        assertThat(dbPost.get().getStatus()).isEqualTo(PostStatus.PUBLISHING);

        verify(publishingProducer).sendPublishRequest(dbPost.get().getId(), false);
    }

    @Test
    void shouldUpdatePostSuccessfully() {
        Post post = new Post();
        post.setOwner(owner);
        post.setContent("Old Content");
        post.setTitle("Old Title");
        post.setStatus(PostStatus.DRAFT);
        post.setPlatforms("DISCORD");
        post = postRepository.save(post);

        PostUpsertRequest updateRequest = new PostUpsertRequest();
        updateRequest.setContent("Newly updated content!");
        updateRequest.setPlatforms(List.of("TELEGRAM", "SLACK"));
        updateRequest.setStatus("DRAFT");

        PostView updatedView = postService.updatePost(owner.getId(), post.getId(), updateRequest);

        assertThat(updatedView.content()).isEqualTo("Newly updated content!");
        
        Post dbPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(dbPost.getPlatforms()).isEqualTo("TELEGRAM,SLACK");
    }

    @Test
    void shouldMovePostToDraftSuccessfully() {
        Post post = new Post();
        post.setOwner(owner);
        post.setContent("Scheduled content");
        post.setTitle("Scheduled Title");
        post.setStatus(PostStatus.SCHEDULED);
        post.setPlatforms("DISCORD");
        post = postRepository.save(post);

        postService.moveToDraft(owner.getId(), post.getId());

        Post dbPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(dbPost.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(dbPost.getScheduledAt()).isNull();

        verify(postSchedulerService).cancelScheduledPost(post.getId());
    }

    @Test
    void shouldDuplicatePostSuccessfully() {
        Post post = new Post();
        post.setOwner(owner);
        post.setContent("Original post content to copy.");
        post.setTitle("Original Title");
        post.setStatus(PostStatus.PUBLISHED);
        post.setPlatforms("TELEGRAM");
        post = postRepository.save(post);

        PostView duplicate = postService.duplicatePost(owner.getId(), post.getId());

        assertThat(duplicate.id()).isNotEqualTo(post.getId());
        assertThat(duplicate.content()).isEqualTo("Original post content to copy.");
        assertThat(duplicate.status()).isEqualTo("DRAFT");

        List<Post> allPosts = postRepository.findByOwnerIdOrderByUpdatedAtDesc(owner.getId());
        assertThat(allPosts).hasSize(2);
    }
}
