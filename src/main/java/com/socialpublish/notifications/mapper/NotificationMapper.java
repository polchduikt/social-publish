package com.socialpublish.notifications.mapper;

import com.socialpublish.notifications.dto.NotificationItemResponse;
import com.socialpublish.notifications.dto.PostNotification;
import com.socialpublish.notifications.entity.Notification;
import com.socialpublish.posts.entity.PostStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "postId", source = "postId", qualifiedByName = "uuidToEmptyString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "timestamp", source = "createdAt", qualifiedByName = "instantToString")
    NotificationItemResponse toResponse(Notification notification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "read", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", source = "notification.status", qualifiedByName = "stringToStatus")
    Notification toEntity(UUID userId, PostNotification notification);

    List<NotificationItemResponse> toResponses(List<Notification> notifications);

    @Named("uuidToString")
    default String uuidToString(UUID value) {
        return value == null ? "" : value.toString();
    }

    @Named("uuidToEmptyString")
    default String uuidToEmptyString(UUID value) {
        return value == null ? "" : value.toString();
    }

    @Named("statusToString")
    default String statusToString(PostStatus status) {
        return status == null ? "" : status.name();
    }

    @Named("stringToStatus")
    default PostStatus stringToStatus(String status) {
        return status == null || status.isBlank() ? null : PostStatus.valueOf(status);
    }

    @Named("instantToString")
    default String instantToString(Instant value) {
        return value == null ? "" : value.toString();
    }
}
