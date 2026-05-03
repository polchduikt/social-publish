package com.socialpublish.notifications.service;

import com.socialpublish.notifications.dto.PostNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendPostUpdate(UUID userId, PostNotification notification) {
        String destination = "/topic/user." + userId;
        messagingTemplate.convertAndSend(destination, notification);
        log.debug("Sent notification to {}: {}", destination, notification.message());
    }
}
