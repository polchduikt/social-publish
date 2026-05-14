package com.socialpublish.notifications.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.dto.ActionStatusResponse;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.notifications.dto.NotificationItemResponse;
import com.socialpublish.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationItemResponse> getNotifications(@CurrentUser CurrentUserView user) {
        return notificationService.getUserNotifications(user.id());
    }

    @PostMapping("/read")
    public ResponseEntity<ActionStatusResponse> markAllAsRead(@CurrentUser CurrentUserView user) {
        notificationService.markAllAsRead(user.id());
        return ResponseEntity.ok(new ActionStatusResponse("ok"));
    }

    @DeleteMapping
    public ResponseEntity<ActionStatusResponse> clearAll(@CurrentUser CurrentUserView user) {
        notificationService.clearAll(user.id());
        return ResponseEntity.ok(new ActionStatusResponse("cleared"));
    }
}
