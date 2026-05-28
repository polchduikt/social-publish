package com.socialpublish.posts.mapper;

import com.socialpublish.posts.dto.CalendarEventExtendedPropsResponse;
import com.socialpublish.posts.dto.CalendarEventResponse;
import com.socialpublish.posts.dto.PostView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CalendarEventMapper {

    String COLOR_DRAFT = "#94a3b8";
    String COLOR_SCHEDULED = "#4f83ff";
    String COLOR_PUBLISHING = "#f59e0b";
    String COLOR_PUBLISHED = "#22c55e";
    String COLOR_RETRYING = "#f97316";
    String COLOR_FAILED = "#ef4444";
    String COLOR_CANCELLED = "#6b7280";

    @Mapping(target = "id", expression = "java(post.id().toString())")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "start", expression = "java(post.scheduledAt() != null ? post.scheduledAt().toString() : post.createdAt().toString())")
    @Mapping(target = "color", source = "status", qualifiedByName = "mapStatusColor")
    @Mapping(target = "extendedProps", expression = "java(toExtendedProps(post))")
    CalendarEventResponse toResponse(PostView post);

    List<CalendarEventResponse> toResponses(List<PostView> posts);

    default CalendarEventExtendedPropsResponse toExtendedProps(PostView post) {
        String platformLabel = formatPlatformList(post.platformList());
        return new CalendarEventExtendedPropsResponse(
                post.status(),
                platformLabel,
                post.excerpt() == null ? "" : post.excerpt(),
                post.id().toString()
        );
    }

    @Named("mapStatusColor")
    default String mapStatusColor(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return COLOR_DRAFT;
        return switch (statusStr) {
            case "DRAFT" -> COLOR_DRAFT;
            case "SCHEDULED" -> COLOR_SCHEDULED;
            case "PUBLISHING" -> COLOR_PUBLISHING;
            case "PUBLISHED" -> COLOR_PUBLISHED;
            case "RETRYING" -> COLOR_RETRYING;
            case "FAILED" -> COLOR_FAILED;
            case "CANCELLED" -> COLOR_CANCELLED;
            default -> COLOR_DRAFT;
        };
    }

    default String formatPlatformList(List<String> platformList) {
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
