package com.socialpublish.notifications.service;

import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.dto.NotificationItemResponse;
import com.socialpublish.notifications.entity.Notification;
import com.socialpublish.notifications.repository.NotificationRepository;
import com.socialpublish.notifications.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public void sendPostUpdate(UUID userId, PostNotification notification) {
        Notification entity = notificationMapper.toEntity(userId, notification);
        notificationRepository.save(entity);
        String destination = "/topic/user." + userId;
        messagingTemplate.convertAndSend(destination, notification);
        log.debug("Sent notification to {}: {}", destination, notification.message());
    }

    @Transactional(readOnly = true)
    public List<NotificationItemResponse> getUserNotifications(UUID userId) {
        return notificationMapper.toResponses(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
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
