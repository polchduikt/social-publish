package com.socialpublish.posts.dto;

public record CalendarEventResponse(
        String id,
        String title,
        String start,
        String color,
        CalendarEventExtendedPropsResponse extendedProps
) {
}
