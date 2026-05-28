package com.socialpublish.notifications.service;

import com.socialpublish.auth.event.UserDeletedEvent;
import com.socialpublish.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupListener {

    private final NotificationRepository notificationRepository;

    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("Received UserDeletedEvent for user: {}. Cleaning up notifications.", event.getUserId());
        try {
            notificationRepository.deleteByUserId(event.getUserId());
        } catch (Exception e) {
            log.error("Failed to clean up notifications for user {}", event.getUserId(), e);
            throw e;
        }
        log.info("Completed cleaning up notifications for user: {}", event.getUserId());
    }
}
