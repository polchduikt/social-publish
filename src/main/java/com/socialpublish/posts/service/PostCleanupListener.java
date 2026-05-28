package com.socialpublish.posts.service;

import com.socialpublish.auth.event.UserDeletedEvent;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.repository.PostTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCleanupListener {

    private final PostRepository postRepository;
    private final PostTemplateRepository postTemplateRepository;

    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("Received UserDeletedEvent for user: {}. Cleaning up posts and templates.", event.getUserId());
        try {
            postRepository.deleteAllByOwnerId(event.getUserId());
            postTemplateRepository.deleteAllByOwnerId(event.getUserId());
        } catch (Exception e) {
            log.error("Failed to clean up posts and templates for user {}", event.getUserId(), e);
            throw e;
        }
        log.info("Completed cleaning up posts and templates for user: {}", event.getUserId());
    }
}
