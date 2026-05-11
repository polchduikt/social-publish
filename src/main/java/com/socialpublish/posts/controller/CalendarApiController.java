package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.dto.ActionStatusResponse;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.posts.dto.CalendarEventResponse;
import com.socialpublish.posts.dto.RescheduleEventRequest;
import com.socialpublish.posts.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarApiController {

    private final CalendarService calendarService;

    @GetMapping("/events")
    public List<CalendarEventResponse> events(@CurrentUser CurrentUserView currentUser) {
        return calendarService.getCalendarEvents(currentUser.id());
    }

    @PatchMapping("/events/{id}/reschedule")
    public ResponseEntity<ActionStatusResponse> reschedule(
            @CurrentUser CurrentUserView currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleEventRequest request
    ) {
        calendarService.rescheduleEvent(currentUser.id(), id, request.getScheduledAt());
        return ResponseEntity.ok(new ActionStatusResponse("ok"));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<ActionStatusResponse> delete(
            @CurrentUser CurrentUserView currentUser,
            @PathVariable UUID id
    ) {
        calendarService.deleteEvent(currentUser.id(), id);
        return ResponseEntity.ok(new ActionStatusResponse("deleted"));
    }
}
