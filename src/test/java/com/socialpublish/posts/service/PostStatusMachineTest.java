package com.socialpublish.posts.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostStatusMachineTest {

    private PostStatusMachine statusMachine;
    private Post post;

    @BeforeEach
    void setUp() {
        statusMachine = new PostStatusMachine();
        post = new Post();
    }

    @Test
    void transition_DraftToScheduled_Success() {
        post.setStatus(PostStatus.DRAFT);
        assertDoesNotThrow(() -> statusMachine.transition(post, PostStatus.SCHEDULED));
        assertEquals(PostStatus.SCHEDULED, post.getStatus());
    }

    @Test
    void transition_DraftToCancelled_Success() {
        post.setStatus(PostStatus.DRAFT);
        assertDoesNotThrow(() -> statusMachine.transition(post, PostStatus.CANCELLED));
        assertEquals(PostStatus.CANCELLED, post.getStatus());
    }

    @Test
    void transition_DraftToPublished_ThrowsException() {
        post.setStatus(PostStatus.DRAFT);
        PostValidationException exception = assertThrows(PostValidationException.class, 
                () -> statusMachine.transition(post, PostStatus.PUBLISHED));
        assertTrue(exception.getMessage().contains("Cannot transition from DRAFT to PUBLISHED"));
    }

    @Test
    void transition_ScheduledToPublishing_Success() {
        post.setStatus(PostStatus.SCHEDULED);
        assertDoesNotThrow(() -> statusMachine.transition(post, PostStatus.PUBLISHING));
        assertEquals(PostStatus.PUBLISHING, post.getStatus());
    }

    @Test
    void transition_PublishingToPublished_Success() {
        post.setStatus(PostStatus.PUBLISHING);
        assertDoesNotThrow(() -> statusMachine.transition(post, PostStatus.PUBLISHED));
        assertEquals(PostStatus.PUBLISHED, post.getStatus());
    }
    
    @Test
    void transition_FailedToDraft_Success() {
        post.setStatus(PostStatus.FAILED);
        assertDoesNotThrow(() -> statusMachine.transition(post, PostStatus.DRAFT));
        assertEquals(PostStatus.DRAFT, post.getStatus());
    }
}
