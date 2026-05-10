package com.socialpublish.notifications.service;

import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.entity.Notification;
import com.socialpublish.notifications.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public NotificationService(SimpMessagingTemplate messagingTemplate,
                               NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    public void sendPostUpdate(UUID userId, PostNotification notification) {
        Notification entity = new Notification(
                userId,
                notification.postId(),
                notification.title(),
                notification.message(),
                notification.type(),
                notification.status()
        );
        notificationRepository.save(entity);
        String destination = "/topic/user." + userId;
        messagingTemplate.convertAndSend(destination, notification);
        log.debug("Sent notification to {}: {}", destination, notification.message());
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void clearAll(UUID userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
