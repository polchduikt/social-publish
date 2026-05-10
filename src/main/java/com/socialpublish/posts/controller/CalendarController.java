package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final PostService postService;

    @GetMapping("/calendar")
    public String calendarPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        return "pages/dashboard/calendar";
    }

    @GetMapping("/api/calendar/events")
    @ResponseBody
    public List<Map<String, Object>> calendarEvents(@CurrentUser CurrentUserView currentUser) {
        List<PostView> posts = postService.getQueuePosts(currentUser.id(), null);
        return posts.stream()
                .map(this::toCalendarEvent)
                .toList();
    }

    @PatchMapping("/api/calendar/events/{id}/reschedule")
    @ResponseBody
    public ResponseEntity<Map<String, String>> rescheduleEvent(
            @CurrentUser CurrentUserView currentUser,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String dateStr = body.get("scheduledAt");
        if (dateStr == null || dateStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledAt is required"));
        }
        LocalDateTime scheduledAt = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        postService.reschedulePost(currentUser.id(), id, scheduledAt);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/api/calendar/events/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteEvent(
            @CurrentUser CurrentUserView currentUser,
            @PathVariable UUID id
    ) {
        postService.deletePost(currentUser.id(), id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    private Map<String, Object> toCalendarEvent(PostView post) {
        String color = switch (post.status()) {
            case DRAFT -> "#94a3b8";
            case SCHEDULED -> "#4f83ff";
            case PUBLISHING -> "#f59e0b";
            case PUBLISHED -> "#22c55e";
            case RETRYING -> "#f97316";
            case FAILED -> "#ef4444";
            case CANCELLED -> "#6b7280";
        };

        String start = post.scheduledAt() != null
                ? post.scheduledAt().toString()
                : post.createdAt().toString();

        List<String> platforms = post.platformList();
        String platformLabel = platforms.isEmpty() ? "" : String.join(", ", platforms);

        return Map.of(
                "id", post.id().toString(),
                "title", post.title(),
                "start", start,
                "color", color,
                "extendedProps", Map.of(
                        "status", post.status().name(),
                        "platforms", platformLabel,
                        "excerpt", post.excerpt(),
                        "postId", post.id().toString()
                )
        );
    }
}
