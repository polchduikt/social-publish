package com.socialpublish.notifications.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.notifications.entity.Notification;
import com.socialpublish.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Map<String, Object>> getNotifications(@CurrentUser CurrentUserView user) {
        List<Notification> notifications = notificationService.getUserNotifications(user.id());
        return notifications.stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId().toString(),
                        "postId", n.getPostId() != null ? n.getPostId().toString() : "",
                        "title", n.getTitle(),
                        "message", n.getMessage(),
                        "type", n.getType(),
                        "status", n.getStatus() != null ? n.getStatus().name() : "",
                        "read", n.isRead(),
                        "timestamp", n.getCreatedAt().toString()
                ))
                .toList();
    }

    @PostMapping("/read")
    public ResponseEntity<Map<String, String>> markAllAsRead(@CurrentUser CurrentUserView user) {
        notificationService.markAllAsRead(user.id());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearAll(@CurrentUser CurrentUserView user) {
        notificationService.clearAll(user.id());
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
