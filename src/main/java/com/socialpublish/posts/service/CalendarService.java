package com.socialpublish.posts.service;

import com.socialpublish.posts.dto.CalendarEventResponse;
import com.socialpublish.posts.mapper.CalendarEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final PostService postService;
    private final CalendarEventMapper calendarEventMapper;

    public List<CalendarEventResponse> getCalendarEvents(UUID userId) {
        return calendarEventMapper.toResponses(postService.getQueuePosts(userId, null));
    }

    public void rescheduleEvent(UUID userId, UUID postId, LocalDateTime scheduledAt) {
        postService.reschedulePost(userId, postId, scheduledAt);
    }

    public void deleteEvent(UUID userId, UUID postId) {
        postService.deletePost(userId, postId);
    }
}
