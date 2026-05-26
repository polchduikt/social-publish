package com.socialpublish.posts.service;

import com.socialpublish.posts.dto.CalendarEventResponse;
import com.socialpublish.posts.dto.CalendarEventExtendedPropsResponse;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.PostStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final String COLOR_DRAFT = "#94a3b8";
    private static final String COLOR_SCHEDULED = "#4f83ff";
    private static final String COLOR_PUBLISHING = "#f59e0b";
    private static final String COLOR_PUBLISHED = "#22c55e";
    private static final String COLOR_RETRYING = "#f97316";
    private static final String COLOR_FAILED = "#ef4444";
    private static final String COLOR_CANCELLED = "#6b7280";

    private final PostService postService;

    public List<CalendarEventResponse> getCalendarEvents(UUID userId) {
        return postService.getQueuePosts(userId, null)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    public void rescheduleEvent(UUID userId, UUID postId, LocalDateTime scheduledAt) {
        postService.reschedulePost(userId, postId, scheduledAt);
    }

    public void deleteEvent(UUID userId, UUID postId) {
        postService.deletePost(userId, postId);
    }

    private CalendarEventResponse toEventResponse(PostView post) {
        String color = mapStatusColor(post.status());
        String start = post.scheduledAt() != null
                ? post.scheduledAt().toString()
                : post.createdAt().toString();
        String platformLabel = formatPlatformList(post.platformList());

        CalendarEventExtendedPropsResponse extendedProps = new CalendarEventExtendedPropsResponse(
                post.status(),
                platformLabel,
                post.excerpt() == null ? "" : post.excerpt(),
                post.id().toString()
        );

        return new CalendarEventResponse(
                post.id().toString(),
                post.title(),
                start,
                color,
                extendedProps
        );
    }

    private String mapStatusColor(String statusStr) {
        PostStatus status = statusStr == null || statusStr.isBlank() ? PostStatus.DRAFT : PostStatus.valueOf(statusStr);
        return switch (status) {
            case DRAFT -> COLOR_DRAFT;
            case SCHEDULED -> COLOR_SCHEDULED;
            case PUBLISHING -> COLOR_PUBLISHING;
            case PUBLISHED -> COLOR_PUBLISHED;
            case RETRYING -> COLOR_RETRYING;
            case FAILED -> COLOR_FAILED;
            case CANCELLED -> COLOR_CANCELLED;
        };
    }

    private String formatPlatformList(List<String> platformList) {
        if (platformList == null || platformList.isEmpty()) {
            return "";
        }
        return platformList.stream()
                .map(p -> p.contains(":") ? p.split(":")[0] : p)
                .map(p -> p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
