package com.socialpublish.dashboard.service;

import com.socialpublish.AbstractIntegrationTest;
import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setEmail("dashboard-owner@example.com");
        owner.setFullName("Dashboard User");
        owner.setProvider(AuthProvider.LOCAL);
        owner.setRole(Role.USER);
        owner = userRepository.save(owner);
    }

    @Test
    void shouldBuildDashboardWithCorrectStatsAndActivity() {
        createPost(PostStatus.PUBLISHED, null);
        createPost(PostStatus.PUBLISHED, null);
        createPost(PostStatus.DRAFT, null);
        createPost(PostStatus.FAILED, null);
        createPost(PostStatus.SCHEDULED, Instant.now().plus(1, ChronoUnit.HOURS));

        DashboardView dashboard = dashboardService.buildDashboard(owner.getId());

        assertThat(dashboard).isNotNull();
        
        assertThat(dashboard.stats()).isNotNull();
        assertThat(dashboard.stats().totalPosts()).isEqualTo(5);
        assertThat(dashboard.stats().publishedPosts()).isEqualTo(2);
        assertThat(dashboard.stats().draftPosts()).isEqualTo(1);
        assertThat(dashboard.stats().failedPosts()).isEqualTo(1);
        assertThat(dashboard.stats().scheduledPosts()).isEqualTo(1);

        assertThat(dashboard.statusSlices()).hasSize(4);
        
        assertThat(dashboard.nextPublish()).isNotNull();
        assertThat(dashboard.nextPublish().excerpt()).isEqualTo("Post scheduled for future");

        assertThat(dashboard.recentPosts()).hasSize(5);
    }

    private void createPost(PostStatus status, Instant scheduledAt) {
        Post post = new Post();
        post.setOwner(owner);
        post.setStatus(status);
        post.setContent(status == PostStatus.SCHEDULED ? "Post scheduled for future" : "Sample content " + status);
        post.setTitle("Title " + status);
        post.setPlatforms("TELEGRAM");
        post.setScheduledAt(scheduledAt);
        postRepository.save(post);
    }
}
